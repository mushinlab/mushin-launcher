package com.bluecubelabs.env.feature.home

import com.bluecubelabs.env.core.model.SwipeDirection
import com.bluecubelabs.env.feature.home.gesture.GestureArbiter
import com.bluecubelabs.env.feature.home.gesture.GestureConfig
import com.bluecubelabs.env.feature.home.gesture.StartZone
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GestureArbiterTest {
    private val dp = 2.625f
    private val config = GestureConfig(
        doubleTapTimeoutMs = 300,
        doubleTapSlopPx = 100 * dp,
        swipeDistancePx = 96 * dp,
        flingDistancePx = 48 * dp,
        flingVelocityPxPerSec = 1000 * dp,
    )
    private val arbiter = GestureArbiter(config)
    private val zone =
        StartZone(topLimitPx = 63f, bottomLimitPx = 2400f - 63f - 16 * dp, leftLimitPx = 80f, rightLimitPx = 1000f)

    @Test
    fun AC_07_1_secondBackgroundDownWithinTimeoutAndDistanceIsDoubleTap() {
        arbiter.onTapUp(500f, 1500f, 1000, onRow = false)
        assertThat(arbiter.isDoubleTap(520f, 1510f, 1250, onRow = false)).isTrue()
    }

    @Test
    fun doubleTapResetsAfterRecognition() {
        arbiter.onTapUp(500f, 1500f, 1000, onRow = false)
        assertThat(arbiter.isDoubleTap(500f, 1500f, 1100, onRow = false)).isTrue()
        assertThat(arbiter.isDoubleTap(500f, 1500f, 1200, onRow = false)).isFalse()
    }

    @Test
    fun noDoubleTapWithoutPreviousTap() {
        assertThat(arbiter.isDoubleTap(500f, 1500f, 1000, onRow = false)).isFalse()
    }

    @Test
    fun noDoubleTapAfterTimeout() {
        arbiter.onTapUp(500f, 1500f, 1000, onRow = false)
        assertThat(arbiter.isDoubleTap(500f, 1500f, 1301, onRow = false)).isFalse()
    }

    @Test
    fun timeoutBoundaryIsInclusive() {
        arbiter.onTapUp(500f, 1500f, 1000, onRow = false)
        assertThat(arbiter.isDoubleTap(500f, 1500f, 1300, onRow = false)).isTrue()
    }

    @Test
    fun noDoubleTapWhenTooFar() {
        arbiter.onTapUp(100f, 1500f, 1000, onRow = false)
        assertThat(arbiter.isDoubleTap(100f + 101 * dp, 1500f, 1100, onRow = false)).isFalse()
    }

    @Test
    fun AC_07_4_noDoubleTapOnRows() {
        arbiter.onTapUp(500f, 1500f, 1000, onRow = false)
        assertThat(arbiter.isDoubleTap(500f, 1500f, 1100, onRow = true)).isFalse()
    }

    @Test
    fun rowTapClearsPendingBackgroundTap() {
        arbiter.onTapUp(500f, 1500f, 1000, onRow = false)
        arbiter.onTapUp(500f, 1500f, 1050, onRow = true)
        assertThat(arbiter.isDoubleTap(500f, 1500f, 1100, onRow = false)).isFalse()
    }

    @Test
    fun resetClearsPendingTap() {
        arbiter.onTapUp(500f, 1500f, 1000, onRow = false)
        arbiter.reset()
        assertThat(arbiter.isDoubleTap(500f, 1500f, 1100, onRow = false)).isFalse()
    }

    @Test
    fun timeGoingBackwardsIsNotDoubleTap() {
        arbiter.onTapUp(500f, 1500f, 1000, onRow = false)
        assertThat(arbiter.isDoubleTap(500f, 1500f, 900, onRow = false)).isFalse()
    }

    @Test
    fun C_4_longSwipesInFourDirections() {
        val d = 100 * dp
        assertThat(arbiter.swipeDirection(0f, -d, 0f, 0f)).isEqualTo(SwipeDirection.UP)
        assertThat(arbiter.swipeDirection(0f, d, 0f, 0f)).isEqualTo(SwipeDirection.DOWN)
        assertThat(arbiter.swipeDirection(-d, 0f, 0f, 0f)).isEqualTo(SwipeDirection.LEFT)
        assertThat(arbiter.swipeDirection(d, 0f, 0f, 0f)).isEqualTo(SwipeDirection.RIGHT)
    }

    @Test
    fun C_4_shortSlowSwipeIsIgnored() {
        assertThat(arbiter.swipeDirection(0f, -90 * dp, 0f, -500 * dp)).isNull()
    }

    @Test
    fun C_4_flingNeedsHalfDistanceAndVelocity() {
        assertThat(arbiter.swipeDirection(0f, -50 * dp, 0f, -1200 * dp)).isEqualTo(SwipeDirection.UP)
        assertThat(arbiter.swipeDirection(0f, -40 * dp, 0f, -3000 * dp)).isNull()
        assertThat(arbiter.swipeDirection(60 * dp, 0f, 900 * dp, 0f)).isNull()
        assertThat(arbiter.swipeDirection(60 * dp, 0f, 1100 * dp, 0f)).isEqualTo(SwipeDirection.RIGHT)
    }

    @Test
    fun C_4_diagonalSwipeIsIgnored() {
        assertThat(arbiter.swipeDirection(100 * dp, -100 * dp, 0f, 0f)).isNull()
        assertThat(arbiter.swipeDirection(100 * dp, -80 * dp, 0f, 0f)).isNull()
    }

    @Test
    fun C_4_axisRatioBoundary() {
        assertThat(arbiter.swipeDirection(150 * dp, 100 * dp, 0f, 0f)).isEqualTo(SwipeDirection.RIGHT)
        assertThat(arbiter.swipeDirection(100 * dp, 150 * dp, 0f, 0f)).isEqualTo(SwipeDirection.DOWN)
        assertThat(arbiter.swipeDirection(0f, 0f, 0f, 0f)).isNull()
    }

    @Test
    fun C_4_startZones() {
        assertThat(arbiter.isAllowedStart(SwipeDirection.UP, 500f, 2000f, zone)).isTrue()
        assertThat(arbiter.isAllowedStart(SwipeDirection.UP, 500f, 2350f, zone)).isFalse()
        assertThat(arbiter.isAllowedStart(SwipeDirection.DOWN, 500f, 100f, zone)).isTrue()
        assertThat(arbiter.isAllowedStart(SwipeDirection.DOWN, 500f, 30f, zone)).isFalse()
        assertThat(arbiter.isAllowedStart(SwipeDirection.LEFT, 500f, 1000f, zone)).isTrue()
        assertThat(arbiter.isAllowedStart(SwipeDirection.LEFT, 40f, 1000f, zone)).isFalse()
        assertThat(arbiter.isAllowedStart(SwipeDirection.RIGHT, 1050f, 1000f, zone)).isFalse()
    }
}
