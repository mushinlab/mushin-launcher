package com.mushinlab.launcher.feature.home.gesture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

class RowRegistry {
    private val rows = HashMap<Int, Rect>()
    private val blocked = HashMap<String, Rect>()
    private var clock: Rect? = null
    private var handle: Rect? = null

    fun update(index: Int, bounds: Rect) {
        rows[index] = bounds
    }

    fun remove(index: Int) {
        rows.remove(index)
    }

    fun retainBelow(count: Int) {
        rows.keys.removeAll { it >= count }
    }

    fun block(id: String, bounds: Rect?) {
        if (bounds == null) blocked.remove(id) else blocked[id] = bounds
    }

    fun clock(bounds: Rect?) {
        clock = bounds
    }

    fun isClock(position: Offset): Boolean = clock?.contains(position) == true

    fun handle(bounds: Rect?) {
        handle = bounds
    }

    fun isHandle(position: Offset): Boolean = handle?.contains(position) == true

    fun hit(position: Offset): Int? = rows.entries.firstOrNull { it.value.contains(position) }?.key

    fun boundsOf(index: Int): Rect? = rows[index]

    fun isBlocked(position: Offset): Boolean = blocked.values.any { it.contains(position) }
}
