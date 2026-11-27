package com.mushinlab.launcher.feature.settings

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap

@Immutable
data class WidgetLabel(val id: Int, val label: String, val app: String? = null, val clock: Boolean = false)

@Stable
interface WidgetArt {
    suspend fun preview(id: Int): ImageBitmap?

    suspend fun appIcon(id: Int): ImageBitmap?
}
