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
)

enum class SearchMode { TYPING, BROWSING }

@Immutable
data class SearchMenu(
    val key: AppKey,
    val label: String,
    val anchor: Offset,
    val inEnv: Boolean,
    val canUninstall: Boolean,
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

    private var index: SearchIndex = SearchIndex.EMPTY
    private var usage: Map<AppKey, AppUsage> = emptyMap()

    val vocabulary: List<String> get() = index.vocabulary

    fun update(index: SearchIndex, usage: Map<AppKey, AppUsage>) {
        this.index = index
        this.usage = usage
        refresh()
    }

    fun setEngine(engine: SearchEngine) {
        this.engine = engine
        refresh()
    }

    fun type(c: Char): Boolean {
        if (query.length >= MAX_QUERY) return false
        query += c
        refresh()
        return true
    }

    fun backspace(): Boolean {
        if (query.isEmpty()) return false
        query = query.dropLast(1)
        refresh()
        return true
    }

    fun replaceQuery(text: String) {
        query = text.take(MAX_QUERY)
        refresh()
    }

    fun clear() {
        if (query.isEmpty()) return
        query = ""
        refresh()
    }

    fun reset() {
        query = ""
        selected = NO_SELECTION
        menu = null
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

    fun target(): SearchRow? = rows.getOrNull(selected.coerceAtLeast(0))

    private fun refresh() {
        selected = NO_SELECTION
        rows = if (query.isEmpty()) {
            index.alphabetical.map { SearchRow(it.key, it.displayLabel, hidden = false) }.toImmutableList()
        } else {
            engine.search(index, query, usage, clock())
                .map { SearchRow(it.key, it.label, it.hidden) }
                .toImmutableList()
        }
    }

    companion object {
        const val MAX_QUERY = 32
        const val NO_SELECTION = -1
    }
}
