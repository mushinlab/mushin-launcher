package com.mushinlab.launcher.feature.settings.tips

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mushinlab.launcher.core.ui.MushinColors

internal fun DrawScope.glyphCircle(colors: MushinColors, center: Offset, radius: Float, alpha: Float = 1f) {
    drawCircle(colors.surface, radius = radius, center = center, alpha = alpha)
    drawCircle(
        colors.textSecondary,
        radius = radius,
        center = center,
        alpha = alpha * OUTLINE_ALPHA,
        style = Stroke(width = OUTLINE_WIDTH),
    )
}

internal fun DrawScope.imageGlyph(center: Offset, color: Color, alpha: Float) {
    drawRoundRect(
        color,
        Offset(center.x - FRAME_HALF, center.y - FRAME_HALF),
        Size(FRAME_HALF * 2, FRAME_HALF * 2),
        CornerRadius(FRAME_CORNER),
        alpha = alpha,
        style = GlyphStroke,
    )
    drawPath(shape(center, Mountain).apply { close() }, color, alpha = alpha)
    drawCircle(color, radius = DOT_RADIUS, center = center + Sun, alpha = alpha)
}

internal fun DrawScope.textGlyph(center: Offset, color: Color, alpha: Float) {
    TextStrokes.forEach { line -> drawPath(shape(center, line), color, alpha = alpha, style = GlyphStroke) }
}

internal fun DrawScope.paletteGlyph(center: Offset, color: Color, alpha: Float) {
    drawCircle(color, radius = PALETTE_RADIUS, center = center, alpha = alpha, style = GlyphStroke)
    PaletteDots.forEach { drawCircle(color, radius = DOT_RADIUS, center = center + it, alpha = alpha) }
}

internal fun DrawScope.checkGlyph(center: Offset, color: Color, alpha: Float) {
    drawPath(shape(center, Check), color, alpha = alpha, style = GlyphStroke)
}

internal fun DrawScope.plusGlyph(center: Offset, color: Color, arm: Float) {
    drawLine(color, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), GLYPH_WIDTH, StrokeCap.Round)
    drawLine(color, Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), GLYPH_WIDTH, StrokeCap.Round)
}

private fun shape(center: Offset, points: List<Offset>): Path = Path().apply {
    moveTo(center.x + points.first().x, center.y + points.first().y)
    points.drop(1).forEach { lineTo(center.x + it.x, center.y + it.y) }
}

private val GlyphStroke = Stroke(width = 1.3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
private val Mountain =
    listOf(Offset(-3.5f, 3.5f), Offset(-1f, 0f), Offset(0.8f, 2f), Offset(2f, 0.8f), Offset(3.5f, 3.5f))
private val Sun = Offset(1.8f, -2f)
private val TextStrokes = listOf(
    listOf(Offset(-5f, -4f), Offset(1f, -4f)),
    listOf(Offset(-2f, -4f), Offset(-2f, 4.5f)),
    listOf(Offset(2f, 0f), Offset(5.5f, 0f)),
    listOf(Offset(3.75f, -1.5f), Offset(3.75f, 4.5f)),
)
private val PaletteDots = listOf(Offset(-2.6f, 0.6f), Offset(-1.2f, -2.4f), Offset(1.6f, -2.4f), Offset(2.8f, 0.6f))
private val Check = listOf(Offset(-4f, 0.5f), Offset(-1.3f, 3.2f), Offset(4f, -2.8f))

private const val OUTLINE_ALPHA = 0.5f
private const val OUTLINE_WIDTH = 0.8f
private const val GLYPH_WIDTH = 1.3f
private const val FRAME_HALF = 5f
private const val FRAME_CORNER = 1.5f
private const val PALETTE_RADIUS = 5.2f
private const val DOT_RADIUS = 0.9f
