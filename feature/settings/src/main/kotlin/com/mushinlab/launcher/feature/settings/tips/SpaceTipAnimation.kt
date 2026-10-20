package com.mushinlab.launcher.feature.settings.tips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import com.mushinlab.launcher.core.model.Palettes
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinColors
import com.mushinlab.launcher.feature.settings.R

@Composable
internal fun SpaceTipAnimation(modifier: Modifier = Modifier) {
    val colors = LocalMushinColors.current
    val text = rememberMiniText()
    val apps = stringArrayResource(R.array.tip_apps).toList()
    val strings = listOf(
        stringResource(R.string.picker_title),
        stringResource(R.string.style_theme, stringResource(R.string.tip_space_name)),
        stringResource(R.string.home_icons),
        stringResource(R.string.tip_add_apps),
    )
    val labels = remember(text, apps, strings) { SpaceLabels(text, strings, apps) }
    val swatches = remember(colors) {
        listOf(colors) + SwatchPalettes.map(MushinColors::from).filter { it != colors }.take(SWATCHES - 1)
    }
    TipCanvas(LOOP_MS, STILL_MS, modifier) { time -> drawSpaceTip(time, colors, swatches, labels) }
}

@Immutable
private class SpaceLabels(text: MiniText, strings: List<String>, apps: List<String>) {
    val apps = apps.take(PICKER_APPS).map { text.of(it, Scene.ROW_SIZE) }
    val pickerTitle = text.of(strings[0], Scene.TITLE_SIZE)
    val themeTitle = text.of(strings[1], Scene.TITLE_SIZE)
    val icons = text.of(strings[2], Scene.LABEL_SIZE, maxWidth = SpaceGeometry.LABEL_WIDTH)
    val addApps = text.of(strings[3], Scene.ROW_SIZE, maxWidth = SpaceGeometry.LABEL_WIDTH)
    val clock = MiniClock(text.of(CLOCK_TEXT, Scene.CLOCK_SIZE))
}

private fun DrawScope.drawSpaceTip(time: Float, base: MushinColors, swatches: List<MushinColors>, labels: SpaceLabels) {
    val colors = base.mixedWith(swatches[TARGET], ease(time, PALETTE_FROM, PALETTE_TO))
    val picker = slide(time, TAP_ADD) - slide(time, TAP_BACK)
    val sheet = sheetOffset(
        SpaceGeometry.SHEET_TOP,
        open = slide(time, TAP_THEME),
        dragged = SpaceGeometry.SHEET_DRAG * ease(time, DRAG_FROM, DRAG_TO),
        closed = ease(time, DRAG_TO, DRAG_TO + SLIDE_MS),
    )
    scene(base, loopFade(time, LOOP_MS)) {
        drawSpaceHome(time, colors, labels)
        homeSheet(colors, SpaceGeometry.SHEET_TOP, sheet, labels.themeTitle) {
            drawThemeSheet(time, colors, swatches, labels)
        }
        if (picker > 0f) drawPicker(time, colors, labels, Scene.WIDTH * (1f - picker))
        val press = maxOf(
            pulse(time, TAP_EDIT, TAP_THEME, TAP_SWATCH, TAP_ADD, TAP_MAPS, TAP_MUSIC, TAP_BACK, TAP_DONE),
            hold(time, DRAG_FROM - PRESS_LEAD, DRAG_TO),
        )
        finger(
            colors,
            Finger.at(time),
            press,
            phase(time, FINGER_IN, FINGER_IN + FADE_MS) - phase(time, FINGER_OUT, FINGER_OUT + FADE_MS),
        )
    }
}

private fun slide(time: Float, tap: Int): Float = ease(time, tap + SLIDE_DELAY, tap + SLIDE_DELAY + SLIDE_MS)

private fun DrawScope.drawSpaceHome(time: Float, colors: MushinColors, labels: SpaceLabels) {
    val edit = ease(time, TAP_EDIT + EDIT_DELAY, TAP_EDIT + EDIT_DELAY + EDIT_MS) -
        ease(time, TAP_DONE + EDIT_DELAY, TAP_DONE + EDIT_DELAY + EDIT_MS)
    val added = ease(time, ROWS_FROM, ROWS_FROM + SLIDE_MS)
    drawRect(colors.background, size = Size(Scene.WIDTH, Scene.HEIGHT))
    editableClock(colors, labels.clock, 0f, edit)
    listHandle(colors, MiniHome.handle(MiniHome.LIST_TOP), edit, 0f)
    zebraRows(colors, MiniHome.LIST_TOP, labels.apps.take(HOME_APPS_AFTER)) { index ->
        if (index < HOME_APPS_BEFORE) 1f else added
    }
    val rows = mix(HOME_APPS_BEFORE.toFloat(), HOME_APPS_AFTER.toFloat(), added)
    addAppsRow(colors, MiniHome.LIST_TOP + Scene.ROW_HEIGHT * rows, HOME_APPS_BEFORE, labels.addApps)
    editBar(colors, edit)
}

private fun DrawScope.drawThemeSheet(
    time: Float,
    colors: MushinColors,
    swatches: List<MushinColors>,
    labels: SpaceLabels,
) {
    swatches.forEachIndexed { index, swatch ->
        val center = SpaceGeometry.swatch(index)
        drawCircle(swatch.background, radius = SWATCH_RADIUS, center = center)
        drawCircle(swatch.accent, radius = SWATCH_DOT, center = center)
        drawCircle(colors.divider, radius = SWATCH_RADIUS, center = center, style = Stroke(width = 1f))
    }
    val ring = lerp(SpaceGeometry.swatch(0), SpaceGeometry.swatch(TARGET), ease(time, TAP_SWATCH, PALETTE_FROM))
    drawCircle(colors.accent, radius = SWATCH_RING, center = ring, style = Stroke(width = RING_STROKE))
    val top = SpaceGeometry.ICONS_TOP
    drawRoundRect(
        colors.surface,
        Offset(Scene.CARD_INSET, top),
        Size(Scene.WIDTH - Scene.CARD_INSET * 2, Scene.CARD_HEIGHT),
        CornerRadius(CARD_CORNER),
    )
    textCenteredY(labels.icons, colors.textPrimary, Scene.CARD_INSET * 2, top + Scene.CARD_HEIGHT / 2)
    switchOff(colors, Offset(SpaceGeometry.SWITCH_X, top + Scene.CARD_HEIGHT / 2))
}

private fun DrawScope.switchOff(colors: MushinColors, center: Offset) {
    val topLeft = Offset(center.x - SWITCH_WIDTH / 2, center.y - SWITCH_HEIGHT / 2)
    drawRoundRect(colors.keyBackground, topLeft, Size(SWITCH_WIDTH, SWITCH_HEIGHT), CornerRadius(SWITCH_HEIGHT / 2))
    drawCircle(
        colors.textSecondary,
        radius = SWITCH_HEIGHT / 2 - SWITCH_GAP,
        center = Offset(topLeft.x + SWITCH_HEIGHT / 2, center.y),
    )
}

private fun DrawScope.drawPicker(time: Float, colors: MushinColors, labels: SpaceLabels, offsetX: Float) {
    page(colors, offsetX, labels.pickerTitle) {
        zebraRows(colors, Scene.CARD_TOP, labels.apps)
        labels.apps.indices.forEach { index ->
            val checked = when (index) {
                MAPS -> ease(time, TAP_MAPS + CHECK_DELAY, TAP_MAPS + CHECK_DELAY + CHECK_MS)
                MUSIC -> ease(time, TAP_MUSIC + CHECK_DELAY, TAP_MUSIC + CHECK_DELAY + CHECK_MS)
                else -> if (index < HOME_APPS_BEFORE) 1f else 0f
            }
            checkbox(colors, SpaceGeometry.checkbox(index), checked)
        }
    }
}

private fun DrawScope.checkbox(colors: MushinColors, center: Offset, checked: Float) {
    val half = CHECKBOX / 2
    val topLeft = Offset(center.x - half, center.y - half)
    val box = Size(CHECKBOX, CHECKBOX)
    drawRoundRect(colors.textSecondary, topLeft, box, CornerRadius(2f), style = Stroke(width = 1f))
    if (checked > 0f) {
        drawRoundRect(colors.accent, topLeft, box, CornerRadius(2f), alpha = checked)
        val mark = Path().apply {
            moveTo(center.x - half * CHECK_LEFT, center.y)
            lineTo(center.x - half * CHECK_BOTTOM_X, center.y + half * CHECK_BOTTOM_Y)
            lineTo(center.x + half * CHECK_RIGHT, center.y - half * CHECK_TOP)
        }
        drawPath(mark, colors.background, alpha = checked, style = Stroke(width = RING_STROKE, cap = StrokeCap.Round))
    }
}

private fun MushinColors.mixedWith(other: MushinColors, fraction: Float): MushinColors = when (fraction) {
    0f -> this

    1f -> other

    else -> copy(
        background = lerp(background, other.background, fraction),
        zebraA = lerp(zebraA, other.zebraA, fraction),
        zebraB = lerp(zebraB, other.zebraB, fraction),
        textPrimary = lerp(textPrimary, other.textPrimary, fraction),
        textSecondary = lerp(textSecondary, other.textSecondary, fraction),
        accent = lerp(accent, other.accent, fraction),
        divider = lerp(divider, other.divider, fraction),
        surface = lerp(surface, other.surface, fraction),
        keyBackground = lerp(keyBackground, other.keyBackground, fraction),
        keyPressed = lerp(keyPressed, other.keyPressed, fraction),
    )
}

private object SpaceGeometry {
    const val SHEET_TOP = 224f
    const val SHEET_DRAG = 70f
    const val ICONS_TOP = SHEET_TOP + 80f
    const val SWITCH_X = Scene.WIDTH - 30f
    const val LABEL_WIDTH = 120f
    const val SWATCH_LEFT = 24f
    const val SWATCH_STEP = 30f
    const val SWATCH_Y = SHEET_TOP + 56f
    const val CHECKBOX_X = Scene.WIDTH - 22f

    fun swatch(index: Int) = Offset(SWATCH_LEFT + SWATCH_STEP * index, SWATCH_Y)

    fun checkbox(index: Int) = Offset(CHECKBOX_X, Scene.rowCenterY(Scene.CARD_TOP, index))
}

private val SwatchPalettes = listOf(Palettes.Lagoon, Palettes.Sand, Palettes.Plum, Palettes.Mist, Palettes.Moss)
private val Grip = MiniHome.grip(SpaceGeometry.SHEET_TOP)
private val AddRow = Offset(Scene.WIDTH / 2, Scene.rowCenterY(MiniHome.LIST_TOP, HOME_APPS_BEFORE))

private val Finger = Track(
    listOf(
        Key(0, Scene.rest),
        Key(400, Scene.rest),
        Key(900, MiniHome.edit),
        Key(1250, MiniHome.edit),
        Key(1700, MiniHome.tool(MiniHome.TOOL_THEME)),
        Key(2150, MiniHome.tool(MiniHome.TOOL_THEME)),
        Key(2550, SpaceGeometry.swatch(TARGET)),
        Key(2900, SpaceGeometry.swatch(TARGET)),
        Key(DRAG_FROM, Grip),
        Key(DRAG_TO, Grip + Offset(0f, SpaceGeometry.SHEET_DRAG)),
        Key(3800, Grip + Offset(0f, SpaceGeometry.SHEET_DRAG)),
        Key(4200, AddRow),
        Key(4550, AddRow),
        Key(4900, SpaceGeometry.checkbox(MAPS)),
        Key(5150, SpaceGeometry.checkbox(MAPS)),
        Key(5450, SpaceGeometry.checkbox(MUSIC)),
        Key(5650, SpaceGeometry.checkbox(MUSIC)),
        Key(6000, Scene.back),
        Key(6200, Scene.back),
        Key(6800, MiniHome.edit),
        Key(7100, MiniHome.edit),
        Key(7600, Scene.rest),
    ),
)

private const val CLOCK_TEXT = "12:45"
private const val LOOP_MS = 8600
private const val STILL_MS = 7700
private const val SLIDE_MS = 300
private const val SLIDE_DELAY = 100
private const val EDIT_DELAY = 50
private const val EDIT_MS = 200
private const val CHECK_DELAY = 60
private const val CHECK_MS = 160
private const val PRESS_LEAD = 50
private const val TAP_EDIT = 950
private const val TAP_THEME = 1750
private const val TAP_SWATCH = 2600
private const val PALETTE_FROM = 2700
private const val PALETTE_TO = 3100
private const val DRAG_FROM = 3300
private const val DRAG_TO = 3650
private const val TAP_ADD = 4250
private const val TAP_MAPS = 4950
private const val TAP_MUSIC = 5500
private const val TAP_BACK = 6050
private const val ROWS_FROM = 6450
private const val TAP_DONE = 6850
private const val FINGER_IN = 200
private const val FINGER_OUT = 7100
private const val FADE_MS = 300
private const val SWATCHES = 4
private const val TARGET = 2
private const val PICKER_APPS = 6
private const val HOME_APPS_BEFORE = 3
private const val HOME_APPS_AFTER = 5
private const val MAPS = 3
private const val MUSIC = 4
private const val SWATCH_RADIUS = 10f
private const val SWATCH_DOT = 4f
private const val SWATCH_RING = 13f
private const val RING_STROKE = 1.5f
private const val CARD_CORNER = 4f
private const val SWITCH_WIDTH = 20f
private const val SWITCH_HEIGHT = 11f
private const val SWITCH_GAP = 1.5f
private const val CHECKBOX = 10f
private const val CHECK_LEFT = 0.55f
private const val CHECK_BOTTOM_X = 0.1f
private const val CHECK_BOTTOM_Y = 0.45f
private const val CHECK_RIGHT = 0.6f
private const val CHECK_TOP = 0.45f
