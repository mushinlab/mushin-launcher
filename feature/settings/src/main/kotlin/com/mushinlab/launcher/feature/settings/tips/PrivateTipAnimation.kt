package com.mushinlab.launcher.feature.settings.tips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import com.mushinlab.launcher.core.model.Palettes
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinColors
import com.mushinlab.launcher.feature.settings.R

@Composable
internal fun PrivateTipAnimation(modifier: Modifier = Modifier) {
    val colors = LocalMushinColors.current
    val text = rememberMiniText()
    val apps = stringArrayResource(R.array.tip_apps).toList()
    val names = listOf(
        stringResource(R.string.tip_space_name),
        stringResource(R.string.space_work_default_name),
        stringResource(R.string.private_hint_space),
    )
    val labels = remember(text, apps, names) { PrivateLabels(text, apps, names) }
    val night = remember { MushinColors.from(Palettes.Night) }
    TipCanvas(LOOP_MS, STILL_MS, modifier) { time -> drawPrivateTip(time, colors, night, labels) }
}

@Immutable
private class PrivateLabels(text: MiniText, apps: List<String>, names: List<String>) {
    val home = apps.take(HOME_APPS).map { text.of(it, Scene.ROW_SIZE) }
    val hidden = apps.drop(HOME_APPS).take(PRIVATE_APPS).map { text.of(it, Scene.ROW_SIZE) }
    val spaces = names.take(2).map { text.of(it, Scene.LABEL_SIZE, maxWidth = PrivateGeometry.POPUP_TEXT) }
    val homeName = text.of(names[0], Scene.LABEL_SIZE)
    val privateName = text.of(names[2], Scene.LABEL_SIZE)
    val clock = text.of(CLOCK_TEXT, Scene.CLOCK_SIZE)
    val digits = (0..9).map { text.of(it.toString(), Scene.LABEL_SIZE) }
}

private fun DrawScope.drawPrivateTip(time: Float, colors: MushinColors, night: MushinColors, labels: PrivateLabels) {
    val popup = ease(time, TAP_BAR + OPEN_DELAY, TAP_BAR + OPEN_DELAY + SLIDE_MS)
    val lifted = ease(time, SWIPE_FROM, SWIPE_TO)
    val pin = ease(time, PIN_FROM, PIN_FROM + SLIDE_MS) - ease(time, UNLOCK_FROM, UNLOCK_FROM + FADE_MS)
    val unlocked = ease(time, UNLOCK_FROM, UNLOCK_TO)
    val typed = Keys.count { time >= it.ms + TYPE_DELAY }
    scene(colors, loopFade(time, LOOP_MS)) {
        drawHome(colors, labels.home, labels.homeName, labels.clock)
        if (unlocked > 0f) drawPrivateHome(night, labels, unlocked)
        val shown = popup * (1f - phase(time, SWIPE_TO - FADE_MS, SWIPE_TO))
        if (shown > 0f) {
            drawRect(Color.Black, size = Size(Scene.WIDTH, Scene.HEIGHT), alpha = SCRIM * shown)
            translate(top = -PrivateGeometry.LIFT * lifted + PrivateGeometry.RISE * (1f - popup)) {
                drawSpacePopup(colors, labels.spaces, shown)
            }
        }
        if (pin > 0f) drawPinPad(colors, labels.digits, typed, pin, time)
        finger(
            colors,
            Finger.at(time),
            maxOf(
                pulse(time, TAP_BAR),
                Keys.maxOf { pulse(time, it.ms) },
                hold(time, SWIPE_FROM - PRESS_LEAD, SWIPE_TO),
            ),
            phase(time, FINGER_IN, FINGER_IN + FADE_MS) - phase(time, FINGER_OUT, FINGER_OUT + FADE_MS),
        )
    }
}

private fun DrawScope.drawHome(
    colors: MushinColors,
    rows: List<TextLayoutResult>,
    name: TextLayoutResult,
    clock: TextLayoutResult,
) {
    clockFace(colors, clock, Scene.clock)
    zebraRows(colors, Scene.HOME_ROWS_TOP, rows)
    textCenteredY(name, colors.textPrimary, Scene.MARGIN, Scene.pencil.y)
    editBar(colors, 0f)
}

private fun DrawScope.drawPrivateHome(night: MushinColors, labels: PrivateLabels, alpha: Float) {
    drawRect(night.background, size = Size(Scene.WIDTH, Scene.HEIGHT), alpha = alpha)
    labels.hidden.forEachIndexed { index, label ->
        val top = Scene.HOME_ROWS_TOP + Scene.ROW_HEIGHT * index
        drawRect(night.zebra(index), Offset(0f, top), Size(Scene.WIDTH, Scene.ROW_HEIGHT), alpha = alpha)
        text(label, night.textPrimary, Offset(Scene.MARGIN, top + (Scene.ROW_HEIGHT - label.size.height) / 2), alpha)
    }
    val name = labels.privateName
    text(name, night.textPrimary, Offset(Scene.MARGIN, Scene.pencil.y - name.size.height / 2f), alpha)
    lockGlyph(Offset(Scene.MARGIN + name.size.width + LOCK_GAP, Scene.pencil.y), night.accent, alpha)
}

private fun DrawScope.drawSpacePopup(colors: MushinColors, spaces: List<TextLayoutResult>, alpha: Float) {
    val box = PrivateGeometry.popup
    drawRoundRect(colors.surface, box.topLeft, box.size, CornerRadius(POPUP_CORNER), alpha = alpha)
    drawRoundRect(
        colors.textSecondary,
        box.topLeft,
        box.size,
        CornerRadius(POPUP_CORNER),
        alpha = alpha * OUTLINE_ALPHA,
        style = Stroke(width = OUTLINE_WIDTH),
    )
    spaces.forEachIndexed { index, label ->
        val centerY = box.topLeft.y + POPUP_PADDING + POPUP_ROW * index + POPUP_ROW / 2
        val x = box.topLeft.x + POPUP_PADDING * 2
        text(label, colors.textPrimary, Offset(x, centerY - label.size.height / 2f), alpha)
        if (index == 0) {
            drawCircle(
                colors.accent,
                CURRENT_DOT,
                Offset(box.topLeft.x + box.size.width - POPUP_PADDING * 2, centerY),
                alpha,
            )
        }
    }
    val grip = Offset(box.topLeft.x + box.size.width / 2, box.topLeft.y + GRIP_Y)
    drawRoundRect(
        colors.textSecondary,
        Offset(grip.x - GRIP_WIDTH / 2, grip.y - GRIP_HEIGHT / 2),
        Size(GRIP_WIDTH, GRIP_HEIGHT),
        CornerRadius(GRIP_HEIGHT / 2),
        alpha = alpha * GRIP_ALPHA,
    )
}

private fun DrawScope.drawPinPad(
    colors: MushinColors,
    digits: List<TextLayoutResult>,
    typed: Int,
    alpha: Float,
    time: Float,
) {
    drawRect(colors.background, size = Size(Scene.WIDTH, Scene.HEIGHT), alpha = alpha * PIN_SCRIM)
    translate(top = PrivateGeometry.RISE * (1f - alpha)) {
        repeat(PIN_LENGTH) { index ->
            val center = PrivateGeometry.dot(index)
            if (index < typed) drawCircle(colors.accent, DOT_RADIUS, center, alpha)
            drawCircle(colors.textSecondary, DOT_RADIUS, center, alpha, style = Stroke(width = OUTLINE_WIDTH))
        }
        PadDigits.forEachIndexed { slot, digit ->
            if (digit != null) {
                val center = PrivateGeometry.key(slot)
                val pressed = Keys.firstOrNull { it.at == center }?.let { pulse(time, it.ms) } ?: 0f
                drawCircle(colors.keyBackground, KEY_RADIUS, center, alpha)
                if (pressed > 0f) drawCircle(colors.keyPressed, KEY_RADIUS, center, alpha * pressed)
                val label = digits[digit]
                text(label, colors.textPrimary, center - Offset(label.size.width / 2f, label.size.height / 2f), alpha)
            }
        }
    }
}

private fun DrawScope.lockGlyph(center: Offset, color: Color, alpha: Float) {
    drawRoundRect(
        color,
        Offset(center.x - LOCK_HALF, center.y - LOCK_HALF / 2),
        Size(LOCK_HALF * 2, LOCK_HALF * LOCK_BODY),
        CornerRadius(1f),
        alpha = alpha,
    )
    drawArc(
        color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - LOCK_ARC, center.y - LOCK_HALF / 2 - LOCK_ARC),
        size = Size(LOCK_ARC * 2, LOCK_ARC * 2),
        alpha = alpha,
        style = Stroke(width = OUTLINE_WIDTH * 2),
    )
}

private object PrivateGeometry {
    const val POPUP_LEFT = 24f
    const val POPUP_WIDTH = Scene.WIDTH - POPUP_LEFT * 2
    const val POPUP_HEIGHT = POPUP_ROW * 2 + POPUP_PADDING * 2
    const val POPUP_BOTTOM = Scene.HEIGHT - 34f
    const val POPUP_TEXT = POPUP_WIDTH - 40f
    const val LIFT = 90f
    const val RISE = 24f
    const val DOTS_Y = 130f
    const val DOT_STEP = 16f
    const val KEYS_TOP = 172f
    const val KEY_STEP = 30f

    val popup = Rect(
        Offset(POPUP_LEFT, POPUP_BOTTOM - POPUP_HEIGHT),
        Size(POPUP_WIDTH, POPUP_HEIGHT),
    )

    fun dot(index: Int) = Offset(Scene.WIDTH / 2 + DOT_STEP * (index - (PIN_LENGTH - 1) / 2f), DOTS_Y)

    fun key(slot: Int) = Offset(
        Scene.WIDTH / 2 + KEY_STEP * (slot % PAD_COLUMNS - 1),
        KEYS_TOP + KEY_STEP * (slot / PAD_COLUMNS),
    )
}

private val PadDigits = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, null, 0, null)
private val BarPoint = Offset(40f, Scene.pencil.y)
private val PopupPoint = PrivateGeometry.popup.center
private val Keys = listOf(
    Key(2600, PrivateGeometry.key(0)),
    Key(2950, PrivateGeometry.key(4)),
    Key(3300, PrivateGeometry.key(8)),
    Key(3650, PrivateGeometry.key(10)),
)

private val Finger = Track(
    listOf(
        Key(0, Scene.rest),
        Key(500, Scene.rest),
        Key(950, BarPoint),
        Key(1100, BarPoint),
        Key(1550, PopupPoint),
        Key(SWIPE_FROM, PopupPoint),
        Key(SWIPE_TO, PopupPoint - Offset(0f, PrivateGeometry.LIFT)),
        Key(2350, PopupPoint - Offset(0f, PrivateGeometry.LIFT)),
    ) + Keys.flatMap { listOf(Key(it.ms - KEY_LEAD, it.at), Key(it.ms + KEY_LEAD, it.at)) } + listOf(
        Key(4300, Scene.rest),
    ),
)

private const val CLOCK_TEXT = "12:45"
private const val LOOP_MS = 6400
private const val STILL_MS = 3400
private const val SLIDE_MS = 300
private const val FADE_MS = 300
private const val OPEN_DELAY = 60
private const val TYPE_DELAY = 60
private const val PRESS_LEAD = 50
private const val KEY_LEAD = 60
private const val TAP_BAR = 1000
private const val SWIPE_FROM = 1700
private const val SWIPE_TO = 2100
private const val PIN_FROM = 2000
private const val UNLOCK_FROM = 3850
private const val UNLOCK_TO = 4250
private const val FINGER_IN = 200
private const val FINGER_OUT = 4100
private const val HOME_APPS = 4
private const val PRIVATE_APPS = 2
private const val PIN_LENGTH = 4
private const val SCRIM = 0.15f
private const val PIN_SCRIM = 0.94f
private const val POPUP_ROW = 22f
private const val POPUP_PADDING = 6f
private const val POPUP_CORNER = 8f
private const val CURRENT_DOT = 2.5f
private const val GRIP_Y = 3.5f
private const val GRIP_WIDTH = 14f
private const val GRIP_HEIGHT = 2f
private const val GRIP_ALPHA = 0.5f
private const val OUTLINE_ALPHA = 0.5f
private const val OUTLINE_WIDTH = 0.8f
private const val DOT_RADIUS = 4f
private const val KEY_RADIUS = 11f
private const val LOCK_HALF = 3f
private const val LOCK_ARC = 2.2f
private const val LOCK_GAP = 8f
private const val LOCK_BODY = 1.6f
private const val PAD_COLUMNS = 3
