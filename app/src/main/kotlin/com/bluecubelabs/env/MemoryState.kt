package com.bluecubelabs.env

import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.AppUsage
import com.bluecubelabs.env.core.model.LearnedGesture
import com.bluecubelabs.env.core.model.SpaceUsage
import com.bluecubelabs.env.core.model.Usefulness
import com.bluecubelabs.env.core.search.Alphabet
import com.bluecubelabs.env.feature.settings.LearnedApp
import com.bluecubelabs.env.feature.settings.LearnedDrawing
import com.bluecubelabs.env.feature.settings.ScoreRow
import com.bluecubelabs.env.feature.settings.SettingsUiState
import com.bluecubelabs.env.feature.settings.UsageRow
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

class MemoryEditor(private val container: AppContainer) {
    fun forget(match: (LearnedGesture) -> Boolean) {
        container.scope.launch { container.gestureLearning.update { samples -> samples.filterNot(match) } }
    }

    fun clearUsage() {
        container.scope.launch { container.history.clearUsage() }
    }
}

internal fun SettingsUiState.withMemory(samples: List<LearnedGesture>, usage: Map<AppKey, AppUsage>): SettingsUiState {
    val labels = apps.associate { it.key to it.label }
    return copy(
        learned = samples.groupBy { it.key }
            .mapNotNull { (key, list) ->
                val label = labels[key] ?: return@mapNotNull null
                val drawings = list.sortedByDescending { it.weight }.map { LearnedDrawing(it.path, it.weight) }
                LearnedApp(key, label, drawings.toImmutableList())
            }
            .sortedWith(Alphabet.comparator { it.label })
            .toImmutableList(),
        usage = usage.values
            .mapNotNull { u -> labels[u.key]?.let { UsageRow(u.key, it, u.launchCount) } }
            .sortedByDescending { it.launches }
            .toImmutableList(),
    )
}

internal fun SettingsUiState.withUsefulness(usage: List<SpaceUsage>, now: Long): SettingsUiState {
    val labels = apps.associate { it.key to it.label }
    return copy(
        usefulness = Usefulness.scores(usage, now)
            .mapNotNull { (key, score) -> labels[key]?.let { ScoreRow(key, it, score) } }
            .sortedByDescending { it.score }
            .toImmutableList(),
    )
}
