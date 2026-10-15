package com.mushinlab.launcher.feature.home.gesture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

class RowRegistry {
    private val rows = HashMap<Int, Rect>()
    private val blocked = HashMap<String, Rect>()
    val clock = HitArea()
    val handle = HitArea()

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

    fun hit(position: Offset): Int? = rows.entries.firstOrNull { it.value.contains(position) }?.key

    fun boundsOf(index: Int): Rect? = rows[index]

    fun isBlocked(position: Offset): Boolean = blocked.values.any { it.contains(position) }
}

class HitArea {
    private var bounds: Rect? = null

    fun set(bounds: Rect?) {
        this.bounds = bounds
    }

    fun contains(position: Offset): Boolean = bounds?.contains(position) == true
}
