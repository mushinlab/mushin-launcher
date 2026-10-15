package com.mushinlab.launcher

import android.content.ContentResolver
import android.graphics.ImageDecoder
import android.net.Uri
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.SwipeDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PreferencesEditor(private val container: AppContainer, private val contentResolver: ContentResolver) {
    fun update(transform: (LauncherSettings) -> LauncherSettings) {
        container.scope.launch { container.settings.update(transform) }
    }

    fun setActiveLayout(id: String) = update { it.copy(activeKeyboardLayout = id) }

    fun setGesture(direction: SwipeDirection, action: GestureAction) {
        container.scope.launch { container.gestures.set(direction, action) }
    }

    fun clearHistory(spaceId: Long) {
        container.scope.launch { container.history.clearRecents(spaceId) }
    }

    fun setBackground(uri: Uri, onFailure: () -> Unit) {
        val source = ImageDecoder.createSource(contentResolver, uri)
        container.scope.launch(Dispatchers.IO) {
            if (!container.background.save(source)) launch(Dispatchers.Main) { onFailure() }
        }
    }

    fun clearBackground() {
        container.scope.launch(Dispatchers.IO) { container.background.clear() }
    }
}
