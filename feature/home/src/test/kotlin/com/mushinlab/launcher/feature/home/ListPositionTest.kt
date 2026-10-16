package com.mushinlab.launcher.feature.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ListPositionTest {
    @Test
    fun handleMovesToThreeTimesItsNaturalDistanceFromTheTop() {
        val offset = autoListOffset(naturalTop = 477f, listHeight = 1650f, rowHeight = 168f, maxOffset = 1260f)
        assertThat(offset).isEqualTo(954f)
        assertThat(477f + offset).isEqualTo(477f * 3)
    }

    @Test
    fun smallScreenKeepsRoomForFourRows() {
        val offset = autoListOffset(naturalTop = 400f, listHeight = 900f, rowHeight = 150f, maxOffset = 1260f)
        assertThat(offset).isEqualTo(300f)
        assertThat(900f - offset).isEqualTo(150f * 4)
    }

    @Test
    fun listThatCannotFitFourRowsStaysAtTheTop() {
        assertThat(autoListOffset(naturalTop = 400f, listHeight = 500f, rowHeight = 150f, maxOffset = 1260f))
            .isEqualTo(0f)
    }

    @Test
    fun offsetIsCappedAtTheMaximum() {
        val offset = autoListOffset(naturalTop = 900f, listHeight = 4000f, rowHeight = 168f, maxOffset = 1260f)
        assertThat(offset).isEqualTo(1260f)
    }
}
