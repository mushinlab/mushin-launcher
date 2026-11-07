package com.mushinlab.launcher.core.ui

import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BrushTrailTest {
    @Test
    fun emptyAndStationaryTracesHaveNoInvalidGeometry() {
        assertThat(brushOutline(emptyList(), 8f) { 1f }).isEmpty()
        assertThat(brushOutline(List(4) { Offset.Zero }, 8f) { 1f }).isEmpty()
    }

    @Test
    fun AC_37_8_olderTailNarrowsWhileHeadKeepsItsWidth() {
        val points = listOf(Offset.Zero, Offset(10f, 0f), Offset(20f, 0f))
        val outline = brushOutline(points, 8f) { it / 2f }
        assertThat(outline[0]).isEqualTo(Offset.Zero)
        assertThat(outline[1]).isEqualTo(Offset(10f, 2f))
        assertThat(outline[2]).isEqualTo(Offset(20f, 4f))
        assertThat(outline[3]).isEqualTo(Offset(20f, -4f))
    }

    @Test
    fun reversingAndRepeatedPointsStayFinite() {
        val points = listOf(Offset.Zero, Offset(10f, 0f), Offset(10f, 0f), Offset.Zero)
        val outline = brushOutline(points, 8f) { 1f }
        assertThat(outline).hasSize(6)
        assertThat(outline.all { it.x.isFinite() && it.y.isFinite() }).isTrue()
    }
}
