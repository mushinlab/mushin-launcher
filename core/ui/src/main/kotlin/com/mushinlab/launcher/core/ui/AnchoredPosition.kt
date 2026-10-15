package com.mushinlab.launcher.core.ui

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

object AnchoredPosition {
    fun safeArea(window: IntSize, insets: IntRect, marginPx: Int): IntRect = IntRect(
        left = insets.left + marginPx,
        top = insets.top + marginPx,
        right = window.width - insets.right - marginPx,
        bottom = window.height - insets.bottom - marginPx,
    )

    fun belowPoint(anchorX: Int, anchorY: Int, popup: IntSize, safe: IntRect, gapPx: Int): IntOffset {
        val maxX = (safe.right - popup.width).coerceAtLeast(safe.left)
        val x = (anchorX - popup.width / 2).coerceIn(safe.left, maxX)
        val below = (anchorY + gapPx).coerceAtLeast(safe.top)
        val above = anchorY - gapPx - popup.height
        val maxY = (safe.bottom - popup.height).coerceAtLeast(safe.top)
        val y = when {
            below + popup.height <= safe.bottom -> below
            above >= safe.top -> above.coerceAtMost(maxY)
            else -> maxY
        }
        return IntOffset(x, y)
    }
}
