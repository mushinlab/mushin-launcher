package com.mushinlab.launcher.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A radio-button ring, filled with a dot and accent-colored when [selected]. */
@Composable
fun RadioMark(selected: Boolean, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    val colors = LocalMushinColors.current
    val ring = if (selected) colors.accent else colors.textSecondary
    Canvas(modifier.size(size)) {
        val stroke = this.size.width * RADIO_STROKE
        drawCircle(ring, radius = this.size.width / 2 - stroke / 2, style = Stroke(stroke))
        if (selected) drawCircle(ring, radius = this.size.width * RADIO_DOT)
    }
}

private const val RADIO_STROKE = 0.11f
private const val RADIO_DOT = 0.22f
