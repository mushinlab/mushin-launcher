package com.mushinlab.launcher.feature.settings.tips

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.ui.MushinColors
import com.mushinlab.launcher.core.ui.MushinFont
import kotlin.math.min

internal object Scene {
    const val WIDTH = 180f
    const val HEIGHT = 360f
    const val MARGIN = 12f
    const val CORNER = 14f
    const val HEADER_Y = 22f
    const val TITLE_X = 36f
    const val CARD_TOP = 44f
    const val CARD_HEIGHT = 26f
    const val CARD_STEP = 30f
    const val CARD_INSET = 8f
    const val ROW_HEIGHT = 24f
    const val HOME_ROWS_TOP = 84f
    const val TITLE_SIZE = 13f
    const val LABEL_SIZE = 10f
    const val SMALL_SIZE = 8f
    const val ROW_SIZE = 11f
    const val CLOCK_SIZE = 30f
    val back = Offset(16f, HEADER_Y)
    val pencil = Offset(WIDTH - 20f, HEIGHT - 16f)
    val rest = Offset(WIDTH * 0.66f, HEIGHT * 0.84f)
    val clock = Offset(MARGIN, 20f)
    val date = Offset(MARGIN + 1f, 58f)
    val dateSize = Size(56f, 6f)

    fun cardCenter(index: Int) = Offset(WIDTH / 2, CARD_TOP + CARD_STEP * index + CARD_HEIGHT / 2)

    fun rowCenterY(top: Float, index: Int) = top + ROW_HEIGHT * index + ROW_HEIGHT / 2
}

@Immutable
internal class MiniText(private val measurer: TextMeasurer, private val font: MushinFont) {
    fun of(
        text: String,
        size: Float,
        family: FontFamily = font.family,
        weight: FontWeight = font.weight,
        maxWidth: Float = Scene.WIDTH - Scene.MARGIN * 2,
    ): TextLayoutResult = measurer.measure(
        text,
        TextStyle(fontFamily = family, fontWeight = weight, fontSize = size.sp),
        overflow = TextOverflow.Ellipsis,
        softWrap = false,
        maxLines = 1,
        constraints = Constraints(maxWidth = maxWidth.toInt()),
        density = UnitDensity,
    )
}

private val UnitDensity = Density(1f, 1f)

internal fun DrawScope.scene(colors: MushinColors, fade: Float, content: DrawScope.() -> Unit) {
    val factor = min(size.width / Scene.WIDTH, size.height / Scene.HEIGHT)
    val left = (size.width - Scene.WIDTH * factor) / 2
    val top = (size.height - Scene.HEIGHT * factor) / 2
    translate(left, top) {
        scale(factor, factor, pivot = Offset.Zero) {
            val frame = Path().apply {
                addRoundRect(RoundRect(0f, 0f, Scene.WIDTH, Scene.HEIGHT, CornerRadius(Scene.CORNER)))
            }
            clipPath(frame) {
                drawRect(colors.background, size = Size(Scene.WIDTH, Scene.HEIGHT))
                content()
                if (fade < 1f) drawRect(colors.background, size = Size(Scene.WIDTH, Scene.HEIGHT), alpha = 1f - fade)
            }
            drawPath(frame, colors.divider, style = Stroke(width = FRAME_STROKE))
        }
    }
}

internal fun DrawScope.text(layout: TextLayoutResult, color: Color, at: Offset, alpha: Float = 1f) {
    drawText(layout, color, at, alpha = alpha)
}

internal fun DrawScope.textCenteredY(layout: TextLayoutResult, color: Color, x: Float, centerY: Float) {
    drawText(layout, color, Offset(x, centerY - layout.size.height / 2f))
}

internal fun DrawScope.page(
    colors: MushinColors,
    offsetX: Float,
    title: TextLayoutResult,
    content: DrawScope.() -> Unit,
) {
    translate(left = offsetX) {
        drawRect(colors.background, size = Size(Scene.WIDTH, Scene.HEIGHT))
        backArrow(Scene.back, colors.textPrimary, colors.surface)
        textCenteredY(title, colors.textPrimary, Scene.TITLE_X, Scene.HEADER_Y)
        content()
    }
}

internal fun DrawScope.card(index: Int, color: Color, label: TextLayoutResult, textColor: Color) {
    val top = Scene.CARD_TOP + Scene.CARD_STEP * index
    drawRoundRect(
        color,
        topLeft = Offset(Scene.CARD_INSET, top),
        size = Size(Scene.WIDTH - Scene.CARD_INSET * 2, Scene.CARD_HEIGHT),
        cornerRadius = CornerRadius(CARD_CORNER),
    )
    textCenteredY(label, textColor, Scene.CARD_INSET * 2, top + Scene.CARD_HEIGHT / 2)
}

internal fun DrawScope.backArrow(center: Offset, color: Color, circle: Color) {
    drawCircle(circle, radius = BACK_RADIUS, center = center)
    val stroke = Stroke(width = ICON_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val path = Path().apply {
        moveTo(center.x + ARROW_HALF, center.y)
        lineTo(center.x - ARROW_HALF, center.y)
        moveTo(center.x - ARROW_HALF + ARROW_HEAD, center.y - ARROW_HEAD)
        lineTo(center.x - ARROW_HALF, center.y)
        lineTo(center.x - ARROW_HALF + ARROW_HEAD, center.y + ARROW_HEAD)
    }
    drawPath(path, color, style = stroke)
}

internal fun DrawScope.pencil(center: Offset, color: Color) {
    val stroke = Stroke(width = ICON_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val path = Path().apply {
        moveTo(center.x - PENCIL_HALF, center.y + PENCIL_HALF)
        lineTo(center.x - PENCIL_HALF, center.y + PENCIL_HALF - PENCIL_TIP)
        lineTo(center.x + PENCIL_HALF - PENCIL_TIP, center.y - PENCIL_HALF)
        lineTo(center.x + PENCIL_HALF, center.y - PENCIL_HALF + PENCIL_TIP)
        lineTo(center.x - PENCIL_HALF + PENCIL_TIP, center.y + PENCIL_HALF)
        close()
    }
    drawPath(path, color, style = stroke)
}

internal fun DrawScope.zebraRows(
    colors: MushinColors,
    top: Float,
    labels: List<TextLayoutResult>,
    alphas: (Int) -> Float = { 1f },
) {
    labels.forEachIndexed { index, label ->
        val alpha = alphas(index)
        if (alpha > 0f) {
            val y = top + Scene.ROW_HEIGHT * index
            drawRect(
                colors.zebra(index),
                topLeft = Offset(0f, y),
                size = Size(Scene.WIDTH, Scene.ROW_HEIGHT),
                alpha = alpha,
            )
            drawText(
                label,
                colors.textPrimary,
                Offset(Scene.MARGIN, y + (Scene.ROW_HEIGHT - label.size.height) / 2),
                alpha = alpha,
            )
        }
    }
}

internal fun DrawScope.clockFace(colors: MushinColors, clock: TextLayoutResult, at: Offset) {
    drawText(clock, colors.textPrimary, at)
    drawRoundRect(
        colors.textSecondary,
        topLeft = Offset(at.x + 1f, at.y + clock.size.height + DATE_GAP),
        size = Scene.dateSize,
        cornerRadius = CornerRadius(Scene.dateSize.height / 2),
        alpha = DATE_ALPHA,
    )
}

private const val FRAME_STROKE = 1.5f
private const val CARD_CORNER = 4f
private const val BACK_RADIUS = 9f
private const val ICON_STROKE = 1.4f
private const val ARROW_HALF = 4.5f
private const val ARROW_HEAD = 3.5f
private const val PENCIL_HALF = 5.5f
private const val PENCIL_TIP = 3f
private const val DATE_GAP = 4f
private const val DATE_ALPHA = 0.5f
