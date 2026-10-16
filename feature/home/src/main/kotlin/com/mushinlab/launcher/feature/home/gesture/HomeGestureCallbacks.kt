package com.mushinlab.launcher.feature.home.gesture

import androidx.compose.ui.geometry.Offset
import com.mushinlab.launcher.core.model.SwipeDirection

interface HomeGestureCallbacks {
    val clock: DragCallbacks

    val handle: DragCallbacks

    fun onRowPressed(index: Int?)

    fun onRowTap(index: Int)

    fun onLongPress(position: Offset, rowIndex: Int?)

    fun onDoubleTap()

    fun onSwipe(direction: SwipeDirection, fromShadeZone: Boolean)

    fun onClockTap()

    fun onTrace(position: Offset, timeMs: Long)

    fun onDraw(points: List<Offset>)
}

interface DragCallbacks {
    fun onHold(position: Float)

    fun onDrag(position: Float)

    fun onRelease()
}
