package com.mushinlab.launcher.feature.settings.tips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
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
    val root = listOf(
        stringResource(R.string.settings_style),
        stringResource(R.string.settings_keyboard),
        stringResource(R.string.settings_gestures),
        stringResource(R.string.settings_apps),
    )
    val titles = listOf(stringResource(R.string.settings_title), stringResource(R.string.settings_style))
    val rows = listOf(
        stringResource(R.string.style_font),
        stringResource(R.string.home_text_size),
        stringResource(R.string.home_icons),
    )
    val labels = remember(text, apps, root, titles, rows) { StyleLabels(text, root + titles, rows, apps) }
    val motion = remember(labels) { StyleMotion(labels.clock) }
    TipCanvas(LOOP_MS, STILL_MS, modifier) { time -> drawStyleTip(time, colors, labels, motion) }
}

@Immutable
private class StyleLabels(text: MiniText, pages: List<String>, rows: List<String>, apps: List<String>) {
    private val families = FontChoices.map { MushinFonts.family(it) }
    val root = pages.take(ROOT_ROWS).map { text.of(it, Scene.LABEL_SIZE) }
    val settingsTitle = text.of(pages[ROOT_ROWS], Scene.TITLE_SIZE)
    val styleTitle = text.of(pages[ROOT_ROWS + 1], Scene.TITLE_SIZE)
    val rows = rows.map { text.of(it, Scene.LABEL_SIZE, maxWidth = StyleGeometry.LABEL_WIDTH) }
    val fontNames = FontNames.map { text.of(it, Scene.LABEL_SIZE) }
    val apps = families.map { family -> apps.take(HOME_APPS).map { text.of(it, Scene.ROW_SIZE, family) } }
    val clock = text.of(CLOCK_TEXT, Scene.CLOCK_SIZE, families.last())
}

@Immutable
private class StyleMotion(clock: TextLayoutResult) {
    private val width = clock.size.width.toFloat()
    private val height = clock.size.height.toFloat()
    private val slots = listOf(Scene.MARGIN, (Scene.WIDTH - width) / 2, Scene.WIDTH - Scene.MARGIN - width)

    fun clockLeft(slot: Float): Float = if (slot <= 1f) {
        mix(slots[0], slots[1], slot)
    } else {
        mix(slots[1], slots[2], slot - 1f)
    }

    fun clockCenter(slot: Float) = Offset(clockLeft(slot) + width / 2, Scene.clock.y + height / 2)

    fun slotLeft(index: Int): Float = slots[index]

    val finger = Track(
        listOf(
            Key(0, Scene.rest),
            Key(300, Scene.rest),
            Key(700, Scene.cardCenter(0)),
            Key(1200, Scene.cardCenter(0)),
            Key(1500, StyleGeometry.fontValue),
            Key(2200, StyleGeometry.fontValue),
            Key(2550, StyleGeometry.thumb(0f)),
            Key(3200, StyleGeometry.thumb(1f)),
            Key(3600, StyleGeometry.switch),
            Key(3950, StyleGeometry.switch),
            Key(4300, Scene.back),
            Key(4750, Scene.back),
            Key(5100, clockCenter(0f)),
            Key(6900, clockCenter(2f)),
            Key(7400, Scene.rest),
        ),
    )
}

private fun DrawScope.drawStyleTip(time: Float, colors: MushinColors, labels: StyleLabels, motion: StyleMotion) {
    val family = when {
        time >= TAP_FONT_2 + SWITCH_DELAY -> 2
        time >= TAP_FONT_1 + SWITCH_DELAY -> 1
        else -> 0
    }
    val look = RowLook(
        family = family,
        scale = mix(1f, TEXT_SCALE, ease(time, DRAG_FROM, DRAG_TO)),
        icons = ease(time, TAP_ICONS + SWITCH_DELAY, TAP_ICONS + SWITCH_DELAY + SWITCH_MS),
    )
    val settingsOut = ease(time, TAP_BACK + SLIDE_DELAY, TAP_BACK + SLIDE_DELAY + SLIDE_MS)
    val stylePage = ease(time, TAP_STYLE + SLIDE_DELAY, TAP_STYLE + SLIDE_DELAY + SLIDE_MS)
    scene(colors, loopFade(time, LOOP_MS)) {
        if (settingsOut > 0f) drawStyledHome(time, colors, labels, motion, look)
        if (settingsOut < 1f) {
            translate(left = Scene.WIDTH * settingsOut) {
                page(colors, 0f, labels.settingsTitle) {
                    labels.root.forEachIndexed { index, label ->
                        card(index, colors.surface, label, colors.textPrimary)
                    }
                }
                if (stylePage > 0f) drawStylePage(time, colors, labels, look, Scene.WIDTH * (1f - stylePage))
            }
        }
        val touch = if (time in HOLD_FROM.toFloat()..RELEASE.toFloat()) {
            motion.clockCenter(slot(time))
        } else {
            motion.finger.at(time)
        }
        val press = maxOf(
            pulse(time, TAP_STYLE, TAP_FONT_1, TAP_FONT_2, TAP_ICONS, TAP_BACK),
            hold(time, DRAG_FROM - PRESS_LEAD, DRAG_TO),
            hold(time, HOLD_FROM, RELEASE),
        )
        finger(
            colors,
            touch,
            press,
            phase(time, 0, FINGER_FADE_MS) - phase(time, FINGER_OUT, FINGER_OUT + FINGER_FADE_MS),
        )
    }
}

@Immutable
private data class RowLook(val family: Int, val scale: Float, val icons: Float)

private fun slot(time: Float): Float = ease(time, SLOT_1_FROM, SLOT_1_FROM + SLOT_MS) +
    ease(time, SLOT_2_FROM, SLOT_2_FROM + SLOT_MS)

private fun DrawScope.drawStylePage(
    time: Float,
    colors: MushinColors,
    labels: StyleLabels,
    look: RowLook,
    offsetX: Float,
) {
    page(colors, offsetX, labels.styleTitle) {
        val preview = StyleGeometry.preview
        drawRoundRect(colors.surface, preview.first, preview.second, CornerRadius(CORNER))
        styledRows(colors, labels.apps[look.family].take(PREVIEW_APPS), StyleGeometry.PREVIEW_ROWS_TOP, look)
        StyleGeometry.cards.forEachIndexed { index, (top, height) ->
            drawRoundRect(
                colors.surface,
                Offset(Scene.CARD_INSET, top),
                Size(Scene.WIDTH - Scene.CARD_INSET * 2, height),
                CornerRadius(CORNER),
            )
            text(labels.rows[index], colors.textPrimary, Offset(StyleGeometry.TEXT_X, top + StyleGeometry.TEXT_TOP))
        }
        val name = labels.fontNames[look.family]
        textCenteredY(
            name,
            colors.textSecondary,
            StyleGeometry.VALUE_RIGHT - name.size.width,
            StyleGeometry.fontValue.y,
        )
        val progress = ease(time, DRAG_FROM, DRAG_TO)
        drawRoundRect(
            colors.textSecondary,
            Offset(StyleGeometry.TRACK_LEFT, StyleGeometry.TRACK_Y - 1f),
            Size(StyleGeometry.TRACK_RIGHT - StyleGeometry.TRACK_LEFT, 2f),
            alpha = TRACK_ALPHA,
        )
        val thumb = StyleGeometry.thumb(progress)
        drawLine(colors.accent, Offset(StyleGeometry.TRACK_LEFT, thumb.y), thumb, strokeWidth = 2f)
        drawCircle(colors.accent, radius = THUMB_RADIUS, center = thumb)
        switch(colors, StyleGeometry.switch, look.icons)
    }
}

private fun DrawScope.switch(colors: MushinColors, center: Offset, on: Float) {
    val size = Size(SWITCH_WIDTH, SWITCH_HEIGHT)
    val topLeft = Offset(center.x - SWITCH_WIDTH / 2, center.y - SWITCH_HEIGHT / 2)
    drawRoundRect(colors.keyBackground, topLeft, size, CornerRadius(SWITCH_HEIGHT / 2))
    drawRoundRect(colors.accent, topLeft, size, CornerRadius(SWITCH_HEIGHT / 2), alpha = on)
    val knob = mix(topLeft.x + SWITCH_HEIGHT / 2, topLeft.x + SWITCH_WIDTH - SWITCH_HEIGHT / 2, on)
    drawCircle(
        if (on > HALF) colors.background else colors.textSecondary,
        radius = SWITCH_HEIGHT / 2 - SWITCH_GAP,
        center = Offset(knob, center.y),
    )
}

private fun DrawScope.styledRows(colors: MushinColors, rows: List<TextLayoutResult>, top: Float, look: RowLook) {
    rows.forEachIndexed { index, label ->
        val y = top + Scene.ROW_HEIGHT * index
        val centerY = y + Scene.ROW_HEIGHT / 2
        drawRect(
            colors.zebra(index),
            Offset(Scene.CARD_INSET, y),
            Size(Scene.WIDTH - Scene.CARD_INSET * 2, Scene.ROW_HEIGHT),
        )
        if (look.icons > 0f) {
            drawRoundRect(
                colors.accent,
                Offset(ICON_X, centerY - ICON_SIZE / 2),
                Size(ICON_SIZE, ICON_SIZE),
                CornerRadius(ICON_SIZE / 2 - 1f),
                alpha = look.icons,
            )
        }
        val x = mix(ICON_X, ICON_X + ICON_SHIFT, look.icons)
        scale(look.scale, pivot = Offset(x, centerY)) {
            textCenteredY(label, colors.textPrimary, x, centerY)
        }
    }
}

private fun DrawScope.drawStyledHome(
    time: Float,
    colors: MushinColors,
    labels: StyleLabels,
    motion: StyleMotion,
    look: RowLook,
) {
    val lift = ease(time, LIFT_FROM, LIFT_FROM + SWITCH_MS) - ease(time, RELEASE, RELEASE + SWITCH_MS)
    if (lift > 0f) {
        repeat(SLOTS) { index ->
            drawRoundRect(
                colors.divider,
                Offset(motion.slotLeft(index), Scene.clock.y + labels.clock.size.height + SLOT_GAP),
                Size(labels.clock.size.width.toFloat(), 2f),
                alpha = lift,
            )
        }
    }
    val left = motion.clockLeft(slot(time))
    val center = motion.clockCenter(slot(time))
    scale(1f + LIFT_SCALE * lift, pivot = center) {
        if (lift > 0f) {
            drawRoundRect(
                colors.surface,
                Offset(left - LIFT_PAD, Scene.clock.y - LIFT_PAD),
                Size(labels.clock.size.width + LIFT_PAD * 2, labels.clock.size.height + LIFT_PAD * 2),
                CornerRadius(CORNER),
                alpha = lift,
            )
        }
        clockFace(colors, labels.clock, Offset(left, Scene.clock.y))
    }
    styledRows(colors, labels.apps[look.family], Scene.HOME_ROWS_TOP, look)
    pencil(Scene.pencil, colors.textSecondary)
}

private object StyleGeometry {
    const val LABEL_WIDTH = 100f
    const val TEXT_X = 16f
    const val TEXT_TOP = 7f
    const val VALUE_RIGHT = Scene.WIDTH - 16f
    const val PREVIEW_ROWS_TOP = 48f
    const val TRACK_LEFT = 20f
    const val TRACK_RIGHT = Scene.WIDTH - 20f
    const val TRACK_Y = 202f
    const val THUMB_FROM = 70f
    const val THUMB_TO = 140f
    val preview = Offset(Scene.CARD_INSET, 40f) to Size(Scene.WIDTH - Scene.CARD_INSET * 2, 88f)
    val cards = listOf(140f to 26f, 172f to 42f, 220f to 26f)
    val fontValue = Offset(VALUE_RIGHT - 20f, 153f)
    val switch = Offset(Scene.WIDTH - 30f, 233f)

    fun thumb(progress: Float) = Offset(mix(THUMB_FROM, THUMB_TO, progress), TRACK_Y)
}

private val FontChoices = listOf(AppFont.SYSTEM, AppFont.MONTSERRAT, AppFont.INTER)
private val FontNames = listOf("Roboto", "Montserrat", "Inter")

private const val CLOCK_TEXT = "12:45"
private const val LOOP_MS = 8000
private const val STILL_MS = 7300
private const val SLIDE_MS = 300
private const val SLIDE_DELAY = 100
private const val SWITCH_DELAY = 60
private const val SWITCH_MS = 200
private const val TAP_STYLE = 750
private const val TAP_FONT_1 = 1550
private const val TAP_FONT_2 = 2050
private const val DRAG_FROM = 2600
private const val DRAG_TO = 3200
private const val PRESS_LEAD = 50
private const val TAP_ICONS = 3650
private const val TAP_BACK = 4350
private const val HOLD_FROM = 5100
private const val LIFT_FROM = 5650
private const val SLOT_1_FROM = 5900
private const val SLOT_2_FROM = 6450
private const val SLOT_MS = 400
private const val RELEASE = 6900
private const val FINGER_OUT = 7300
private const val FINGER_FADE_MS = 300
private const val HOME_APPS = 4
private const val ROOT_ROWS = 4
private const val PREVIEW_APPS = 3
private const val SLOTS = 3
private const val TEXT_SCALE = 1.3f
private const val CORNER = 4f
private const val TRACK_ALPHA = 0.35f
private const val THUMB_RADIUS = 5f
private const val SWITCH_WIDTH = 20f
private const val SWITCH_HEIGHT = 11f
private const val SWITCH_GAP = 1.5f
private const val HALF = 0.5f
private const val ICON_X = 14f
private const val ICON_SIZE = 9f
private const val ICON_SHIFT = 14f
private const val SLOT_GAP = 16f
private const val LIFT_SCALE = 0.05f
private const val LIFT_PAD = 4f
