package com.mushinlab.launcher.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Briefcase(color: Color, modifier: Modifier = Modifier, size: Dp = 14.dp) {
    Canvas(modifier.size(size)) {
        val stroke = Stroke(width = this.size.width * 0.1f)
        val w = this.size.width
        val h = this.size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.08f, h * 0.3f),
            size = Size(w * 0.84f, h * 0.6f),
            cornerRadius = CornerRadius(w * 0.1f),
            style = stroke,
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.34f, h * 0.12f),
            size = Size(w * 0.32f, h * 0.18f),
            cornerRadius = CornerRadius(w * 0.05f),
            style = stroke,
        )
        drawLine(color, Offset(w * 0.08f, h * 0.55f), Offset(w * 0.92f, h * 0.55f), stroke.width)
    }
}
