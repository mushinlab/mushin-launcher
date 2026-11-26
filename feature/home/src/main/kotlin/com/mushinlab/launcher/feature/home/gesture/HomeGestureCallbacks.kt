package com.mushinlab.launcher.feature.home.gesture

import androidx.compose.ui.geometry.Offset
import com.mushinlab.launcher.core.model.SwipeDirection

interface HomeGestureCallbacks {
    val clock: DragCallbacks

    val handle: DragCallbacks

    fun onPressed(rowIndex: Int?, clock: Boolean)

    fun onRowTap(index: Int, position: Offset)

    fun onLongPress(position: Offset, rowIndex: Int?)

    fun onSwipe(direction: SwipeDirection, fromShadeZone: Boolean)

    fun onBarDrag(dx: Float)

    fun onBarRelease(direction: SwipeDirection?)

    fun onClockTap()

    fun onBarTap(anchor: Offset)

    fun onTrace(position: Offset, timeMs: Long)

    fun onDraw(points: List<Offset>)
}

interface DragCallbacks {
    fun onHold(position: Float)

    fun onDrag(position: Float)

    fun onRelease()
}
