package com.mushinlab.launcher.feature.settings.tips

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mushinlab.launcher.core.ui.MushinColors

internal fun DrawScope.finger(colors: MushinColors, at: Offset, press: Float, visible: Float = 1f) {
    if (visible <= 0f) return
    val ring = FINGER_RADIUS + FINGER_SPREAD * press
    drawCircle(
        colors.textPrimary,
        radius = FINGER_RADIUS,
        center = at,
        alpha = visible * (FINGER_FILL + FINGER_PRESSED_FILL * press),
    )
    drawCircle(
        colors.textPrimary,
        radius = ring,
        center = at,
        alpha = visible * (FINGER_RING - FINGER_RING_FADE * press),
        style = Stroke(width = FINGER_STROKE),
    )
}

@Immutable
internal data class TrailLook(val color: Color, val width: Float, val dots: Boolean)

internal fun DrawScope.trail(line: Polyline, progress: Float, look: TrailLook, alpha: Float) {
    if (progress <= 0f || alpha <= 0f) return
    val points = line.reached(progress)
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    val stroke = Stroke(width = look.width, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(path, look.color, alpha = alpha, style = stroke)
    if (look.dots) drawCircle(look.color, radius = look.width * DOT_TO_WIDTH, center = points.first(), alpha = alpha)
}

private const val FINGER_RADIUS = 8f
private const val FINGER_SPREAD = 5f
private const val FINGER_FILL = 0.22f
private const val FINGER_PRESSED_FILL = 0.25f
private const val FINGER_RING = 0.7f
private const val FINGER_RING_FADE = 0.4f
private const val FINGER_STROKE = 1.5f
private const val DOT_TO_WIDTH = 0.9f
