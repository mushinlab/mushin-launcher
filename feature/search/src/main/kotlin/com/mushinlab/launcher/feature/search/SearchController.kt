package com.mushinlab.launcher.feature.search

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.search.SearchEngine
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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
    val inSpace: Boolean,
    val canUninstall: Boolean,
    val hidden: Boolean = false,
)

@Stable
class SearchController(
    engine: SearchEngine = SearchEngine(),
    clock: () -> Long = System::currentTimeMillis,
) {
    var query by mutableStateOf("")
        private set
    var rows by mutableStateOf<ImmutableList<SearchRow>>(persistentListOf())
        private set
    var selected by mutableIntStateOf(NO_SELECTION)
        private set
    var session by mutableIntStateOf(0)
        private set
    var suggesting by mutableStateOf(false)
        private set
    var showHidden by mutableStateOf(false)
        private set
    var fromGesture by mutableStateOf(false)
        private set

    val sources = SearchSources(engine, clock) { refresh() }
    val panels = SearchPanels()
    val gestures = SearchGestures(
        sources = sources,
        onWord = { word ->
            replaceQuery(word)
            fromGesture = true
        },
        onWords = { keys -> suggest(keys) },
    )

    private var suggestions: List<AppKey> = emptyList()

    fun type(c: Char): Boolean {
        val doubleSpace = c == ' ' && (query.isEmpty() || query.last() == ' ')
        val accepted = query.length < MAX_QUERY && !doubleSpace
        if (accepted) {
            gestures.markCorrected()
            replaceQuery(query + c)
        }
        return accepted
    }

    fun backspace(): Boolean {
        if (query.isEmpty() && !suggesting) return false
        gestures.markCorrected()
        replaceQuery(if (fromGesture) "" else query.dropLast(1))
        return true
    }

    fun replaceQuery(text: String) {
        suggesting = false
        fromGesture = false
        query = text.take(MAX_QUERY)
        refresh()
    }

    fun clear() {
        if (query.isEmpty() && !suggesting) return
        replaceQuery("")
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
        panels.reset()
        fromGesture = false
        gestures.reset()
        session++
        refresh()
    }

    fun moveSelection(delta: Int) {
        if (rows.isEmpty()) return
        selected = (selected + delta).coerceIn(0, rows.lastIndex)
    }

    fun suggest(keys: List<AppKey>) {
        suggestions = keys
        query = ""
        suggesting = true
        fromGesture = true
        refresh()
    }

    fun target(): SearchRow? = rows.getOrNull(selected.coerceAtLeast(0))

    private fun refresh() {
        selected = NO_SELECTION
        rows = if (suggesting) sources.suggestedRows(suggestions) else sources.rows(query, showHidden)
    }

    companion object {
        const val MAX_QUERY = 32
        const val NO_SELECTION = -1
    }
}
