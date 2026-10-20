package com.mushinlab.launcher.feature.settings.tips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import com.mushinlab.launcher.core.model.AppFont
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinColors
import com.mushinlab.launcher.core.ui.MushinFonts
import com.mushinlab.launcher.feature.settings.R

@Composable
internal fun StyleTipAnimation(modifier: Modifier = Modifier) {
    val colors = LocalMushinColors.current
    val text = rememberMiniText()
    val apps = stringArrayResource(R.array.tip_apps).toList()
    val strings = listOf(
        stringResource(R.string.style_text),
        stringResource(R.string.style_font),
        stringResource(R.string.home_text_size),
        stringResource(R.string.home_text_align),
        stringResource(R.string.home_text_m),
        stringResource(R.string.home_text_align_start),
        stringResource(R.string.home_text_align_center),
    )
    val labels = remember(text, apps, strings) { StyleLabels(text, strings, apps) }
    val motion = remember(labels) { StyleMotion(labels.clock) }
    TipCanvas(LOOP_MS, STILL_MS, modifier) { time -> drawStyleTip(time, colors, labels, motion) }
}

@Immutable
private class StyleLabels(text: MiniText, strings: List<String>, apps: List<String>) {
    private val families = FontChoices.map { MushinFonts.family(it) }
    val title = text.of(strings[0], Scene.TITLE_SIZE)
    val rows = strings.subList(1, SIZE_VALUE).map { text.of(it, SHEET_TEXT, maxWidth = StyleGeometry.LABEL_WIDTH) }
    val size = value(text, strings[SIZE_VALUE])
    val aligns = strings.drop(SIZE_VALUE + 1).map { value(text, it) }
    val fontNames = FontNames.map { value(text, it) }
    val apps = families.map { family -> apps.take(HOME_APPS).map { text.of(it, Scene.ROW_SIZE, family) } }
    val clock = MiniClock(text.of(CLOCK_TEXT, Scene.CLOCK_SIZE, families.last()))

    private fun value(text: MiniText, label: String) = text.of(label, SHEET_TEXT, maxWidth = StyleGeometry.VALUE_WIDTH)
}

@Immutable
private class StyleMotion(clock: MiniClock) {
    val finger = Track(
        listOf(
            Key(0, Scene.rest),
            Key(400, Scene.rest),
            Key(900, MiniHome.edit),
            Key(1250, MiniHome.edit),
            Key(1650, MiniHome.tool(MiniHome.TOOL_TEXT)),
            Key(2050, MiniHome.tool(MiniHome.TOOL_TEXT)),
            Key(2450, StyleGeometry.fontValue),
            Key(3100, StyleGeometry.fontValue),
            Key(3450, StyleGeometry.alignValue),
            Key(3650, StyleGeometry.alignValue),
            Key(SHEET_DRAG_FROM, StyleGeometry.grip),
            Key(SHEET_DRAG_TO, StyleGeometry.grip + Offset(0f, StyleGeometry.SHEET_DRAG)),
            Key(CLOCK_FROM, clock.center(0f)),
            Key(CLOCK_TO, clock.center(MiniClock.LAST_SLOT)),
            Key(LIST_FROM, MiniHome.handle(MiniHome.LIST_TOP)),
            Key(LIST_TO, MiniHome.handle(MiniHome.LIST_TOP + StyleGeometry.LIST_SHIFT)),
            Key(7250, MiniHome.edit),
            Key(7550, MiniHome.edit),
            Key(8000, Scene.rest),
        ),
    )
}

@Immutable
private data class RowLook(val family: Int, val align: Float, val listTop: Float)

private fun DrawScope.drawStyleTip(time: Float, colors: MushinColors, labels: StyleLabels, motion: StyleMotion) {
    val family = when {
        time >= TAP_FONT_2 + SWITCH_DELAY -> 2
        time >= TAP_FONT_1 + SWITCH_DELAY -> 1
        else -> 0
    }
    val look = RowLook(
        family = family,
        align = ease(time, TAP_ALIGN + SWITCH_DELAY, TAP_ALIGN + SWITCH_DELAY + ALIGN_MS),
        listTop = MiniHome.LIST_TOP + StyleGeometry.LIST_SHIFT * ease(time, LIST_FROM, LIST_TO),
    )
    val sheet = sheetOffset(
        StyleGeometry.SHEET_TOP,
        open = ease(time, TAP_TEXT + SLIDE_DELAY, TAP_TEXT + SLIDE_DELAY + SLIDE_MS),
        dragged = StyleGeometry.SHEET_DRAG * ease(time, SHEET_DRAG_FROM, SHEET_DRAG_TO),
        closed = ease(time, SHEET_DRAG_TO, SHEET_DRAG_TO + SLIDE_MS),
    )
    scene(colors, loopFade(time, LOOP_MS)) {
        drawStyledHome(time, colors, labels, look)
        homeSheet(colors, StyleGeometry.SHEET_TOP, sheet, labels.title) { drawTextSheet(colors, labels, look) }
        val press = maxOf(
            pulse(time, TAP_EDIT, TAP_TEXT, TAP_FONT_1, TAP_FONT_2, TAP_ALIGN, TAP_DONE),
            hold(time, SHEET_DRAG_FROM - PRESS_LEAD, SHEET_DRAG_TO),
            hold(time, CLOCK_FROM - PRESS_LEAD, CLOCK_TO),
            hold(time, LIST_FROM - PRESS_LEAD, LIST_TO),
        )
        finger(
            colors,
            motion.finger.at(time),
            press,
            phase(time, 0, FADE_MS) - phase(time, FINGER_OUT, FINGER_OUT + FADE_MS),
        )
    }
}

private fun DrawScope.drawStyledHome(time: Float, colors: MushinColors, labels: StyleLabels, look: RowLook) {
    val edit = ease(time, TAP_EDIT + EDIT_DELAY, TAP_EDIT + EDIT_DELAY + EDIT_MS) -
        ease(time, TAP_DONE + EDIT_DELAY, TAP_DONE + EDIT_DELAY + EDIT_MS)
    editableClock(colors, labels.clock, MiniClock.LAST_SLOT * ease(time, CLOCK_FROM, CLOCK_TO), edit)
    listHandle(colors, MiniHome.handle(look.listTop), edit, hold(time, LIST_FROM - PRESS_LEAD, LIST_TO))
    labels.apps[look.family].forEachIndexed { index, label ->
        val top = look.listTop + Scene.ROW_HEIGHT * index
        val centerY = top + Scene.ROW_HEIGHT / 2
        drawRect(colors.zebra(index), Offset(0f, top), Size(Scene.WIDTH, Scene.ROW_HEIGHT))
        drawRoundRect(
            colors.accent,
            Offset(StyleGeometry.ICON_X, centerY - ICON_SIZE / 2),
            Size(ICON_SIZE, ICON_SIZE),
            CornerRadius(ICON_CORNER),
        )
        val centered = Scene.MARGIN + (StyleGeometry.TEXT_RIGHT - Scene.MARGIN - label.size.width) / 2
        textCenteredY(label, colors.textPrimary, mix(Scene.MARGIN, centered, look.align), centerY)
    }
    editBar(colors, edit)
}

private fun DrawScope.drawTextSheet(colors: MushinColors, labels: StyleLabels, look: RowLook) {
    val values = listOf(labels.fontNames[look.family], labels.size, labels.aligns[if (look.align > 0f) 1 else 0])
    labels.rows.forEachIndexed { index, label ->
        val centerY = StyleGeometry.rowCenterY(index)
        drawRoundRect(
            colors.surface,
            Offset(Scene.CARD_INSET, centerY - StyleGeometry.ROW_HEIGHT / 2),
            Size(Scene.WIDTH - Scene.CARD_INSET * 2, StyleGeometry.ROW_HEIGHT),
            CornerRadius(CARD_CORNER),
        )
        textCenteredY(label, colors.textPrimary, StyleGeometry.TEXT_X, centerY)
        val value = values[index]
        textCenteredY(value, colors.textSecondary, StyleGeometry.VALUE_RIGHT - value.size.width, centerY)
    }
}

private object StyleGeometry {
    const val SHEET_TOP = 214f
    const val SHEET_DRAG = 70f
    const val ROWS_TOP = SHEET_TOP + 44f
    const val ROW_HEIGHT = 24f
    const val ROW_STEP = 28f
    const val TEXT_X = 16f
    const val VALUE_RIGHT = Scene.WIDTH - 16f
    const val LABEL_WIDTH = 92f
    const val VALUE_WIDTH = 52f
    const val VALUE_TAP = 14f
    const val LIST_SHIFT = 30f
    const val ICON_X = Scene.WIDTH - Scene.MARGIN - ICON_SIZE
    const val TEXT_RIGHT = ICON_X - 6f
    val grip = MiniHome.grip(SHEET_TOP)
    val fontValue = Offset(VALUE_RIGHT - VALUE_TAP, rowCenterY(0))
    val alignValue = Offset(VALUE_RIGHT - VALUE_TAP, rowCenterY(2))

    fun rowCenterY(index: Int) = ROWS_TOP + ROW_STEP * index + ROW_HEIGHT / 2
}

private val FontChoices = listOf(AppFont.SYSTEM, AppFont.MONTSERRAT, AppFont.INTER)
private val FontNames = listOf("Roboto", "Montserrat", "Inter")

private const val CLOCK_TEXT = "12:45"
private const val LOOP_MS = 9000
private const val STILL_MS = 8000
private const val SLIDE_MS = 300
private const val SLIDE_DELAY = 100
private const val EDIT_DELAY = 50
private const val EDIT_MS = 200
private const val SWITCH_DELAY = 60
private const val ALIGN_MS = 300
private const val PRESS_LEAD = 50
private const val TAP_EDIT = 950
private const val TAP_TEXT = 1700
private const val TAP_FONT_1 = 2500
private const val TAP_FONT_2 = 2950
private const val TAP_ALIGN = 3500
private const val SHEET_DRAG_FROM = 4000
private const val SHEET_DRAG_TO = 4350
private const val CLOCK_FROM = 4800
private const val CLOCK_TO = 5800
private const val LIST_FROM = 6250
private const val LIST_TO = 6750
private const val TAP_DONE = 7300
private const val FINGER_OUT = 7550
private const val FADE_MS = 300
private const val HOME_APPS = 4
private const val SIZE_VALUE = 4
private const val SHEET_TEXT = 9f
private const val CARD_CORNER = 4f
private const val ICON_SIZE = 9f
private const val ICON_CORNER = 3.5f
