package com.mushinlab.launcher.core.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/** Draws a continuous brush silhouette, tapering towards older points without overlapping segment caps. */
fun DrawScope.drawBrushTrail(points: List<Offset>, color: Color, widthPx: Float, strength: (Int) -> Float) {
    val outline = brushOutline(points, widthPx, strength)
    if (outline.isEmpty()) return
    val path = Path()
    path.moveTo(outline.first().x, outline.first().y)
    outline.forEachIndexed { index, point ->
        val next = outline[(index + 1) % outline.size]
        val middle = (point + next) / 2f
        path.quadraticBezierTo(point.x, point.y, middle.x, middle.y)
    }
    path.close()
    drawPath(path, color)
}

internal fun brushOutline(points: List<Offset>, widthPx: Float, strength: (Int) -> Float): List<Offset> {
    val distinct = points.indices.filter { it == 0 || points[it] != points[it - 1] }
    if (distinct.size < 2) return emptyList()
    val left = ArrayList<Offset>(distinct.size)
    val right = ArrayList<Offset>(distinct.size)
    distinct.forEachIndexed { position, index ->
        val before = points[distinct[(position - 1).coerceAtLeast(0)]]
        val after = points[distinct[(position + 1).coerceAtMost(distinct.lastIndex)]]
        val direction = after - before
        val distance = direction.getDistance()
        val normal = if (distance > 0f) Offset(-direction.y, direction.x) / distance else Offset.Zero
        val radius = widthPx * strength(index).coerceIn(0f, 1f) / 2
        left += points[index] + normal * radius
        right += points[index] - normal * radius
    }
    return left + right.asReversed()
}
