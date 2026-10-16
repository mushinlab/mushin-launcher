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
    val title = stringResource(R.string.tip_space_name)
    val palette = stringResource(R.string.space_palette)
    val items = stringResource(R.string.space_items)
    val labels = remember(text, apps, title, palette, items) {
        SpaceLabels(
            apps = apps.take(EDITOR_APPS).map { text.of(it, Scene.ROW_SIZE) },
            title = text.of(title, Scene.TITLE_SIZE),
            palette = text.of(palette, Scene.SMALL_SIZE),
            items = text.of(items, Scene.SMALL_SIZE),
            clock = text.of(CLOCK_TEXT, Scene.CLOCK_SIZE),
        )
    }
    val swatches = remember(colors) {
        listOf(colors) + SwatchPalettes.map(MushinColors::from).filter { it != colors }.take(SWATCHES - 1)
    }
    TipCanvas(LOOP_MS, STILL_MS, modifier) { time -> drawSpaceTip(time, colors, swatches, labels) }
}

@Immutable
private class SpaceLabels(
    val apps: List<TextLayoutResult>,
    val title: TextLayoutResult,
    val palette: TextLayoutResult,
    val items: TextLayoutResult,
    val clock: TextLayoutResult,
)

private fun DrawScope.drawSpaceTip(time: Float, base: MushinColors, swatches: List<MushinColors>, labels: SpaceLabels) {
    val chosen = base.mixedWith(swatches[TARGET], ease(time, PALETTE_FROM, PALETTE_TO))
    val editor = ease(time, TAP_EDIT + SLIDE_DELAY, TAP_EDIT + SLIDE_DELAY + SLIDE_MS) -
        ease(time, TAP_BACK + SLIDE_DELAY, TAP_BACK + SLIDE_DELAY + SLIDE_MS)
    val closed = time >= TAP_BACK + SLIDE_DELAY
    scene(base, loopFade(time, LOOP_MS)) {
        val added = if (closed) ease(time, ROWS_FROM, ROWS_FROM + SLIDE_MS) else 0f
        drawSpaceHome(chosen, labels, added)
        if (editor > 0f) drawSpaceEditor(time, chosen, swatches, labels, Scene.WIDTH * (1f - editor))
        finger(
            chosen,
            Finger.at(time),
            pulse(time, TAP_EDIT, TAP_SWATCH, TAP_MAPS, TAP_MUSIC, TAP_BACK),
            phase(time, FINGER_IN, FINGER_IN + FINGER_FADE_MS) - phase(time, FINGER_OUT, FINGER_OUT + FINGER_FADE_MS),
        )
    }
}

private fun DrawScope.drawSpaceHome(colors: MushinColors, labels: SpaceLabels, added: Float) {
    drawRect(colors.background, size = Size(Scene.WIDTH, Scene.HEIGHT))
    clockFace(colors, labels.clock, Scene.clock)
    zebraRows(colors, Scene.HOME_ROWS_TOP, labels.apps.take(HOME_APPS_AFTER)) { index ->
        if (index < HOME_APPS_BEFORE) 1f else added
    }
    pencil(Scene.pencil, colors.textSecondary)
}

private fun DrawScope.drawSpaceEditor(
    time: Float,
    colors: MushinColors,
    swatches: List<MushinColors>,
    labels: SpaceLabels,
    offsetX: Float,
) {
    page(colors, offsetX, labels.title) {
        text(labels.palette, colors.textSecondary, SpaceGeometry.paletteLabel)
        swatches.forEachIndexed { index, swatch ->
            val center = SpaceGeometry.swatch(index)
            drawCircle(swatch.background, radius = SWATCH_RADIUS, center = center)
            drawCircle(swatch.accent, radius = SWATCH_DOT, center = center)
            drawCircle(colors.divider, radius = SWATCH_RADIUS, center = center, style = Stroke(width = 1f))
        }
        val ring = lerp(SpaceGeometry.swatch(0), SpaceGeometry.swatch(TARGET), ease(time, TAP_SWATCH, PALETTE_FROM))
        drawCircle(colors.accent, radius = SWATCH_RING, center = ring, style = Stroke(width = RING_STROKE))
        text(labels.items, colors.textSecondary, SpaceGeometry.itemsLabel)
        zebraRows(colors, SpaceGeometry.LIST_TOP, labels.apps)
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
    const val LIST_TOP = 104f
    const val SWATCH_LEFT = 24f
    const val SWATCH_STEP = 30f
    const val SWATCH_Y = 70f
    const val CHECKBOX_X = Scene.WIDTH - 22f
    val paletteLabel = Offset(Scene.MARGIN, 42f)
    val itemsLabel = Offset(Scene.MARGIN, 90f)

    fun swatch(index: Int) = Offset(SWATCH_LEFT + SWATCH_STEP * index, SWATCH_Y)

    fun checkbox(index: Int) = Offset(CHECKBOX_X, Scene.rowCenterY(LIST_TOP, index))
}

private val SwatchPalettes = listOf(Palettes.Lagoon, Palettes.Sand, Palettes.Plum, Palettes.Mist, Palettes.Moss)

private val Finger = Track(
    listOf(
        Key(0, Scene.rest),
        Key(400, Scene.rest),
        Key(1000, Scene.pencil),
        Key(1600, Scene.pencil),
        Key(2100, SpaceGeometry.swatch(2)),
        Key(2600, SpaceGeometry.swatch(2)),
        Key(3000, SpaceGeometry.checkbox(3)),
        Key(3400, SpaceGeometry.checkbox(3)),
        Key(3700, SpaceGeometry.checkbox(4)),
        Key(4100, SpaceGeometry.checkbox(4)),
        Key(4500, Scene.back),
        Key(5000, Scene.back),
        Key(5500, Scene.rest),
    ),
)

private const val CLOCK_TEXT = "12:45"
private const val LOOP_MS = 7000
private const val STILL_MS = 6000
private const val SLIDE_MS = 300
private const val SLIDE_DELAY = 100
private const val CHECK_DELAY = 60
private const val CHECK_MS = 160
private const val TAP_EDIT = 1050
private const val TAP_SWATCH = 2150
private const val PALETTE_FROM = 2250
private const val PALETTE_TO = 2650
private const val TAP_MAPS = 3050
private const val TAP_MUSIC = 3750
private const val TAP_BACK = 4550
private const val ROWS_FROM = 4950
private const val FINGER_IN = 200
private const val FINGER_OUT = 5500
private const val FINGER_FADE_MS = 300
private const val SWATCHES = 4
private const val TARGET = 2
private const val EDITOR_APPS = 6
private const val HOME_APPS_BEFORE = 3
private const val HOME_APPS_AFTER = 5
private const val MAPS = 3
private const val MUSIC = 4
private const val SWATCH_RADIUS = 10f
private const val SWATCH_DOT = 4f
private const val SWATCH_RING = 13f
private const val RING_STROKE = 1.5f
private const val CHECKBOX = 10f
private const val CHECK_LEFT = 0.55f
private const val CHECK_BOTTOM_X = 0.1f
private const val CHECK_BOTTOM_Y = 0.45f
private const val CHECK_RIGHT = 0.6f
private const val CHECK_TOP = 0.45f
