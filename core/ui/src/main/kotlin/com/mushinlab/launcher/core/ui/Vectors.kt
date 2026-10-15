package com.mushinlab.launcher.core.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mushinlab.launcher.core.model.ShapeVertex

fun DrawScope.drawVectors(vertices: List<ShapeVertex>, color: Color, widthPx: Float, dots: Boolean) {
    if (vertices.isEmpty()) return
    if (vertices.size > 1) {
        val path = Path().apply {
            moveTo(vertices.first().x, vertices.first().y)
            vertices.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, color, style = Stroke(widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    if (!dots) return
    vertices.forEachIndexed { index, vertex ->
        val center = Offset(vertex.x, vertex.y)
        when {
            index == 0 -> drawCircle(color, radius = widthPx * START_DOT, center = center)

            vertex.dwellMs >= VectorDots.DWELL_MARK_MS -> drawCircle(
                color,
                radius = widthPx * VectorDots.ringScale(vertex.dwellMs),
                center = center,
                style = Stroke(widthPx / 2),
            )

            else -> drawCircle(color, radius = widthPx * VERTEX_DOT, center = center)
        }
    }
}

object VectorDots {
    const val DWELL_MARK_MS = 300L
    private const val RING_BASE = 1.8f
    private const val RING_PER_SECOND = 1f
    private const val RING_CAP_MS = 2000L
    private const val MS_PER_SECOND = 1000f

    fun ringScale(dwellMs: Long): Float =
        RING_BASE + RING_PER_SECOND * dwellMs.coerceAtMost(RING_CAP_MS) / MS_PER_SECOND
}

private const val START_DOT = 1.6f
private const val VERTEX_DOT = 0.9f
