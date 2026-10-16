package com.mushinlab.launcher.feature.home.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.Velocity

class HomeGestureDetector(
    private val arbiter: GestureArbiter,
    private val registry: RowRegistry,
    private val zone: () -> StartZone,
    private val enabled: () -> Boolean,
    private val clockHoldMs: Long,
) {
    suspend fun detect(scope: PointerInputScope, callbacks: HomeGestureCallbacks) {
        scope.awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (enabled() && !registry.isBlocked(down.position)) onDown(down, callbacks)
        }
    }

    private suspend fun AwaitPointerEventScope.onDown(down: PointerInputChange, callbacks: HomeGestureCallbacks) {
        val target = targetAt(down.position)
        val doubleTap = target != PressTarget.Clock &&
            arbiter.isDoubleTap(down.position.x, down.position.y, down.uptimeMillis, target is PressTarget.Row)
        if (doubleTap) {
            callbacks.onDoubleTap()
            consumeUntilUp(down.id)
            return
        }
        val tracker = VelocityTracker()
        tracker.addPointerInputChange(down)
        val holdMs = if (target == PressTarget.Clock) clockHoldMs else viewConfiguration.longPressTimeoutMillis
        callbacks.onRowPressed(target.rowIndex)
        val outcome = try {
            withTimeoutOrNull(holdMs) { awaitSlopOrUp(down.id, down.position, tracker) }
        } finally {
            callbacks.onRowPressed(null)
        }
        when (outcome) {
            null -> onHold(down, target, callbacks)
            is PressOutcome.Up -> onTap(outcome, target, callbacks)
            PressOutcome.Slop -> onMove(down, tracker, callbacks)
            PressOutcome.Cancelled -> arbiter.reset()
        }
    }

    private fun targetAt(position: Offset): PressTarget {
        val row = registry.hit(position)
        return when {
            row != null -> PressTarget.Row(row)
            registry.clock.contains(position) -> PressTarget.Clock
            registry.handle.contains(position) -> PressTarget.Handle
            else -> PressTarget.Background
        }
    }

    private suspend fun AwaitPointerEventScope.onHold(
        down: PointerInputChange,
        target: PressTarget,
        callbacks: HomeGestureCallbacks,
    ) {
        arbiter.reset()
        when (target) {
            PressTarget.Clock -> holdAndDrag(down.id, down.position.x, callbacks.clock) { it.x }

            PressTarget.Handle -> holdAndDrag(down.id, down.position.y, callbacks.handle) { it.y }

            else -> {
                callbacks.onLongPress(down.position, target.rowIndex)
                consumeUntilUp(down.id)
            }
        }
    }

    private fun onTap(up: PressOutcome.Up, target: PressTarget, callbacks: HomeGestureCallbacks) {
        if (target == PressTarget.Clock) {
            arbiter.reset()
            callbacks.onClockTap()
        } else {
            arbiter.onTapUp(up.position.x, up.position.y, up.timeMs, target is PressTarget.Row)
            target.rowIndex?.let(callbacks::onRowTap)
        }
    }

    private suspend fun AwaitPointerEventScope.onMove(
        down: PointerInputChange,
        tracker: VelocityTracker,
        callbacks: HomeGestureCallbacks,
    ) {
        arbiter.reset()
        callbacks.onTrace(down.position, down.uptimeMillis)
        val points = mutableListOf(down.position)
        val end = trackUntilUp(down.id, tracker) { position, time ->
            points += position
            callbacks.onTrace(position, time)
        } ?: return
        val velocity = tracker.calculateVelocity()
        val delta = end - down.position
        if (arbiter.isDrawing(pathLength(points), delta.getDistance())) {
            callbacks.onDraw(points)
        } else {
            onSwipe(down.position, delta, velocity, callbacks)
        }
    }

    private fun onSwipe(start: Offset, delta: Offset, velocity: Velocity, callbacks: HomeGestureCallbacks) {
        val direction = arbiter.swipeDirection(delta.x, delta.y, velocity.x, velocity.y)
        val startZone = zone()
        if (direction != null && arbiter.isAllowedStart(direction, start.x, start.y, startZone)) {
            callbacks.onSwipe(direction, arbiter.isShadeSwipe(direction, start.y, startZone))
        }
    }
}

private sealed interface PressTarget {
    data class Row(val index: Int) : PressTarget

    data object Clock : PressTarget

    data object Handle : PressTarget

    data object Background : PressTarget
}

private val PressTarget.rowIndex: Int?
    get() = (this as? PressTarget.Row)?.index

private sealed interface PressOutcome {
    data class Up(val position: Offset, val timeMs: Long) : PressOutcome

    data object Slop : PressOutcome

    data object Cancelled : PressOutcome
}

private suspend fun AwaitPointerEventScope.awaitSlopOrUp(
    id: PointerId,
    start: Offset,
    tracker: VelocityTracker,
): PressOutcome {
    while (true) {
        outcomeOf(awaitSingle(id), start, tracker)?.let { return it }
    }
}

private fun AwaitPointerEventScope.outcomeOf(
    change: PointerInputChange?,
    start: Offset,
    tracker: VelocityTracker,
): PressOutcome? {
    if (change == null) return PressOutcome.Cancelled
    tracker.addPointerInputChange(change)
    return when {
        !change.pressed -> PressOutcome.Up(change.position, change.uptimeMillis)
        (change.position - start).getDistance() > viewConfiguration.touchSlop -> PressOutcome.Slop
        else -> null
    }
}

private suspend fun AwaitPointerEventScope.trackUntilUp(
    id: PointerId,
    tracker: VelocityTracker,
    onMove: (Offset, Long) -> Unit,
): Offset? {
    while (true) {
        val change = awaitSingle(id) ?: return null
        tracker.addPointerInputChange(change)
        if (!change.pressed) return change.position
        onMove(change.position, change.uptimeMillis)
    }
}

private suspend fun AwaitPointerEventScope.awaitSingle(id: PointerId): PointerInputChange? {
    val event = awaitPointerEvent()
    return if (event.changes.count { it.pressed } > 1) null else event.changes.firstOrNull { it.id == id }
}

private fun pathLength(points: List<Offset>): Float =
    points.zipWithNext { a, b -> (b - a).getDistance() }.sum()

private suspend fun AwaitPointerEventScope.holdAndDrag(
    id: PointerId,
    start: Float,
    callbacks: DragCallbacks,
    axis: (Offset) -> Float,
) {
    callbacks.onHold(start)
    dragUntilUp(id) { callbacks.onDrag(axis(it)) }
    callbacks.onRelease()
}

private suspend fun AwaitPointerEventScope.dragUntilUp(id: PointerId, onMove: (Offset) -> Unit) {
    while (true) {
        val event = awaitPointerEvent()
        event.changes.forEach { it.consume() }
        val change = event.changes.firstOrNull { it.id == id } ?: return
        if (!change.pressed) return
        onMove(change.position)
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
