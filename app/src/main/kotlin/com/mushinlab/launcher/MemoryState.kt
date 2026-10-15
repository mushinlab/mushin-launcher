package com.mushinlab.launcher

import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.AppUsage
import com.mushinlab.launcher.core.model.LearnedGesture
import com.mushinlab.launcher.core.model.SpaceUsage
import com.mushinlab.launcher.core.model.Usefulness
import com.mushinlab.launcher.core.search.Alphabet
import com.mushinlab.launcher.core.search.GestureLearning
import com.mushinlab.launcher.core.search.GestureSample
import com.mushinlab.launcher.feature.settings.LearnedApp
import com.mushinlab.launcher.feature.settings.LearnedDrawing
import com.mushinlab.launcher.feature.settings.ScoreRow
import com.mushinlab.launcher.feature.settings.SettingsUiState
import com.mushinlab.launcher.feature.settings.UsageRow
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

class MemoryEditor(private val container: AppContainer) {
    fun forget(match: (LearnedGesture) -> Boolean) {
        container.scope.launch { container.gestureLearning.update { samples -> samples.filterNot(match) } }
    }

    fun add(key: AppKey, sample: GestureSample) {
        container.scope.launch { container.gestureLearning.update { GestureLearning.learn(it, listOf(sample), key) } }
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
                val drawings = list.sortedByDescending {
                    it.weight
                }.map { LearnedDrawing(it.path, it.weight, it.shape) }
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
