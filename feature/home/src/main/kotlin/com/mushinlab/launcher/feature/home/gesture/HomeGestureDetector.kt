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
import com.mushinlab.launcher.core.model.SwipeDirection
import kotlin.math.abs

class HomeGestureDetector(
    private val arbiter: GestureArbiter,
    private val registry: RowRegistry,
    private val zone: () -> StartZone,
    private val enabled: () -> Boolean,
    private val editing: () -> Boolean,
) {
    suspend fun detect(scope: PointerInputScope, callbacks: HomeGestureCallbacks) {
        scope.awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            when {
                !enabled() || registry.isBlocked(down.position) -> Unit
                editing() -> onEditDown(down)
                else -> onDown(down, callbacks)
            }
        }
    }

    private suspend fun AwaitPointerEventScope.onEditDown(down: PointerInputChange) {
        if (registry.hit(down.position) == null) consumeUntilUp(down.id)
    }

    private suspend fun AwaitPointerEventScope.onDown(down: PointerInputChange, callbacks: HomeGestureCallbacks) {
        val target = targetAt(down.position)
        val tracker = VelocityTracker()
        tracker.addPointerInputChange(down)
        callbacks.onPressed(target.rowIndex, target == PressTarget.Clock)
        val outcome = try {
            if (target == PressTarget.Background || target == PressTarget.Bar) {
                awaitSlopOrUp(down.id, down.position, tracker)
            } else {
                withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    awaitSlopOrUp(down.id, down.position, tracker)
                }
            }
        } finally {
            callbacks.onPressed(null, clock = false)
        }
        when (outcome) {
            null -> onHold(down, target, callbacks)

            is PressOutcome.Up -> onTap(outcome, target, callbacks)

            is PressOutcome.Slop -> if (target == PressTarget.Bar && outcome.horizontalFrom(down.position)) {
                onBarDrag(down, outcome.position, tracker, callbacks)
            } else {
                onMove(down, target, tracker, callbacks)
            }

            PressOutcome.Cancelled -> Unit
        }
    }

    private fun targetAt(position: Offset): PressTarget {
        val row = registry.hit(position)
        return when {
            row != null -> PressTarget.Row(row)
            registry.clock.contains(position) -> PressTarget.Clock
            registry.bar.contains(position) -> PressTarget.Bar
            else -> PressTarget.Background
        }
    }

    private suspend fun AwaitPointerEventScope.onHold(
        down: PointerInputChange,
        target: PressTarget,
        callbacks: HomeGestureCallbacks,
    ) {
        callbacks.onLongPress(down.position, target.rowIndex)
        consumeUntilUp(down.id)
    }

    private fun onTap(up: PressOutcome.Up, target: PressTarget, callbacks: HomeGestureCallbacks) {
        when (target) {
            PressTarget.Clock -> callbacks.onClockTap()
            PressTarget.Bar -> callbacks.onBarTap(registry.bar.topCenter() ?: up.position)
            is PressTarget.Row -> callbacks.onRowTap(target.index, up.position)
            PressTarget.Background -> Unit
        }
    }

    private suspend fun AwaitPointerEventScope.onMove(
        down: PointerInputChange,
        target: PressTarget,
        tracker: VelocityTracker,
        callbacks: HomeGestureCallbacks,
    ) {
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
            onSwipe(down.position, target, delta, velocity, callbacks)
        }
    }

    private suspend fun AwaitPointerEventScope.onBarDrag(
        down: PointerInputChange,
        slop: Offset,
        tracker: VelocityTracker,
        callbacks: HomeGestureCallbacks,
    ) {
        callbacks.onBarDrag(slop.x - down.position.x)
        var last = slop
        while (true) {
            val change = awaitSingle(down.id)
            if (change == null) {
                callbacks.onBarRelease(null)
                return
            }
            tracker.addPointerInputChange(change)
            change.consume()
            last = change.position
            if (!change.pressed) break
            callbacks.onBarDrag(last.x - down.position.x)
        }
        val velocity = tracker.calculateVelocity()
        val delta = last - down.position
        val direction = arbiter.swipeDirection(delta.x, delta.y, velocity.x, velocity.y)
        callbacks.onBarRelease(direction?.takeIf { it == SwipeDirection.LEFT || it == SwipeDirection.RIGHT })
    }

    private fun onSwipe(
        start: Offset,
        target: PressTarget,
        delta: Offset,
        velocity: Velocity,
        callbacks: HomeGestureCallbacks,
    ) {
        val direction = arbiter.swipeDirection(delta.x, delta.y, velocity.x, velocity.y)
        if (target == PressTarget.Bar && (direction == SwipeDirection.LEFT || direction == SwipeDirection.RIGHT)) return
        val startZone = zone()
        if (direction != null && arbiter.isAllowedStart(direction, start.x, start.y, startZone)) {
            callbacks.onSwipe(direction, arbiter.isShadeSwipe(direction, start.y, startZone))
        }
    }
}

private sealed interface PressTarget {
    data class Row(val index: Int) : PressTarget

    data object Clock : PressTarget

    data object Bar : PressTarget

    data object Background : PressTarget
}

private val PressTarget.rowIndex: Int?
    get() = (this as? PressTarget.Row)?.index

private sealed interface PressOutcome {
    data class Up(val position: Offset) : PressOutcome

    data class Slop(val position: Offset) : PressOutcome {
        fun horizontalFrom(start: Offset): Boolean = abs(position.x - start.x) > abs(position.y - start.y)
    }

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
    if (change == null || change.isConsumed) return PressOutcome.Cancelled
    tracker.addPointerInputChange(change)
    return when {
        !change.pressed -> PressOutcome.Up(change.position)
        (change.position - start).getDistance() > viewConfiguration.touchSlop -> PressOutcome.Slop(change.position)
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

private suspend fun AwaitPointerEventScope.consumeUntilUp(id: PointerId) {
    while (true) {
        val event = awaitPointerEvent()
        event.changes.forEach { it.consume() }
        val change = event.changes.firstOrNull { it.id == id } ?: return
        if (!change.pressed) return
    }
}
