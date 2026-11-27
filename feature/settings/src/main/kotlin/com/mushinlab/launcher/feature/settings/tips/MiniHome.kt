package com.mushinlab.launcher.feature.settings.tips

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import com.mushinlab.launcher.core.ui.MushinColors

internal object MiniHome {
    const val LIST_TOP = 100f
    const val GLYPH_RADIUS = 10f
    const val TOOL_LEFT = 20f
    const val TOOL_STEP = 24f
    const val TOOL_TEXT = 1
    const val TOOL_THEME = 2
    const val TOOL_MOTION = 3
    const val GRIP_Y = 8f
    const val ADD_GLYPH_Y = 8f
    const val ROW_INSET = 16f
    const val ROW_HANDLE_X = 16f
    const val ROW_REMOVE_X = Scene.WIDTH - 16f
    const val ROW_REVEAL = 32f
    val edit = Scene.pencil

    fun tool(index: Int) = Offset(TOOL_LEFT + TOOL_STEP * index, edit.y)

    fun grip(sheetTop: Float) = Offset(Scene.WIDTH / 2, sheetTop + GRIP_Y)

    fun rowHandle(centerY: Float) = Offset(ROW_HANDLE_X, centerY)

    fun rowRemove(centerY: Float) = Offset(ROW_REMOVE_X, centerY)

    fun addButton(rowTop: Float, half: Int) = Offset(Scene.WIDTH * (half * 2 + 1) / 2 / 2, rowTop + ADD_GLYPH_Y)
}

@Immutable
internal class MiniClock(val layout: TextLayoutResult) {
    val width = layout.size.width.toFloat()
    val height = layout.size.height + Scene.DATE_GAP + Scene.dateSize.height
}

internal fun sheetOffset(top: Float, open: Float, dragged: Float, closed: Float): Float {
    val hidden = Scene.HEIGHT - top
    return mix(hidden * (1f - open) + dragged, hidden, closed)
}

internal fun DrawScope.editBar(colors: MushinColors, edit: Float) {
    if (edit > 0f) {
        val tint = colors.textPrimary
        repeat(TOOLS) { glyphCircle(colors, MiniHome.tool(it), MiniHome.GLYPH_RADIUS, edit) }
        imageGlyph(MiniHome.tool(0), tint, edit)
        textGlyph(MiniHome.tool(MiniHome.TOOL_TEXT), tint, edit)
        paletteGlyph(MiniHome.tool(MiniHome.TOOL_THEME), tint, edit)
        motionGlyph(MiniHome.tool(MiniHome.TOOL_MOTION), tint, edit)
    }
    glyphCircle(colors, MiniHome.edit, MiniHome.GLYPH_RADIUS)
    pencil(MiniHome.edit, colors.textSecondary, (1f - edit) * BAR_ALPHA)
    if (edit > 0f) checkGlyph(MiniHome.edit, colors.accent, edit)
}

private fun DrawScope.motionGlyph(center: Offset, color: Color, alpha: Float) {
    drawCircle(color, radius = MOTION_RADIUS, center = center + Offset(MOTION_SHIFT, 0f), alpha = alpha)
    MotionLines.forEach { (y, length) ->
        drawLine(
            color,
            Offset(center.x - MOTION_TAIL, center.y + y),
            Offset(center.x - MOTION_TAIL + length, center.y + y),
            MOTION_WIDTH,
            StrokeCap.Round,
            alpha = alpha,
        )
    }
}

internal fun DrawScope.clockCell(colors: MushinColors, clock: MiniClock, edit: Float) {
    if (edit > 0f) {
        val boxLeft = Offset(Scene.clock.x - SLOT_PAD, Scene.clock.y - SLOT_PAD)
        val box = Size(clock.width + SLOT_PAD * 2, clock.height + SLOT_PAD * 2)
        drawRoundRect(colors.accent, boxLeft, box, CornerRadius(SLOT_CORNER), alpha = edit * SLOT_FILL)
        drawRoundRect(
            colors.accent,
            boxLeft,
            box,
            CornerRadius(SLOT_CORNER),
            alpha = edit * SLOT_BORDER,
            style = Stroke(width = OUTLINE_WIDTH),
        )
    }
    clockFace(colors, clock.layout, Scene.clock)
}

internal fun DrawScope.rowControls(colors: MushinColors, top: Float, edit: Float) {
    if (edit <= 0f) return
    val centerY = top + Scene.ROW_HEIGHT / 2
    dragGlyph(MiniHome.rowHandle(centerY), colors.textSecondary, edit)
}

internal fun DrawScope.removeStrip(colors: MushinColors, top: Float, swiped: Float) {
    if (swiped <= 0f) return
    val width = MiniHome.ROW_REVEAL * swiped
    drawRect(
        colors.danger,
        Offset(Scene.WIDTH - width, top),
        Size(width, Scene.ROW_HEIGHT),
        alpha = STRIP_ALPHA * swiped,
    )
    val remove = MiniHome.rowRemove(top + Scene.ROW_HEIGHT / 2)
    glyphCircle(colors, remove, ROW_GLYPH_RADIUS, swiped)
    trashGlyph(remove, colors.danger, swiped)
}

internal fun DrawScope.addRow(colors: MushinColors, top: Float, index: Int, labels: AddLabels, alpha: Float) {
    if (alpha <= 0f) return
    drawRect(colors.zebra(index), Offset(0f, top), Size(Scene.WIDTH, Scene.ROW_HEIGHT), alpha = alpha)
    listOf(labels.folder, labels.app).forEachIndexed { half, label ->
        val glyph = MiniHome.addButton(top, half)
        glyphCircle(colors, glyph, PLUS_RADIUS, alpha)
        if (half == 0) folderGlyph(glyph, colors.textSecondary.copy(alpha = alpha))
        if (half == 1) plusGlyph(glyph, colors.textSecondary.copy(alpha = alpha), PLUS_ARM)
        text(label, colors.textPrimary, Offset(glyph.x - label.size.width / 2f, glyph.y + ADD_LABEL_GAP), alpha)
    }
}

@Immutable
internal class AddLabels(val folder: TextLayoutResult, val app: TextLayoutResult)

internal fun DrawScope.homeSheet(
    colors: MushinColors,
    top: Float,
    offset: Float,
    title: TextLayoutResult,
    content: DrawScope.() -> Unit,
) {
    val hidden = Scene.HEIGHT - top
    if (offset >= hidden) return
    drawRect(Color.Black, size = Size(Scene.WIDTH, Scene.HEIGHT), alpha = SHEET_SCRIM * (1f - offset / hidden))
    translate(top = offset) {
        val corner = CornerRadius(SHEET_CORNER)
        val size = Size(Scene.WIDTH, hidden + SHEET_CORNER)
        drawRoundRect(colors.background, Offset(0f, top), size, corner)
        drawRoundRect(
            colors.textSecondary,
            Offset(0f, top),
            size,
            corner,
            alpha = OUTLINE_ALPHA,
            style = Stroke(width = OUTLINE_WIDTH),
        )
        val grip = MiniHome.grip(top)
        drawRoundRect(
            colors.textSecondary,
            Offset(grip.x - GRIP_WIDTH / 2, grip.y - GRIP_HEIGHT / 2),
            Size(GRIP_WIDTH, GRIP_HEIGHT),
            CornerRadius(GRIP_HEIGHT / 2),
            alpha = GRIP_ALPHA,
        )
        text(title, colors.textPrimary, Offset(Scene.MARGIN, top + SHEET_TITLE_Y))
        content()
    }
}

private const val TOOLS = 4
private const val MOTION_RADIUS = 2.8f
private const val MOTION_SHIFT = 1.8f
private const val MOTION_TAIL = 5f
private const val MOTION_WIDTH = 1.3f
private val MotionLines = listOf(-2.4f to 2.2f, 0f to 3f, 2.4f to 2.2f)
private const val BAR_ALPHA = 0.7f
private const val OUTLINE_ALPHA = 0.5f
private const val OUTLINE_WIDTH = 0.8f
private const val SLOT_PAD = 3f
private const val SLOT_CORNER = 6f
private const val SLOT_FILL = 0.08f
private const val SLOT_BORDER = 0.5f
private const val ROW_GLYPH_RADIUS = 8f
private const val STRIP_ALPHA = 0.28f
private const val PLUS_RADIUS = 6f
private const val PLUS_ARM = 2.6f
private const val ADD_LABEL_GAP = 6f
private const val SHEET_SCRIM = 0.15f
private const val SHEET_CORNER = 8f
private const val SHEET_TITLE_Y = 16f
private const val GRIP_WIDTH = 18f
private const val GRIP_HEIGHT = 2f
private const val GRIP_ALPHA = 0.5f
