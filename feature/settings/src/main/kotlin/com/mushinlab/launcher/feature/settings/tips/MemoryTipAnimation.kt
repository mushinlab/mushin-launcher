package com.mushinlab.launcher.feature.settings.tips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.LocalMushinStroke
import com.mushinlab.launcher.core.ui.MushinColors
import com.mushinlab.launcher.feature.settings.R

@Composable
internal fun MemoryTipAnimation(modifier: Modifier = Modifier) {
    val colors = LocalMushinColors.current
    val stroke = LocalMushinStroke.current
    val text = rememberMiniText()
    val apps = stringArrayResource(R.array.tip_apps).toList()
    val awesome = stringResource(R.string.tip_awesome_app)
    val strings = listOf(
        stringResource(R.string.settings_gestures),
        stringResource(R.string.memory_gestures),
        stringResource(R.string.memory_add_gesture),
        stringResource(R.string.style_stroke),
        stringResource(R.string.memory_pick_title),
        stringResource(R.string.memory_add),
    )
    val labels = remember(text, apps, awesome, strings) { MemoryLabels(text, strings, apps, awesome) }
    val look = TrailLook(stroke.color(colors), stroke.width.value * STROKE_SCALE, stroke.dots)
    TipCanvas(LOOP_MS, STILL_MS, modifier) { time -> drawMemoryTip(time, colors, labels, look) }
}

@Immutable
private class MemoryLabels(text: MiniText, strings: List<String>, apps: List<String>, awesome: String) {
    val memoryTitle = text.of(strings[0], Scene.TITLE_SIZE)
    val gestures = text.of(strings[1], Scene.SMALL_SIZE)
    val add = text.of(strings[2], Scene.LABEL_SIZE)
    val usage = text.of(strings[3], Scene.SMALL_SIZE)
    val pickTitle = text.of(strings[4], Scene.TITLE_SIZE)
    val save = text.of(strings[5], Scene.SMALL_SIZE)
    val awesome = text.of(awesome, Scene.ROW_SIZE)
    val awesomeTitle = text.of(awesome, Scene.TITLE_SIZE)
    val apps = apps.map { text.of(it, Scene.ROW_SIZE) }
    val keys = KeyRows.map { row -> row.map { text.of(it.toString(), KEY_TEXT) } }
    val clock = text.of(CLOCK_TEXT, Scene.CLOCK_SIZE)
}

private fun DrawScope.drawMemoryTip(time: Float, colors: MushinColors, labels: MemoryLabels, look: TrailLook) {
    val settingsOut = ease(time, TAP_SAVE + SLIDE_DELAY, TAP_SAVE + SLIDE_DELAY + SLIDE_MS)
    scene(colors, loopFade(time, LOOP_MS)) {
        if (settingsOut > 0f) drawMemoryHome(time, colors, labels, look)
        if (settingsOut < 1f) {
            translate(left = Scene.WIDTH * settingsOut) { drawMemorySettings(time, colors, labels, look) }
        }
        val touch = when {
            time in KEYBOARD_FROM.toFloat()..KEYBOARD_TO.toFloat() ->
                KeyboardTrail.pointAt(phase(time, KEYBOARD_FROM, KEYBOARD_TO))

            time in HOME_FROM.toFloat()..HOME_TO.toFloat() -> HomeTrail.pointAt(phase(time, HOME_FROM, HOME_TO))

            else -> Finger.at(time)
        }
        val press = maxOf(
            pulse(time, TAP_ADD, TAP_PICK, TAP_SAVE),
            hold(time, KEYBOARD_FROM - PRESS_LEAD, KEYBOARD_TO),
            hold(time, HOME_FROM - PRESS_LEAD, HOME_TO),
        )
        finger(colors, touch, press, phase(time, 0, FADE_MS) - phase(time, FINGER_OUT, FINGER_OUT + FADE_MS))
    }
}

private fun DrawScope.drawMemorySettings(
    time: Float,
    colors: MushinColors,
    labels: MemoryLabels,
    look: TrailLook,
) {
    page(colors, 0f, labels.memoryTitle) {
        text(labels.gestures, colors.accent, MemoryGeometry.gesturesLabel)
        card(1, colors.surface, labels.add, colors.accent)
        text(labels.usage, colors.accent, MemoryGeometry.usageLabel)
        MemoryGeometry.bars.forEach { (y, width) ->
            drawRoundRect(
                colors.textSecondary,
                Offset(Scene.MARGIN, y),
                Size(width, BAR_HEIGHT),
                CornerRadius(BAR_HEIGHT / 2),
                alpha = BAR_ALPHA,
            )
        }
    }
    val picker = ease(time, TAP_ADD + SLIDE_DELAY, TAP_ADD + SLIDE_DELAY + SLIDE_MS)
    if (picker > 0f) {
        page(colors, Scene.WIDTH * (1f - picker), labels.pickTitle) {
            zebraRows(colors, Scene.CARD_TOP, listOf(labels.awesome) + labels.apps.take(PICKER_APPS))
        }
    }
    val sheet = ease(time, TAP_PICK + SLIDE_DELAY, TAP_PICK + SLIDE_DELAY + SLIDE_MS)
    if (sheet > 0f) {
        drawRect(colors.textPrimary, size = Size(Scene.WIDTH, Scene.HEIGHT), alpha = SCRIM_ALPHA * sheet)
        translate(top = (Scene.HEIGHT - MemoryGeometry.SHEET_TOP) * (1f - sheet)) {
            drawSheet(time, colors, labels, look)
        }
    }
}

private fun DrawScope.drawSheet(time: Float, colors: MushinColors, labels: MemoryLabels, look: TrailLook) {
    drawRoundRect(
        colors.surface,
        Offset(0f, MemoryGeometry.SHEET_TOP),
        Size(Scene.WIDTH, Scene.HEIGHT),
        CornerRadius(Scene.CORNER),
    )
    text(labels.awesome, colors.textPrimary, MemoryGeometry.sheetTitle)
    labels.keys.forEachIndexed { row, keys ->
        keys.forEachIndexed { column, key ->
            val center = MemoryGeometry.key(row, column)
            drawRoundRect(
                colors.keyBackground,
                Offset(center.x - MemoryGeometry.KEY_FACE_WIDTH / 2, center.y - MemoryGeometry.KEY_HEIGHT / 2),
                Size(MemoryGeometry.KEY_FACE_WIDTH, MemoryGeometry.KEY_HEIGHT),
                CornerRadius(KEY_CORNER),
            )
            drawText(key, colors.textPrimary, center - Offset(key.size.width / 2f, key.size.height / 2f))
        }
    }
    trail(KeyboardTrail, phase(time, KEYBOARD_FROM, KEYBOARD_TO), look, 1f)
    val button = ease(time, KEYBOARD_TO, KEYBOARD_TO + SWITCH_MS)
    if (button > 0f) {
        val (topLeft, size) = MemoryGeometry.save
        drawRoundRect(colors.accent, topLeft, size, CornerRadius(size.height / 2), alpha = button)
        drawText(
            labels.save,
            colors.background,
            topLeft + Offset((size.width - labels.save.size.width) / 2, (size.height - labels.save.size.height) / 2),
            alpha = button,
        )
    }
}

private fun DrawScope.drawMemoryHome(time: Float, colors: MushinColors, labels: MemoryLabels, look: TrailLook) {
    clockFace(colors, labels.clock, Scene.clock)
    zebraRows(colors, Scene.HOME_ROWS_TOP, labels.apps.take(HOME_APPS))
    pencil(Scene.pencil, colors.textSecondary)
    val fade = 1f - ease(time, TRAIL_FADE_FROM, TRAIL_FADE_FROM + FADE_MS)
    trail(HomeTrail, phase(time, HOME_FROM, HOME_TO), look, fade)
    val open = ease(time, LAUNCH_FROM, LAUNCH_FROM + SLIDE_MS)
    if (open > 0f) {
        val bounds = lerp(MemoryGeometry.drawn, MemoryGeometry.full, open)
        drawRect(colors.accent, bounds.topLeft, bounds.size)
        val title = labels.awesomeTitle
        drawText(
            title,
            colors.background,
            bounds.center - Offset(title.size.width / 2f, title.size.height / 2f),
            alpha = open,
        )
    }
}

private object MemoryGeometry {
    const val SHEET_TOP = 170f
    const val KEYS_TOP = 204f
    const val KEY_STEP_X = (Scene.WIDTH - 8f) / 10f
    const val KEY_STEP_Y = 28f
    const val KEY_HEIGHT = 24f
    const val KEY_FACE_WIDTH = KEY_STEP_X - 2f
    const val KEYS_LEFT = 4f
    val gesturesLabel = Offset(Scene.MARGIN, 40f)
    val usageLabel = Offset(Scene.MARGIN, 112f)
    val bars = listOf(128f to 120f, 140f to 90f, 152f to 60f)
    val sheetTitle = Offset(Scene.MARGIN, SHEET_TOP + 10f)
    val save = Offset(Scene.WIDTH - 52f, Scene.HEIGHT - 34f) to Size(42f, 18f)
    val saveCenter = save.first + Offset(save.second.width / 2, save.second.height / 2)
    val homeCenter = Offset(Scene.WIDTH / 2, 200f)
    val drawn = Rect(homeCenter, 0f)
    val full = Rect(0f, 0f, Scene.WIDTH, Scene.HEIGHT)
    val rowShift = listOf(0f, 0.5f, 1.5f)

    fun key(row: Int, column: Int) = Offset(
        KEYS_LEFT + (column + rowShift[row]) * KEY_STEP_X + KEY_STEP_X / 2,
        KEYS_TOP + row * KEY_STEP_Y + KEY_HEIGHT / 2,
    )

    fun key(letter: Char): Offset {
        val row = KeyRows.indexOfFirst { letter in it }
        return key(row, KeyRows[row].indexOf(letter))
    }
}

private val KeyRows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
private val KeyboardTrail = Polyline("awesome".map(MemoryGeometry::key))
private val HomeTrail = KeyboardTrail.moved(MemoryGeometry.homeCenter)
private val AddCard = Scene.cardCenter(1)
private val PickRow = Offset(Scene.WIDTH / 3, Scene.rowCenterY(Scene.CARD_TOP, 0))

private val Finger = Track(
    listOf(
        Key(0, Scene.rest),
        Key(300, Scene.rest),
        Key(700, AddCard),
        Key(1150, AddCard),
        Key(1450, PickRow),
        Key(1900, PickRow),
        Key(2200, KeyboardTrail.first),
        Key(3550, KeyboardTrail.last),
        Key(3900, MemoryGeometry.saveCenter),
        Key(4350, MemoryGeometry.saveCenter),
        Key(4700, HomeTrail.first),
        Key(5850, HomeTrail.last),
    ),
)

private const val CLOCK_TEXT = "12:45"
private const val LOOP_MS = 7600
private const val STILL_MS = 6400
private const val SLIDE_MS = 300
private const val SLIDE_DELAY = 100
private const val SWITCH_MS = 200
private const val FADE_MS = 300
private const val PRESS_LEAD = 50
private const val TAP_ADD = 750
private const val TAP_PICK = 1500
private const val KEYBOARD_FROM = 2300
private const val KEYBOARD_TO = 3500
private const val TAP_SAVE = 3950
private const val HOME_FROM = 4800
private const val HOME_TO = 5800
private const val TRAIL_FADE_FROM = 5900
private const val LAUNCH_FROM = 6000
private const val FINGER_OUT = 5900
private const val PICKER_APPS = 5
private const val HOME_APPS = 4
private const val KEY_TEXT = 7f
private const val KEY_CORNER = 3f
private const val STROKE_SCALE = 0.6f
private const val BAR_HEIGHT = 5f
private const val BAR_ALPHA = 0.3f
private const val SCRIM_ALPHA = 0.25f
