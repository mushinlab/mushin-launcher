package com.mushinlab.launcher.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.Palettes
import com.mushinlab.launcher.core.model.StrokeColor
import org.junit.Test

class MushinStrokeTest {
    private val colors = MushinColors.from(Palettes.Graphite)

    @Test
    fun themeColoursFollowThePalette() {
        assertThat(MushinStroke(4.dp, StrokeColor.ACCENT).color(colors)).isEqualTo(colors.accent)
        assertThat(MushinStroke(4.dp, StrokeColor.TEXT).color(colors)).isEqualTo(colors.textPrimary)
    }

    @Test
    fun fixedColoursAreOpaque() {
        StrokeColor.entries.filter { it.argb != null }.forEach { color ->
            assertThat(MushinStroke(4.dp, color).color(colors).alpha).isEqualTo(1f)
            assertThat(MushinStroke(4.dp, color).color(colors)).isNotEqualTo(Color.Transparent)
        }
        assertThat(StrokeColor.entries).hasSize(12)
    }

    @Test
    fun longerDwellGivesABiggerRingUpToTwoSeconds() {
        assertThat(VectorDots.ringScale(1000)).isGreaterThan(VectorDots.ringScale(300))
        assertThat(VectorDots.ringScale(5000)).isEqualTo(VectorDots.ringScale(2000))
    }
}
