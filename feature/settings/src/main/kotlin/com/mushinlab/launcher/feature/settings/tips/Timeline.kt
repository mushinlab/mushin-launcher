package com.mushinlab.launcher.feature.settings.tips

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import kotlin.math.max

internal fun phase(time: Float, start: Int, end: Int): Float = ((time - start) / (end - start)).coerceIn(0f, 1f)

internal fun ease(time: Float, start: Int, end: Int): Float = FastOutSlowInEasing.transform(phase(time, start, end))

internal fun pulse(time: Float, vararg taps: Int): Float = taps.fold(0f) { level, tap ->
    max(level, tapLevel(time - tap))
}

internal fun hold(time: Float, start: Int, end: Int): Float =
    minOf(phase(time, start, start + PRESS_IN_MS), 1f - phase(time, end, end + PRESS_OUT_MS))

internal fun loopFade(time: Float, loopMs: Int): Float =
    minOf(phase(time, 0, LOOP_FADE_MS), 1f - phase(time, loopMs - LOOP_FADE_MS, loopMs))

internal fun mix(from: Float, to: Float, fraction: Float): Float = from + (to - from) * fraction

private fun tapLevel(since: Float): Float = when {
    since < 0f -> 0f
    since < PRESS_IN_MS -> since / PRESS_IN_MS
    since < PRESS_IN_MS + PRESS_HOLD_MS -> 1f
    else -> 1f - phase(since, PRESS_IN_MS + PRESS_HOLD_MS, PRESS_IN_MS + PRESS_HOLD_MS + PRESS_OUT_MS)
}

@Immutable
internal data class Key(val ms: Int, val at: Offset)

@Immutable
internal class Track(private val keys: List<Key>) {
    fun at(time: Float): Offset {
        val next = keys.indexOfFirst { it.ms > time }
        return when (next) {
            -1 -> keys.last().at

            0 -> keys.first().at

            else -> {
                val from = keys[next - 1]
                val to = keys[next]
                lerp(from.at, to.at, ease(time, from.ms, to.ms))
            }
        }
    }
}

@Immutable
internal class Polyline(private val points: List<Offset>) {
    private val lengths: List<Float> = points.zipWithNext { a, b -> (b - a).getDistance() }
    private val total = lengths.sum()

    val first: Offset get() = points.first()
    val last: Offset get() = points.last()

    fun pointAt(fraction: Float): Offset {
        var left = fraction.coerceIn(0f, 1f) * total
        lengths.forEachIndexed { index, length ->
            if (left <= length && length > 0f) return lerp(points[index], points[index + 1], left / length)
            left -= length
        }
        return points.last()
    }

    fun reached(fraction: Float): List<Offset> {
        val limit = fraction.coerceIn(0f, 1f) * total
        var walked = 0f
        val result = mutableListOf(points.first())
        lengths.forEachIndexed { index, length ->
            if (walked + length <= limit) result += points[index + 1]
            walked += length
        }
        if (fraction > 0f && fraction < 1f) result += pointAt(fraction)
        return result
    }

    fun moved(by: Offset, scale: Float = 1f): Polyline {
        val center = Offset(points.map { it.x }.average().toFloat(), points.map { it.y }.average().toFloat())
        return Polyline(points.map { by + (it - center) * scale })
    }
}

private const val PRESS_IN_MS = 80
private const val PRESS_HOLD_MS = 120
private const val PRESS_OUT_MS = 140
private const val LOOP_FADE_MS = 300
