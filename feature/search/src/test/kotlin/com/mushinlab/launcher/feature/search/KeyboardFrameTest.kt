package com.mushinlab.launcher.feature.search

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KeyboardFrameTest {
    private val total = 1000f

    @Test
    fun widthAndShiftSurviveARoundTrip() {
        val frame = KeyboardFrame.of(0.8f, -0.5f, total)
        assertThat(frame.left).isWithin(0.01f).of(50f)
        assertThat(frame.fraction(total)).isWithin(0.001f).of(0.8f)
        assertThat(frame.shift(total)).isWithin(0.001f).of(-0.5f)
    }

    @Test
    fun sideAnchorsChangeWidthAndKeepTheOtherEdge() {
        val frame = KeyboardFrame.of(1f, 0f, total)
        val narrower = frame.dragStart(150f, total)
        assertThat(narrower.right).isEqualTo(1000f)
        assertThat(narrower.fraction(total)).isWithin(0.001f).of(0.85f)
        assertThat(narrower.shift(total)).isEqualTo(1f)
        val both = narrower.dragEnd(-100f, total)
        assertThat(both.left).isEqualTo(150f)
        assertThat(both.fraction(total)).isWithin(0.001f).of(0.75f)
    }

    @Test
    fun widthNeverDropsBelowTheMinimumOrLeavesTheScreen() {
        val frame = KeyboardFrame.of(1f, 0f, total)
        assertThat(frame.dragStart(900f, total).width).isEqualTo(600f)
        assertThat(frame.dragEnd(-900f, total).width).isEqualTo(600f)
        assertThat(frame.dragStart(-100f, total).left).isEqualTo(0f)
        assertThat(frame.dragEnd(100f, total).right).isEqualTo(1000f)
    }

    @Test
    fun bottomAnchorMovesWithinTheScreen() {
        val frame = KeyboardFrame.of(0.6f, 0f, total)
        assertThat(frame.move(-500f, total).shift(total)).isEqualTo(-1f)
        assertThat(frame.move(500f, total).shift(total)).isEqualTo(1f)
        assertThat(frame.move(100f, total).width).isEqualTo(600f)
    }
}
