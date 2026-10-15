package com.mushinlab.launcher.feature.home.gesture

import com.mushinlab.launcher.core.model.SwipeDirection
import kotlin.math.abs

data class GestureConfig(
    val doubleTapTimeoutMs: Long,
    val doubleTapSlopPx: Float,
    val swipeDistancePx: Float,
    val flingDistancePx: Float,
    val flingVelocityPxPerSec: Float,
    val drawMinPx: Float = Float.MAX_VALUE,
    val axisRatio: Float = AXIS_RATIO,
) {
    companion object {
        const val AXIS_RATIO = 1.5f
        const val STRAIGHTNESS = 0.8f
    }
}

data class StartZone(
    val topLimitPx: Float,
    val shadeLimitPx: Float,
    val bottomLimitPx: Float,
    val leftLimitPx: Float,
    val rightLimitPx: Float,
)

class GestureArbiter(private val config: GestureConfig) {
    private var lastBackgroundTapTime: Long = NO_TAP
    private var lastBackgroundTapX = 0f
    private var lastBackgroundTapY = 0f

    fun isDoubleTap(x: Float, y: Float, timeMs: Long, onRow: Boolean): Boolean {
        if (onRow || lastBackgroundTapTime == NO_TAP) return false
        val dt = timeMs - lastBackgroundTapTime
        val dx = x - lastBackgroundTapX
        val dy = y - lastBackgroundTapY
        val near = dx * dx + dy * dy <= config.doubleTapSlopPx * config.doubleTapSlopPx
        val inTime = dt in 0..config.doubleTapTimeoutMs
        return if (near && inTime) {
            reset()
            true
        } else {
            false
        }
    }

    fun onTapUp(x: Float, y: Float, timeMs: Long, onRow: Boolean) {
        if (onRow) {
            reset()
        } else {
            lastBackgroundTapTime = timeMs
            lastBackgroundTapX = x
            lastBackgroundTapY = y
        }
    }

    fun reset() {
        lastBackgroundTapTime = NO_TAP
    }

    fun swipeDirection(dx: Float, dy: Float, velocityX: Float, velocityY: Float): SwipeDirection? {
        val ax = abs(dx)
        val ay = abs(dy)
        val horizontal = ax >= config.axisRatio * ay && ax > 0f
        val vertical = ay >= config.axisRatio * ax && ay > 0f
        val (distance, velocity) = when {
            horizontal -> ax to abs(velocityX)
            vertical -> ay to abs(velocityY)
            else -> return null
        }
        val longEnough = distance >= config.swipeDistancePx
        val flung = distance >= config.flingDistancePx && velocity >= config.flingVelocityPxPerSec
        if (!longEnough && !flung) return null
        return when {
            horizontal && dx > 0 -> SwipeDirection.RIGHT
            horizontal -> SwipeDirection.LEFT
            dy < 0 -> SwipeDirection.UP
            else -> SwipeDirection.DOWN
        }
    }

    fun isDrawing(pathLength: Float, chord: Float): Boolean =
        pathLength >= config.drawMinPx && chord < pathLength * GestureConfig.STRAIGHTNESS

    fun isAllowedStart(direction: SwipeDirection, startX: Float, startY: Float, zone: StartZone): Boolean =
        when (direction) {
            SwipeDirection.UP -> startY < zone.bottomLimitPx
            SwipeDirection.DOWN -> startY > zone.topLimitPx
            SwipeDirection.LEFT, SwipeDirection.RIGHT -> startX > zone.leftLimitPx && startX < zone.rightLimitPx
        }

    fun isShadeSwipe(direction: SwipeDirection, startY: Float, zone: StartZone): Boolean =
        direction == SwipeDirection.DOWN && startY < zone.shadeLimitPx

    private companion object {
        const val NO_TAP = Long.MIN_VALUE
    }
}
