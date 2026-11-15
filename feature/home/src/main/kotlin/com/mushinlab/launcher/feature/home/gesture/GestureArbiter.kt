package com.mushinlab.launcher.feature.home.gesture

import com.mushinlab.launcher.core.model.SwipeDirection
import kotlin.math.abs

data class GestureConfig(
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
    fun swipeDirection(dx: Float, dy: Float, velocityX: Float, velocityY: Float): SwipeDirection? {
        val ax = abs(dx)
        val ay = abs(dy)
        val horizontal = ax >= config.axisRatio * ay && ax > 0f
        val vertical = ay >= config.axisRatio * ax && ay > 0f
        val direction = when {
            horizontal -> if (dx > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
            vertical -> if (dy < 0) SwipeDirection.UP else SwipeDirection.DOWN
            else -> null
        }
        val (distance, velocity) = if (horizontal) ax to abs(velocityX) else ay to abs(velocityY)
        val longEnough = distance >= config.swipeDistancePx
        val flung = distance >= config.flingDistancePx && velocity >= config.flingVelocityPxPerSec
        return direction?.takeIf { longEnough || flung }
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
}
