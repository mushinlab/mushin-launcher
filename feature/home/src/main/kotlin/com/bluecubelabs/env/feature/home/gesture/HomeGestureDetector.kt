package com.bluecubelabs.env.feature.home.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import com.bluecubelabs.env.core.model.SwipeDirection

interface HomeGestureCallbacks {
    fun onRowTap(index: Int)

    fun onLongPress(position: Offset, rowIndex: Int?)

    fun onDoubleTap()

    fun onSwipe(direction: SwipeDirection)
}

private sealed interface PressOutcome {
    data class Up(val position: Offset, val timeMs: Long) : PressOutcome

    data object Slop : PressOutcome

    data object Cancelled : PressOutcome
}

suspend fun PointerInputScope.detectHomeGestures(
    arbiter: GestureArbiter,
    registry: RowRegistry,
    zone: () -> StartZone,
    enabled: () -> Boolean,
    callbacks: HomeGestureCallbacks,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (!enabled() || registry.isBlocked(down.position)) return@awaitEachGesture
        val row = registry.hit(down.position)
        if (arbiter.isDoubleTap(down.position.x, down.position.y, down.uptimeMillis, row != null)) {
            callbacks.onDoubleTap()
            consumeUntilUp(down.id)
            return@awaitEachGesture
        }
        val tracker = VelocityTracker()
        tracker.addPointerInputChange(down)
        val outcome = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            awaitSlopOrUp(down.id, down.position, tracker)
        }
        when (outcome) {
            null -> {
                arbiter.reset()
                callbacks.onLongPress(down.position, row)
                consumeUntilUp(down.id)
            }

            is PressOutcome.Up -> {
                arbiter.onTapUp(outcome.position.x, outcome.position.y, outcome.timeMs, row != null)
                if (row != null) callbacks.onRowTap(row)
            }

            PressOutcome.Slop -> {
                arbiter.reset()
                val end = trackUntilUp(down.id, tracker) ?: return@awaitEachGesture
                val velocity = tracker.calculateVelocity()
                val delta = end - down.position
                val direction = arbiter.swipeDirection(delta.x, delta.y, velocity.x, velocity.y)
                if (direction != null && arbiter.isAllowedStart(direction, down.position.x, down.position.y, zone())) {
                    callbacks.onSwipe(direction)
                }
            }

            PressOutcome.Cancelled -> arbiter.reset()
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitSlopOrUp(
    id: PointerId,
    start: Offset,
    tracker: VelocityTracker,
): PressOutcome {
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.count { it.pressed } > 1) return PressOutcome.Cancelled
        val change = event.changes.firstOrNull { it.id == id } ?: return PressOutcome.Cancelled
        tracker.addPointerInputChange(change)
        if (!change.pressed) {
            return PressOutcome.Up(change.position, change.uptimeMillis)
        }
        if ((change.position - start).getDistance() > viewConfiguration.touchSlop) return PressOutcome.Slop
    }
}

private suspend fun AwaitPointerEventScope.trackUntilUp(id: PointerId, tracker: VelocityTracker): Offset? {
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.count { it.pressed } > 1) return null
        val change = event.changes.firstOrNull { it.id == id } ?: return null
        tracker.addPointerInputChange(change)
        if (!change.pressed) return change.position
    }
}

private suspend fun AwaitPointerEventScope.consumeUntilUp(id: PointerId) {
    while (true) {
        val event = awaitPointerEvent()
        event.changes.forEach { it.consume() }
        val change = event.changes.firstOrNull { it.id == id } ?: return
        if (!change.pressed) return
    }
}
