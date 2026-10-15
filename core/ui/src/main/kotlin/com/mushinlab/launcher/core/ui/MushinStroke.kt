package com.mushinlab.launcher.core.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.model.StrokeColor
import com.mushinlab.launcher.core.model.StrokeWidth

/**
 * Appearance of the gesture trail: [width], [color] choice and whether vertices are drawn as [dots].
 * [color] resolves the choice against the current palette.
 */
@Immutable
data class MushinStroke(val width: Dp, val color: StrokeColor, val dots: Boolean = true) {
    fun color(colors: MushinColors): Color = when (color) {
        StrokeColor.ACCENT -> colors.accent
        StrokeColor.TEXT -> colors.textPrimary
        else -> Color(color.argb ?: 0)
    }

    companion object {
        val Default = MushinStroke(StrokeWidth.DEFAULT_DP.dp, StrokeColor.ACCENT)
    }
}

/** Gesture trail appearance in the current subtree. */
val LocalMushinStroke = staticCompositionLocalOf { MushinStroke.Default }
