package com.mushinlab.launcher

import com.mushinlab.launcher.core.model.PalettePreset
import kotlinx.coroutines.launch

class ThemeEditor(private val container: AppContainer) {
    private val shared get() = container.settings.settings.value.sharedTheme

    fun setPalette(id: Long, preset: PalettePreset) = update(id) { container.spaces.setPalette(id, preset) }

    fun setColoredIcons(id: Long, enabled: Boolean) = update(id) { container.spaces.setColoredIcons(id, enabled) }

    fun setShared(fromId: Long, enabled: Boolean) {
        container.scope.launch {
            container.settings.update { it.copy(sharedTheme = enabled) }
            if (enabled) container.looks.shareTheme(fromId)
        }
    }

    private fun update(id: Long, change: suspend () -> Unit) {
        container.scope.launch {
            change()
            if (shared) container.looks.shareTheme(id)
        }
    }
}
