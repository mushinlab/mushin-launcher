package com.bluecubelabs.env.feature.search

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.AppUsage
import com.bluecubelabs.env.core.model.LearnedGesture
import com.bluecubelabs.env.core.model.SwipeDirection
import com.bluecubelabs.env.core.search.GestureDecoder
import com.bluecubelabs.env.core.search.GesturePoint
import com.bluecubelabs.env.core.search.SearchEngine
import com.bluecubelabs.env.core.search.SearchIndex
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Immutable
data class SearchRow(
    val key: AppKey,
    val label: String,
    val hidden: Boolean,
    val swipes: ImmutableList<SwipeDirection> = persistentListOf(),
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

    private var index: SearchIndex = SearchIndex.EMPTY
    private var usage: Map<AppKey, AppUsage> = emptyMap()
    private var learned: List<LearnedGesture> = emptyList()
    private var swipes: Map<AppKey, List<SwipeDirection>> = emptyMap()
    private var suggestions: List<AppKey> = emptyList()
    private val gesturePaths = mutableListOf<String>()
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

    fun applyGesture(decoder: GestureDecoder, path: List<GesturePoint>): Boolean {
        val result = decoder.decode(path, index.gestureVocabulary, learned) ?: return true
        gesturePaths += result.path
        if (gesturePaths.size > 1) gestureCorrected = true
        val word = result.words.singleOrNull()
        when {
            result.words.isEmpty() -> return false

            word != null -> replaceQuery(word.text)

            else -> {
                gestureCorrected = true
                suggestions = result.words.map { it.key }
                query = ""
                suggesting = true
                refresh()
            }
        }
        return true
    }

    fun gesturesToLearn(): List<String> = if (gestureCorrected) gesturePaths.toList() else emptyList()

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
        changeQuery(query.dropLast(1))
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
        gesturePaths.clear()
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

    private fun markCorrected() {
        if (gesturePaths.isNotEmpty()) gestureCorrected = true
    }

    fun target(): SearchRow? = rows.getOrNull(selected.coerceAtLeast(0))

    private fun changeQuery(text: String) {
        suggesting = false
        query = text
        refresh()
    }

    private fun refresh() {
        selected = NO_SELECTION
        rows = when {
            suggesting -> {
                val visible = index.alphabetical.associateBy { it.key }
                suggestions.mapNotNull { key ->
                    visible[key]?.let { SearchRow(key, it.displayLabel, hidden = false, swipesOf(key)) }
                }
                    .toImmutableList()
            }

            query.isEmpty() -> {
                (if (showHidden) index.alphabeticalWithHidden else index.alphabetical)
                    .map { SearchRow(it.key, it.displayLabel, it.hidden, swipesOf(it.key)) }
                    .toImmutableList()
            }

            else -> {
                engine.search(index, query, usage, clock(), includeHidden = showHidden)
                    .map { SearchRow(it.key, it.label, it.hidden, swipesOf(it.key)) }
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
