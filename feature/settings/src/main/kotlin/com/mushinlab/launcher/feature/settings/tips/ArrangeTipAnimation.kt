package com.mushinlab.launcher.feature.settings.tips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinColors
import com.mushinlab.launcher.feature.settings.R

@Composable
internal fun ArrangeTipAnimation(modifier: Modifier = Modifier) {
    val colors = LocalMushinColors.current
    val text = rememberMiniText()
    val apps = stringArrayResource(R.array.tip_apps).toList()
    val add = listOf(stringResource(R.string.tip_add_folder), stringResource(R.string.tip_add_app))
    val labels = remember(text, apps, add) { ArrangeLabels(text, apps, add) }
    TipCanvas(LOOP_MS, STILL_MS, modifier) { time -> drawArrangeTip(time, colors, labels) }
}

@Immutable
private class ArrangeLabels(text: MiniText, apps: List<String>, add: List<String>) {
    val apps = apps.take(ROWS).map { text.of(it, Scene.ROW_SIZE, maxWidth = LABEL_WIDTH) }
    val add = AddLabels(
        text.of(add[0], ADD_TEXT, maxWidth = Scene.WIDTH / 2 - Scene.MARGIN),
        text.of(add[1], ADD_TEXT, maxWidth = Scene.WIDTH / 2 - Scene.MARGIN),
    )
    val clock = MiniClock(text.of(CLOCK_TEXT, Scene.CLOCK_SIZE))
}

private fun DrawScope.drawArrangeTip(time: Float, colors: MushinColors, labels: ArrangeLabels) {
    val edit = ease(time, TAP_EDIT + EDIT_DELAY, TAP_EDIT + EDIT_DELAY + EDIT_MS) -
        ease(time, TAP_DONE + EDIT_DELAY, TAP_DONE + EDIT_DELAY + EDIT_MS)
    val drag = Scene.ROW_HEIGHT * MOVED_ROWS * ease(time, DRAG_FROM, DRAG_TO)
    val removed = ease(time, TAP_REMOVE + EDIT_DELAY, TAP_REMOVE + EDIT_DELAY + EDIT_MS)
    val swiped = ease(time, SWIPE_FROM, SWIPE_TO)
    scene(colors, loopFade(time, LOOP_MS)) {
        clockCell(colors, labels.clock, edit)
        listHandle(colors, MiniHome.handle(MiniHome.LIST_TOP), edit, 0f)
        for (index in 1 until ROWS) {
            val passed = ((drag / Scene.ROW_HEIGHT - (index - HALF_ROW)) / SHIFT_SPAN).coerceIn(0f, 1f)
            val shift = if (index <= MOVED_ROWS) Scene.ROW_HEIGHT * passed else 0f
            val last = index == ROWS - 1
            val state = RowState(edit, if (last) 1f - removed else 1f, 0f, if (last) swiped else 0f)
            arrangeRow(colors, labels.apps[index], rowTop(index) - shift, state)
        }
        arrangeRow(
            colors,
            labels.apps.first(),
            rowTop(0) + drag,
            RowState(edit, 1f, hold(time, DRAG_FROM - PRESS_LEAD, DRAG_TO), 0f),
        )
        addRow(colors, rowTop(ROWS) - Scene.ROW_HEIGHT * removed, labels.add, edit)
        editBar(colors, edit)
        finger(
            colors,
            Finger.at(time),
            maxOf(
                pulse(time, TAP_EDIT, TAP_REMOVE, TAP_DONE),
                hold(time, DRAG_FROM - PRESS_LEAD, DRAG_TO),
                hold(time, SWIPE_FROM - PRESS_LEAD, SWIPE_TO),
            ),
            phase(time, 0, FADE_MS) - phase(time, FINGER_OUT, FINGER_OUT + FADE_MS),
        )
    }
}

@Immutable
private data class RowState(val edit: Float, val shown: Float, val lifted: Float, val swiped: Float)

private fun DrawScope.arrangeRow(colors: MushinColors, label: TextLayoutResult, top: Float, state: RowState) {
    if (state.shown <= 0f) return
    val size = Size(Scene.WIDTH, Scene.ROW_HEIGHT)
    translate(left = -MiniHome.ROW_REVEAL * state.swiped) {
        drawRect(colors.zebraA, Offset(0f, top), size, alpha = state.shown)
        if (state.lifted > 0f) drawRect(colors.accent, Offset(0f, top), size, alpha = LIFT_ALPHA * state.lifted)
        text(
            label,
            colors.textPrimary,
            Offset(Scene.MARGIN + MiniHome.ROW_INSET * state.edit, top + (Scene.ROW_HEIGHT - label.size.height) / 2),
            state.shown,
        )
        rowControls(colors, top, state.edit * state.shown)
    }
    removeStrip(colors, top, state.swiped * state.shown)
}

private fun rowTop(index: Int) = MiniHome.LIST_TOP + Scene.ROW_HEIGHT * index

private val Handle = MiniHome.rowHandle(Scene.rowCenterY(MiniHome.LIST_TOP, 0))
private val Remove = MiniHome.rowRemove(Scene.rowCenterY(MiniHome.LIST_TOP, ROWS - 1))
private val Swipe = Offset(Scene.WIDTH * SWIPE_START, Scene.rowCenterY(MiniHome.LIST_TOP, ROWS - 1))

private val Finger = Track(
    listOf(
        Key(0, Scene.rest),
        Key(400, Scene.rest),
        Key(900, MiniHome.edit),
        Key(1250, MiniHome.edit),
        Key(1750, Handle),
        Key(DRAG_FROM, Handle),
        Key(DRAG_TO, Handle + Offset(0f, Scene.ROW_HEIGHT * MOVED_ROWS)),
        Key(3100, Handle + Offset(0f, Scene.ROW_HEIGHT * MOVED_ROWS)),
        Key(3450, Swipe),
        Key(SWIPE_FROM, Swipe),
        Key(SWIPE_TO, Swipe - Offset(SWIPE_PULL, 0f)),
        Key(4050, Swipe - Offset(SWIPE_PULL, 0f)),
        Key(4350, Remove),
        Key(4700, Remove),
        Key(5200, MiniHome.edit),
        Key(5550, MiniHome.edit),
        Key(6050, Scene.rest),
    ),
)

private const val CLOCK_TEXT = "12:45"
private const val LOOP_MS = 7050
private const val STILL_MS = 6150
private const val EDIT_DELAY = 50
private const val EDIT_MS = 200
private const val PRESS_LEAD = 50
private const val FADE_MS = 300
private const val TAP_EDIT = 950
private const val DRAG_FROM = 1950
private const val DRAG_TO = 2900
private const val SWIPE_FROM = 3550
private const val SWIPE_TO = 3950
private const val SWIPE_START = 0.7f
private const val SWIPE_PULL = 48f
private const val TAP_REMOVE = 4400
private const val TAP_DONE = 5250
private const val FINGER_OUT = 5550
private const val ROWS = 4
private const val MOVED_ROWS = 2
private const val HALF_ROW = 0.5f
private const val SHIFT_SPAN = 0.4f
private const val LIFT_ALPHA = 0.2f
private const val LABEL_WIDTH = 96f
private const val ADD_TEXT = 7f
