package com.mushinlab.launcher.feature.search

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import com.mushinlab.launcher.core.model.ShapeVertex
import com.mushinlab.launcher.core.search.GesturePoint
import com.mushinlab.launcher.core.search.KeyAction
import com.mushinlab.launcher.core.search.KeySpec
import com.mushinlab.launcher.core.search.Shapes
import com.mushinlab.launcher.core.search.TimedPoint
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class PressMode { NORMAL, DIGIT, REPEATING, CLEAR, GLIDE, COMMITTED }

private class ActivePress(val keyIndex: Int, val start: Offset, startMs: Long) {
    var mode = PressMode.NORMAL
    var timer: Job? = null
    val points = mutableListOf(GesturePoint(start.x, start.y))
    val timed = mutableListOf(TimedPoint(start.x, start.y, startMs))
    var anchor = start
}

internal suspend fun PointerInputScope.handleKeys(
    keys: List<KeyGeometry>,
    state: KeyboardState,
    callbacks: KeyboardCallbacks,
    vectorTolerance: Float,
) = coroutineScope {
    val handler = KeyboardGestureHandler(
        keys = keys,
        state = state,
        scope = this,
        callbacks = callbacks,
        thresholds = Thresholds(
            digitSwipe = KeyboardDimens.digitSwipe.toPx(),
            clearSwipe = KeyboardDimens.clearSwipe.toPx(),
            glideStart = KeyboardDimens.glideStart.toPx(),
            dwellRadius = viewConfiguration.touchSlop,
            vectorTolerance = vectorTolerance,
        ),
    )
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { change ->
                when {
                    change.pressed && !change.previousPressed -> handler.onDown(
                        change.id,
                        change.position,
                        change.uptimeMillis,
                    )

                    change.pressed -> handler.onMove(change.id, change.position, change.uptimeMillis)

                    else -> handler.onUp(change.id)
                }
                change.consume()
            }
        }
    }
}

private class Thresholds(
    val digitSwipe: Float,
    val clearSwipe: Float,
    val glideStart: Float,
    val dwellRadius: Float,
    val vectorTolerance: Float,
)

internal class KeyboardCallbacks(
    val actions: () -> KeyboardActions,
    val glide: (List<GesturePoint>, List<ShapeVertex>) -> Boolean,
    val haptic: (Int) -> Unit,
)

private class KeyboardGestureHandler(
    private val keys: List<KeyGeometry>,
    private val state: KeyboardState,
    private val scope: CoroutineScope,
    private val callbacks: KeyboardCallbacks,
    private val thresholds: Thresholds,
) {
    private val active = LinkedHashMap<PointerId, ActivePress>()

    fun onDown(id: PointerId, position: Offset, timeMs: Long) {
        active.values.toList().forEach(::commit)
        active.clear()
        state.clearDrawing()
        val index = KeyboardGeometry.hit(keys, position) ?: return
        val press = ActivePress(index, position, timeMs)
        active[id] = press
        state.pressed[index] = true
        callbacks.haptic(HapticFeedbackConstants.KEYBOARD_TAP)
        val spec = keys[index].spec
        press.timer = when {
            spec.digit != null -> scope.launch { holdForDigit(press) }
            spec.action == KeyAction.BACKSPACE -> scope.launch { repeatBackspace(press) }
            else -> null
        }
    }

    fun onMove(id: PointerId, position: Offset, timeMs: Long) {
        val press = active[id] ?: return
        val spec = keys[press.keyIndex].spec
        val delta = position - press.start
        press.points += GesturePoint(position.x, position.y)
        press.timed += TimedPoint(position.x, position.y, timeMs)
        when {
            press.mode == PressMode.GLIDE -> {
                state.trail += position
                if ((position - press.anchor).getDistance() > thresholds.dwellRadius) {
                    press.anchor = position
                    press.timer?.cancel()
                    press.timer = scope.launch { dwell(press) }
                }
            }

            press.mode == PressMode.NORMAL && startsGlide(press, spec, position, delta) -> {
                press.timer?.cancel()
                press.mode = PressMode.GLIDE
                state.pressed.remove(press.keyIndex)
                press.points.forEach { state.trail += Offset(it.x, it.y) }
                press.anchor = position
                press.timer = scope.launch { dwell(press) }
            }

            else -> swipe(press, spec, delta)
        }
    }

    private fun swipe(press: ActivePress, spec: KeySpec, delta: Offset) {
        if (spec.digit != null && delta.y <= -thresholds.digitSwipe && press.mode == PressMode.NORMAL) {
            press.timer?.cancel()
            press.mode = PressMode.DIGIT
            state.bubbleKey = press.keyIndex
        }
        if (spec.action == KeyAction.BACKSPACE && delta.x <= -thresholds.clearSwipe && press.mode != PressMode.CLEAR) {
            press.timer?.cancel()
            press.mode = PressMode.CLEAR
        }
    }

    fun onUp(id: PointerId) {
        active.remove(id)?.let(::commit)
    }

    private fun startsGlide(press: ActivePress, spec: KeySpec, position: Offset, delta: Offset): Boolean {
        val letter = spec.action == null && spec.char != null
        val far = delta.getDistance() >= thresholds.glideStart
        val toDigit = spec.digit != null && delta.y < 0f && abs(delta.x) < -delta.y * KeyboardDimens.VERTICAL_RATIO
        return letter && far && !toDigit && KeyboardGeometry.hit(keys, position) != press.keyIndex
    }

    private suspend fun dwell(press: ActivePress) {
        while (press.mode == PressMode.GLIDE) {
            delay(KeyboardDimens.DWELL_MS)
            if (press.mode != PressMode.GLIDE) return
            press.points += GesturePoint(press.anchor.x, press.anchor.y, repeat = true)
            state.dwells += press.anchor
            callbacks.haptic(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    private suspend fun holdForDigit(press: ActivePress) {
        delay(KeyboardDimens.DIGIT_HOLD_MS)
        if (press.mode == PressMode.NORMAL) {
            press.mode = PressMode.DIGIT
            state.bubbleKey = press.keyIndex
        }
    }

    private suspend fun repeatBackspace(press: ActivePress) {
        delay(KeyboardDimens.REPEAT_START_MS)
        if (press.mode != PressMode.NORMAL) return
        press.mode = PressMode.REPEATING
        while (press.mode == PressMode.REPEATING) {
            if (callbacks.actions().onBackspace()) callbacks.haptic(HapticFeedbackConstants.KEYBOARD_TAP)
            delay(KeyboardDimens.REPEAT_STEP_MS)
        }
    }

    private fun commit(press: ActivePress) {
        press.timer?.cancel()
        val spec = keys[press.keyIndex].spec
        val target = callbacks.actions()
        when (press.mode) {
            PressMode.COMMITTED, PressMode.REPEATING -> Unit

            PressMode.GLIDE -> {
                val vertices = Shapes.vectorize(press.timed, thresholds.vectorTolerance, thresholds.dwellRadius)
                state.trail.clear()
                state.vectors = vertices
                if (!callbacks.glide(press.points, vertices)) callbacks.haptic(HapticFeedbackConstants.REJECT)
            }

            PressMode.CLEAR -> target.onClear()

            PressMode.DIGIT -> spec.digit?.let(::type)

            PressMode.NORMAL -> when (spec.action) {
                KeyAction.BACKSPACE -> target.onBackspace()
                KeyAction.SWITCH_LAYOUT -> target.onSwitchLayout()
                KeyAction.SPACE -> type(' ')
                KeyAction.RESIZE -> target.onResize()
                null -> spec.char?.let(::type)
            }
        }
        press.mode = PressMode.COMMITTED
        state.pressed.remove(press.keyIndex)
        if (state.bubbleKey == press.keyIndex) state.bubbleKey = null
    }

    private fun type(c: Char) {
        if (!callbacks.actions().onChar(c)) callbacks.haptic(HapticFeedbackConstants.REJECT)
    }
}
