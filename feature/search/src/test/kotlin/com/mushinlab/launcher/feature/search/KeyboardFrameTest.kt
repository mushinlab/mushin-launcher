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
    fun AC_56_2_liftStaysBetweenTheBottomAndTheRoomLeftForTheList() {
        val drag = LiftDrag(start = 100f, room = 300f, cap = 1000f)
        assertThat(drag.at(-50f)).isEqualTo(150f)
        assertThat(drag.at(-1000f)).isEqualTo(400f)
        assertThat(drag.at(500f)).isEqualTo(0f)
        assertThat(LiftDrag(100f, 300f, 200f).at(-1000f)).isEqualTo(200f)
        assertThat(LiftDrag(100f, -300f, 1000f).at(0f)).isEqualTo(0f)
    }
}
