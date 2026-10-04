package com.bluecubelabs.env

import com.bluecubelabs.env.core.model.PalettePreset
import kotlinx.coroutines.launch

class SpaceEditor(private val container: AppContainer) {
    fun setPalette(id: Long, preset: PalettePreset) {
        container.scope.launch { container.environments.setPalette(id, preset) }
    }

    fun setColoredIcons(id: Long, enabled: Boolean) {
        container.scope.launch { container.environments.setColoredIcons(id, enabled) }
    }
}
