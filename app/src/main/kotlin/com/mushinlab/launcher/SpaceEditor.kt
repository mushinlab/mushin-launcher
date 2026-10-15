package com.mushinlab.launcher

import com.mushinlab.launcher.core.model.PalettePreset
import kotlinx.coroutines.launch

class SpaceEditor(private val container: AppContainer) {
    fun setPalette(id: Long, preset: PalettePreset) {
        container.scope.launch { container.environments.setPalette(id, preset) }
    }

    fun setColoredIcons(id: Long, enabled: Boolean) {
        container.scope.launch { container.environments.setColoredIcons(id, enabled) }
    }

    fun setListOffset(id: Long, dp: Int) {
        container.scope.launch { container.environments.setListOffset(id, dp) }
    }
}
