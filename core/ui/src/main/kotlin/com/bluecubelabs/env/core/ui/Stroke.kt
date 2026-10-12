package com.bluecubelabs.env.core.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bluecubelabs.env.core.model.StrokeColor
import com.bluecubelabs.env.core.model.StrokeWidth

@Immutable
data class EnvStroke(val width: Dp, val color: StrokeColor, val dots: Boolean = true) {
    fun color(colors: EnvColors): Color = when (color) {
        StrokeColor.ACCENT -> colors.accent
        StrokeColor.TEXT -> colors.textPrimary
        else -> Color(color.argb ?: 0)
    }

    companion object {
        val Default = EnvStroke(StrokeWidth.DEFAULT_DP.dp, StrokeColor.ACCENT)
    }
}

val LocalEnvStroke = staticCompositionLocalOf { EnvStroke.Default }
