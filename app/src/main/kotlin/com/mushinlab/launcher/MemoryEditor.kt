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
