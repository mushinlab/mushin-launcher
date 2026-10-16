package com.mushinlab.launcher.feature.search

import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.AppUsage
import com.mushinlab.launcher.core.model.LearnedGesture
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.search.SearchEngine
import com.mushinlab.launcher.core.search.SearchIndex
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

class SearchSources internal constructor(
    private var engine: SearchEngine,
    private val clock: () -> Long,
    private val onChange: () -> Unit,
) {
    internal var index: SearchIndex = SearchIndex.EMPTY
        private set
    internal var learned: List<LearnedGesture> = emptyList()
        private set
    private var usage: Map<AppKey, AppUsage> = emptyMap()
    private var swipes: Map<AppKey, List<SwipeDirection>> = emptyMap()

    fun update(index: SearchIndex, usage: Map<AppKey, AppUsage>) {
        this.index = index
        this.usage = usage
        onChange()
    }

    fun setLearned(samples: List<LearnedGesture>) {
        learned = samples
    }

    fun setSwipes(byApp: Map<AppKey, List<SwipeDirection>>) {
        if (byApp == swipes) return
        swipes = byApp
        onChange()
    }

    fun setEngine(engine: SearchEngine) {
        this.engine = engine
        onChange()
    }

    internal fun suggestedRows(keys: List<AppKey>): ImmutableList<SearchRow> {
        val visible = index.alphabetical.associateBy { it.key }
        return keys.mapNotNull { key -> visible[key]?.let { row(key, it.displayLabel, hidden = false) } }
            .toImmutableList()
    }

    internal fun rows(query: String, showHidden: Boolean): ImmutableList<SearchRow> = if (query.isEmpty()) {
        (if (showHidden) index.alphabeticalWithHidden else index.alphabetical)
            .map { row(it.key, it.displayLabel, it.hidden) }
            .toImmutableList()
    } else {
        engine.search(index, query, usage, clock(), includeHidden = showHidden)
            .map { row(it.key, it.label, it.hidden) }
            .toImmutableList()
    }

    private fun row(key: AppKey, label: String, hidden: Boolean) =
        SearchRow(key, label, hidden, swipes[key]?.toImmutableList() ?: persistentListOf())
}
