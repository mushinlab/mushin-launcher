package com.mushinlab.launcher

import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.SwipeDirection
import kotlinx.coroutines.launch

class PreferencesEditor(private val container: AppContainer) {
    fun update(transform: (LauncherSettings) -> LauncherSettings) {
        container.scope.launch { container.settings.update(transform) }
    }

    fun setActiveLayout(id: String) = update { it.copy(activeKeyboardLayout = id) }

    fun setGesture(direction: SwipeDirection, action: GestureAction) {
        container.scope.launch { container.gestures.set(direction, action) }
    }
}
