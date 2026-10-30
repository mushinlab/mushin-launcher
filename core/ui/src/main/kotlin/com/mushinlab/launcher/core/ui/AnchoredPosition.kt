package com.mushinlab.launcher.core.ui

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

/** Pure placement math for [AnchoredPanel], in pixels. */
object AnchoredPosition {
    /** The window minus system [insets] and a [marginPx] margin on every side. */
    fun safeArea(window: IntSize, insets: IntRect, marginPx: Int): IntRect = IntRect(
        left = insets.left + marginPx,
        top = insets.top + marginPx,
        right = window.width - insets.right - marginPx,
        bottom = window.height - insets.bottom - marginPx,
    )

    /**
     * Top-left corner of a [popup] centered horizontally on the anchor: below it with [gapPx] when it fits,
     * otherwise above it, otherwise pinned to the bottom of [safe]. Always clamped into [safe] where possible.
     */
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

    /**
     * Top-left corner of a [popup] centered horizontally on the anchor whose point [offsetPx] below its top edge
     * lands on the anchor, clamped into [safe] where possible.
     */
    fun atPoint(anchorX: Int, anchorY: Int, offsetPx: Int, popup: IntSize, safe: IntRect): IntOffset {
        val maxX = (safe.right - popup.width).coerceAtLeast(safe.left)
        val maxY = (safe.bottom - popup.height).coerceAtLeast(safe.top)
        return IntOffset(
            (anchorX - popup.width / 2).coerceIn(safe.left, maxX),
            (anchorY - offsetPx).coerceIn(safe.top, maxY),
        )
    }
}
