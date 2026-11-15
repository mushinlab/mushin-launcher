package com.mushinlab.launcher.feature.settings.tips

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextLayoutResult
import com.mushinlab.launcher.core.ui.MushinColors

internal object MiniHome {
    const val LIST_TOP = 100f
    const val HANDLE_ABOVE = 14f
    const val GLYPH_RADIUS = 10f
    const val TOOL_LEFT = 20f
    const val TOOL_STEP = 24f
    const val TOOL_TEXT = 1
    const val TOOL_THEME = 2
    const val GRIP_Y = 8f
    const val ADD_GLYPH_Y = 8f
    const val ROW_INSET = 16f
    const val ROW_HANDLE_X = 16f
    const val ROW_EDIT_X = Scene.WIDTH - 36f
    const val ROW_REMOVE_X = Scene.WIDTH - 16f
    val edit = Scene.pencil

    fun tool(index: Int) = Offset(TOOL_LEFT + TOOL_STEP * index, edit.y)

    fun handle(listTop: Float) = Offset(Scene.WIDTH / 2, listTop - HANDLE_ABOVE)

    fun grip(sheetTop: Float) = Offset(Scene.WIDTH / 2, sheetTop + GRIP_Y)

    fun rowHandle(centerY: Float) = Offset(ROW_HANDLE_X, centerY)

    fun rowEdit(centerY: Float) = Offset(ROW_EDIT_X, centerY)

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
    }
    glyphCircle(colors, MiniHome.edit, MiniHome.GLYPH_RADIUS)
    pencil(MiniHome.edit, colors.textSecondary, (1f - edit) * BAR_ALPHA)
    if (edit > 0f) checkGlyph(MiniHome.edit, colors.accent, edit)
}

internal fun DrawScope.listHandle(colors: MushinColors, center: Offset, visible: Float, active: Float) {
    if (visible <= 0f) return
    val outer = Size(HANDLE_WIDTH + HANDLE_PADDING * 2, HANDLE_THICKNESS + HANDLE_PADDING * 2)
    val outerLeft = Offset(center.x - outer.width / 2, center.y - outer.height / 2)
    drawRoundRect(colors.surface, outerLeft, outer, CornerRadius(outer.height / 2), alpha = visible)
    drawRoundRect(
        colors.textSecondary,
        outerLeft,
        outer,
        CornerRadius(outer.height / 2),
        alpha = visible * OUTLINE_ALPHA,
        style = Stroke(width = OUTLINE_WIDTH),
    )
    drawRoundRect(
        lerp(colors.textSecondary.copy(alpha = HANDLE_ALPHA), colors.accent, active),
        Offset(center.x - HANDLE_WIDTH / 2, center.y - HANDLE_THICKNESS / 2),
        Size(HANDLE_WIDTH, HANDLE_THICKNESS),
        CornerRadius(HANDLE_THICKNESS / 2),
        alpha = visible,
    )
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
    val change = MiniHome.rowEdit(centerY)
    val remove = MiniHome.rowRemove(centerY)
    glyphCircle(colors, change, ROW_GLYPH_RADIUS, edit)
    glyphCircle(colors, remove, ROW_GLYPH_RADIUS, edit)
    scale(ROW_PENCIL_SCALE, pivot = change) { pencil(change, colors.textPrimary, edit) }
    crossGlyph(remove, colors.textPrimary, edit)
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

private const val TOOLS = 3
private const val BAR_ALPHA = 0.7f
private const val OUTLINE_ALPHA = 0.5f
private const val OUTLINE_WIDTH = 0.8f
private const val HANDLE_WIDTH = 18f
private const val HANDLE_THICKNESS = 2f
private const val HANDLE_PADDING = 3f
private const val HANDLE_ALPHA = 0.35f
private const val SLOT_PAD = 3f
private const val SLOT_CORNER = 6f
private const val SLOT_FILL = 0.08f
private const val SLOT_BORDER = 0.5f
private const val ROW_GLYPH_RADIUS = 8f
private const val ROW_PENCIL_SCALE = 0.7f
private const val PLUS_RADIUS = 6f
private const val PLUS_ARM = 2.6f
private const val ADD_LABEL_GAP = 6f
private const val SHEET_SCRIM = 0.15f
private const val SHEET_CORNER = 8f
private const val SHEET_TITLE_Y = 16f
private const val GRIP_WIDTH = 18f
private const val GRIP_HEIGHT = 2f
private const val GRIP_ALPHA = 0.5f
