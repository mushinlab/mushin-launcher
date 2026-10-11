package com.bluecubelabs.env.feature.search

import androidx.compose.runtime.Immutable
import com.bluecubelabs.env.core.model.KeyboardFit

@Immutable
data class KeyboardFrame(val left: Float, val width: Float) {
    val right: Float get() = left + width

    fun dragStart(dx: Float, total: Float): KeyboardFrame {
        val start = (left + dx).coerceIn(0f, right - total * KeyboardFit.WIDTH_MIN)
        return KeyboardFrame(start, right - start)
    }

    fun dragEnd(dx: Float, total: Float): KeyboardFrame {
        val end = (right + dx).coerceIn(left + total * KeyboardFit.WIDTH_MIN, total)
        return KeyboardFrame(left, end - left)
    }

    fun move(dx: Float, total: Float): KeyboardFrame = copy(left = (left + dx).coerceIn(0f, total - width))

    fun fraction(total: Float): Float = (width / total).coerceIn(KeyboardFit.WIDTH_MIN, KeyboardFit.WIDTH_MAX)

    fun shift(total: Float): Float {
        val free = total - width
        return if (free < 1f) 0f else (2 * left / free - 1).coerceIn(KeyboardFit.SHIFT_MIN, KeyboardFit.SHIFT_MAX)
    }

    companion object {
        fun of(fraction: Float, shift: Float, total: Float): KeyboardFrame {
            val width = total * fraction
            return KeyboardFrame((total - width) * (1 + shift) / 2, width)
        }
    }
}
