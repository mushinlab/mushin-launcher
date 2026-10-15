package com.mushinlab.launcher

import com.mushinlab.launcher.core.apps.LaunchResult
import com.mushinlab.launcher.core.model.AppEntry
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.AppOverride
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.LearnedGesture
import com.mushinlab.launcher.core.model.Space
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.search.GestureLearning
import com.mushinlab.launcher.core.search.GestureSample
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RepositoryLauncherModel(
    private val container: AppContainer,
    private val data: StateFlow<LauncherData?>,
    private val spaces: SpaceEditor,
    private val apps: AppEditor,
    private val preferences: PreferencesEditor,
) : LauncherModel {
    override val settings: LauncherSettings get() = container.settings.settings.value
    override val gestureBindings: Map<SwipeDirection, GestureAction> get() = container.gestures.bindings.value
    override val learnedGestures: List<LearnedGesture> get() = container.gestureLearning.samples.value
    override val currentSpace: Space? get() = data.value?.current

    override fun entry(key: AppKey): AppEntry? = data.value?.apps?.get(key)

    override fun override(key: AppKey): AppOverride? = data.value?.overrides?.get(key)

    override fun isInCurrentSpace(key: AppKey): Boolean = data.value?.currentItems()?.contains(key) == true

    override fun launch(
        key: AppKey,
        fromSearch: Boolean,
        animate: Boolean,
        gestures: List<GestureSample>,
    ): LaunchResult {
        val result = container.launcher.launch(key, animate = animate)
        if (result == LaunchResult.Started) {
            val spaceId = currentSpace?.id
            container.scope.launch { container.history.recordLaunch(key, spaceId, fromSearch) }
            if (gestures.isNotEmpty()) {
                container.scope.launch { container.gestureLearning.update { GestureLearning.learn(it, gestures, key) } }
            }
        }
        return result
    }

    override fun expandShade(): Boolean = container.shade.expand()

    override fun addToCurrentSpace(key: AppKey): Boolean = currentSpace?.let { spaces.addItem(it.id, key) } == true

    override fun removeFromCurrentSpace(key: AppKey) {
        currentSpace?.let { spaces.removeItem(it.id, key) }
    }

    override fun setHidden(key: AppKey, hidden: Boolean) = apps.setHidden(key, hidden)

    override fun setActiveLayout(id: String) = preferences.setActiveLayout(id)
}
