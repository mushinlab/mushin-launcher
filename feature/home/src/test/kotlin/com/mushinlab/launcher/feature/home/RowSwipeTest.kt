package com.mushinlab.launcher.feature.home

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppKey
import org.junit.Test

class RowSwipeTest {
    private val a = AppKey("a", "A", 0)
    private val b = AppKey("b", "B", 0)
    private val reveal = 72f
    private val limit = 120f

    @Test
    fun AC_37_11_theFurtherTheRowIsPulledTheLessItMoves() {
        val first = SwipeResistance.offset(40f, limit)
        val second = SwipeResistance.offset(80f, limit) - first
        val third = SwipeResistance.offset(120f, limit) - SwipeResistance.offset(80f, limit)
        assertThat(first).isLessThan(40f)
        assertThat(second).isLessThan(first)
        assertThat(third).isLessThan(second)
        assertThat(SwipeResistance.offset(10_000f, limit)).isAtMost(limit)
        assertThat(SwipeResistance.offset(-5f, limit)).isEqualTo(0f)
    }

    @Test
    fun pullAndOffsetAreInverse() {
        val pull = SwipeResistance.pull(reveal, limit)
        assertThat(SwipeResistance.offset(pull, limit)).isWithin(0.01f).of(reveal)
        assertThat(SwipeResistance.pull(0f, limit)).isEqualTo(0f)
    }

    @Test
    fun AC_37_11_crossingTheThresholdIsReportedOnceAndTheRowStaysOpenAfterRelease() {
        val swipe = RowSwipe()
        swipe.begin(a, limit)
        assertThat(swipe.drag(-60f, reveal, limit)).isFalse()
        assertThat(swipe.offsetOf(a)).isLessThan(reveal)
        assertThat(swipe.drag(-80f, reveal, limit)).isTrue()
        assertThat(swipe.drag(-20f, reveal, limit)).isFalse()
        assertThat(swipe.isOpen(a)).isFalse()
        assertThat(swipe.release(reveal)).isEqualTo(reveal)
        swipe.offset = reveal
        swipe.settled(reveal)
        assertThat(swipe.isOpen(a)).isTrue()
        assertThat(swipe.offsetOf(b)).isEqualTo(0f)
    }

    @Test
    fun AC_37_11_aShortPullClosesTheRowAgain() {
        val swipe = RowSwipe()
        swipe.begin(a, limit)
        swipe.drag(-60f, reveal, limit)
        assertThat(swipe.release(reveal)).isEqualTo(0f)
        swipe.offset = 0f
        swipe.settled(reveal)
        assertThat(swipe.isOpen(a)).isFalse()
        assertThat(swipe.key).isNull()
    }

    @Test
    fun anOpenRowClosesWhenPulledBackPastHalfOrWhenAnotherRowIsPulled() {
        val swipe = RowSwipe()
        open(swipe, a)
        swipe.begin(a, limit)
        swipe.drag(20f, reveal, limit)
        assertThat(swipe.release(reveal)).isEqualTo(reveal)
        swipe.drag(200f, reveal, limit)
        assertThat(swipe.offsetOf(a)).isEqualTo(0f)
        assertThat(swipe.release(reveal)).isEqualTo(0f)

        open(swipe, a)
        swipe.begin(b, limit)
        assertThat(swipe.isOpen(a)).isFalse()
        assertThat(swipe.offsetOf(a)).isEqualTo(0f)
    }

    @Test
    fun AC_57_3_theHintMovesOnlyOneRowAndClosingResetsTheSwipe() {
        val swipe = RowSwipe()
        swipe.hint = 24f
        swipe.hintKey = a
        assertThat(swipe.offsetOf(a)).isEqualTo(24f)
        assertThat(swipe.offsetOf(b)).isEqualTo(0f)
        open(swipe, b)
        assertThat(swipe.offsetOf(b)).isEqualTo(reveal)
        assertThat(swipe.offsetOf(a)).isEqualTo(24f)
        swipe.close()
        assertThat(swipe.isOpen(b)).isFalse()
        assertThat(swipe.offsetOf(b)).isEqualTo(0f)
        assertThat(swipe.offsetOf(a)).isEqualTo(24f)
    }

    private fun open(swipe: RowSwipe, key: AppKey) {
        swipe.begin(key, limit)
        swipe.drag(-500f, reveal, limit)
        swipe.offset = swipe.release(reveal)
        swipe.settled(reveal)
    }
}
