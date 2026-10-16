package com.mushinlab.launcher.feature.home

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.model.ClockPosition
import com.mushinlab.launcher.core.model.HomeListOffset
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.feature.home.gesture.DragCallbacks
import com.mushinlab.launcher.feature.home.gesture.GestureArbiter
import com.mushinlab.launcher.feature.home.gesture.GestureConfig
import com.mushinlab.launcher.feature.home.gesture.HomeGestureCallbacks

internal class ClockMover(
    position: ClockPosition,
    private val inset: Int,
    private val view: View,
    private val onMoved: (ClockPosition) -> Unit,
) : DragCallbacks {
    var drag by mutableStateOf<ClockDrag?>(null)
        private set
    var shown by mutableStateOf(position)
        private set
    private val metrics = IntArray(2)

    fun measured(area: Int, block: Int) {
        metrics[0] = area
        metrics[1] = block
    }

    fun sync(position: ClockPosition) {
        if (drag == null) shown = position
    }

    override fun onHold(position: Float) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        drag = ClockDrag(position, position)
    }

    override fun onDrag(position: Float) {
        drag = drag?.copy(x = position)
    }

    override fun onRelease() {
        val current = drag ?: return
        val (area, block) = metrics.let { it[0] to it[1] }
        val from = ClockSlots.left(shown, area, block, inset)
        val left = (from + current.x - current.startX).toInt().coerceIn(0, maxOf(0, area - block))
        val target = ClockSlots.nearest(left, area, block, inset)
        drag = null
        if (target != shown) {
            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
            shown = target
            onMoved(target)
        }
    }
}

internal class ListHandleDrag(
    private val maxOffsetPx: Float,
    private val view: View,
    private val onReleased: (Float) -> Unit,
) : DragCallbacks {
    private var restingPx = 0f
    private var draggedPx by mutableStateOf<Float?>(null)
    private var last by mutableStateOf<Float?>(null)
    val active: Boolean get() = last != null

    fun offsetPx(restingPx: Float): Float {
        this.restingPx = restingPx
        return draggedPx ?: restingPx
    }

    fun settle() {
        if (last == null) draggedPx = null
    }

    override fun onHold(position: Float) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        draggedPx = draggedPx ?: restingPx
        last = position
    }

    override fun onDrag(position: Float) {
        val previous = last ?: return
        draggedPx = ((draggedPx ?: restingPx) + position - previous).coerceIn(0f, maxOffsetPx)
        last = position
    }

    override fun onRelease() {
        last = null
        draggedPx?.let(onReleased)
    }
}

internal class HomeGestureHandler(
    private val view: View,
    private val state: State<HomeUiState>,
    private val actions: State<HomeActions>,
    private val trace: Trace,
    override val clock: ClockMover,
    override val handle: ListHandleDrag,
) : HomeGestureCallbacks {
    var width by mutableFloatStateOf(0f)
    var pressedRow by mutableStateOf<Int?>(null)
        private set

    override fun onRowPressed(index: Int?) {
        pressedRow = index
    }

    override fun onRowTap(index: Int) {
        state.value.rows.getOrNull(index)?.let { actions.value.onRowTap(it, index) }
    }

    override fun onLongPress(position: Offset, rowIndex: Int?) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        actions.value.onLongPress(position, rowIndex?.let { state.value.rows.getOrNull(it) })
    }

    override fun onDoubleTap() {
        actions.value.onDoubleTap()
    }

    override fun onSwipe(direction: SwipeDirection, fromShadeZone: Boolean) {
        view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
        actions.value.onSwipe(direction, fromShadeZone)
    }

    override fun onClockTap() {
        actions.value.onClockTap()
    }

    override fun onTrace(position: Offset, timeMs: Long) {
        trace.add(position, timeMs)
    }

    override fun onDraw(points: List<Offset>) {
        actions.value.onDraw(points, width)
    }
}

@Composable
internal fun rememberHomeGestures(
    state: State<HomeUiState>,
    actions: State<HomeActions>,
    trace: Trace,
): HomeGestureHandler {
    val view = LocalView.current
    val density = LocalDensity.current
    val inset = with(density) { HomeDimens.rowPadding.roundToPx() }
    val maxOffsetPx = with(density) { HomeListOffset.MAX_DP.dp.toPx() }
    val home = state.value
    val gestures = remember(view) {
        HomeGestureHandler(
            view = view,
            state = state,
            actions = actions,
            trace = trace,
            clock = ClockMover(home.clockPosition, inset, view) { actions.value.onClockMoved(it) },
            handle = ListHandleDrag(maxOffsetPx, view) {
                actions.value.onListOffset(with(density) { it.toDp().value.toInt() })
            },
        )
    }
    LaunchedEffect(home.clockPosition) { gestures.clock.sync(home.clockPosition) }
    LaunchedEffect(home.homeListOffset) { gestures.handle.settle() }
    return gestures
}

@Composable
internal fun rememberGestureArbiter(): GestureArbiter {
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    return remember(density, viewConfiguration) {
        with(density) {
            GestureArbiter(
                GestureConfig(
                    doubleTapTimeoutMs = viewConfiguration.doubleTapTimeoutMillis,
                    doubleTapSlopPx = 100.dp.toPx(),
                    swipeDistancePx = 96.dp.toPx(),
                    flingDistancePx = 48.dp.toPx(),
                    flingVelocityPxPerSec = 1000.dp.toPx(),
                    drawMinPx = 144.dp.toPx(),
                ),
            )
        }
    }
}
