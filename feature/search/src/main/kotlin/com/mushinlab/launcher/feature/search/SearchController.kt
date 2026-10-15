package com.mushinlab.launcher.feature.search

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.AppUsage
import com.mushinlab.launcher.core.model.LearnedGesture
import com.mushinlab.launcher.core.model.ShapeVertex
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.search.GestureDecoder
import com.mushinlab.launcher.core.search.GestureLearning
import com.mushinlab.launcher.core.search.GesturePoint
import com.mushinlab.launcher.core.search.GestureSample
import com.mushinlab.launcher.core.search.SearchEngine
import com.mushinlab.launcher.core.search.SearchIndex
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Immutable
data class SearchRow(
    val key: AppKey,
    val label: String,
    val hidden: Boolean,
    val swipes: ImmutableList<SwipeDirection> = persistentListOf(),
    val shortcut: Boolean = false,
)

enum class SearchMode { TYPING, BROWSING }

@Immutable
data class SearchMenu(
    val key: AppKey,
    val label: String,
    val anchor: Offset,
    val inEnv: Boolean,
    val canUninstall: Boolean,
    val hidden: Boolean = false,
    val shortcut: Boolean = false,
)

@Stable
class SearchController(
    private var engine: SearchEngine = SearchEngine(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    var query by mutableStateOf("")
        private set
    var rows by mutableStateOf<ImmutableList<SearchRow>>(persistentListOf())
        private set
    var selected by mutableIntStateOf(NO_SELECTION)
        private set
    var menu by mutableStateOf<SearchMenu?>(null)
        private set
    var session by mutableIntStateOf(0)
        private set
    var suggesting by mutableStateOf(false)
        private set
    var showHidden by mutableStateOf(false)
        private set
    var options by mutableStateOf<Offset?>(null)
        private set
    var resizing by mutableStateOf(false)
        private set
    var fromGesture by mutableStateOf(false)
        private set

    private var index: SearchIndex = SearchIndex.EMPTY
    private var usage: Map<AppKey, AppUsage> = emptyMap()
    private var learned: List<LearnedGesture> = emptyList()
    private var swipes: Map<AppKey, List<SwipeDirection>> = emptyMap()
    private var shortcuts: Set<AppKey> = emptySet()
    private var suggestions: List<AppKey> = emptyList()
    private val drawings = mutableListOf<Drawing>()
    private var decoder: GestureDecoder? = null
    private var gestureCorrected = false

    fun update(index: SearchIndex, usage: Map<AppKey, AppUsage>) {
        this.index = index
        this.usage = usage
        refresh()
    }

    fun setLearned(samples: List<LearnedGesture>) {
        learned = samples
    }

    fun setSwipes(byApp: Map<AppKey, List<SwipeDirection>>) {
        if (byApp == swipes) return
        swipes = byApp
        refresh()
    }

    fun setShortcuts(keys: Set<AppKey>) {
        if (keys == shortcuts) return
        shortcuts = keys
        refresh()
    }

    fun applyGesture(
        decoder: GestureDecoder,
        path: List<GesturePoint>,
        shape: List<ShapeVertex> = emptyList(),
    ): Boolean {
        val result = decoder.decode(path, index.gestureVocabulary, learned) ?: return true
        this.decoder = decoder
        drawings += Drawing(path, GestureSample(result.path, shape))
        if (drawings.size > 1) gestureCorrected = true
        val word = result.words.singleOrNull()
        when {
            result.words.isEmpty() -> return false

            word != null -> {
                replaceQuery(word.text)
                fromGesture = true
            }

            else -> {
                gestureCorrected = true
                suggest(result.words.map { it.key })
            }
        }
        return true
    }

    fun gesturesToLearn(key: AppKey): List<GestureSample> {
        val decoder = decoder ?: return emptyList()
        if (!gestureCorrected) return emptyList()
        val words = index.gestureVocabulary.filter { it.key == key }.map { it.latin }
        return drawings.filter { GestureLearning.resembles(decoder, it.points, words) }.map { it.sample }
    }

    fun setEngine(engine: SearchEngine) {
        this.engine = engine
        refresh()
    }

    fun type(c: Char): Boolean {
        if (query.length >= MAX_QUERY) return false
        if (c == ' ' && (query.isEmpty() || query.last() == ' ')) return false
        markCorrected()
        changeQuery(query + c)
        return true
    }

    fun backspace(): Boolean {
        if (query.isEmpty() && !suggesting) return false
        markCorrected()
        changeQuery(if (fromGesture) "" else query.dropLast(1))
        return true
    }

    fun replaceQuery(text: String) {
        changeQuery(text.take(MAX_QUERY))
    }

    fun clear() {
        if (query.isEmpty() && !suggesting) return
        changeQuery("")
    }

    fun toggleShowHidden() {
        showHidden = !showHidden
        refresh()
    }

    fun reset() {
        query = ""
        suggesting = false
        showHidden = false
        selected = NO_SELECTION
        menu = null
        options = null
        resizing = false
        fromGesture = false
        decoder = null
        drawings.clear()
        gestureCorrected = false
        session++
        refresh()
    }

    fun moveSelection(delta: Int) {
        if (rows.isEmpty()) return
        selected = (selected + delta).coerceIn(0, rows.lastIndex)
    }

    fun openMenu(menu: SearchMenu) {
        this.menu = menu
    }

    fun closeMenu() {
        menu = null
    }

    fun openOptions(anchor: Offset) {
        options = anchor
    }

    fun closeOptions() {
        options = null
    }

    fun toggleResize() {
        resizing = !resizing
    }

    fun suggest(keys: List<AppKey>) {
        suggestions = keys
        query = ""
        suggesting = true
        fromGesture = true
        refresh()
    }

    private fun markCorrected() {
        if (drawings.isNotEmpty()) gestureCorrected = true
    }

    fun target(): SearchRow? = rows.getOrNull(selected.coerceAtLeast(0))

    private fun changeQuery(text: String) {
        suggesting = false
        fromGesture = false
        query = text
        refresh()
    }

    private fun refresh() {
        selected = NO_SELECTION
        rows = when {
            suggesting -> {
                val visible = index.alphabetical.associateBy { it.key }
                suggestions.mapNotNull { key ->
                    visible[key]?.let {
                        SearchRow(key, it.displayLabel, hidden = false, swipesOf(key), key in shortcuts)
                    }
                }
                    .toImmutableList()
            }

            query.isEmpty() -> {
                (if (showHidden) index.alphabeticalWithHidden else index.alphabetical)
                    .map { SearchRow(it.key, it.displayLabel, it.hidden, swipesOf(it.key), it.key in shortcuts) }
                    .toImmutableList()
            }

            else -> {
                engine.search(index, query, usage, clock(), includeHidden = showHidden)
                    .map { SearchRow(it.key, it.label, it.hidden, swipesOf(it.key), it.key in shortcuts) }
                    .toImmutableList()
            }
        }
    }

    private fun swipesOf(key: AppKey): ImmutableList<SwipeDirection> =
        swipes[key]?.toImmutableList() ?: persistentListOf()

    companion object {
        const val MAX_QUERY = 32
        const val NO_SELECTION = -1
    }
}

private class Drawing(val points: List<GesturePoint>, val sample: GestureSample)
