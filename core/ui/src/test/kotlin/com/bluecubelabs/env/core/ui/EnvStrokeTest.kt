package com.bluecubelabs.env.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bluecubelabs.env.core.model.Palettes
import com.bluecubelabs.env.core.model.StrokeColor
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EnvStrokeTest {
    private val colors = EnvColors.from(Palettes.Graphite)

    @Test
    fun themeColoursFollowThePalette() {
        assertThat(EnvStroke(4.dp, StrokeColor.ACCENT).color(colors)).isEqualTo(colors.accent)
        assertThat(EnvStroke(4.dp, StrokeColor.TEXT).color(colors)).isEqualTo(colors.textPrimary)
    }

    @Test
    fun fixedColoursAreOpaque() {
        StrokeColor.entries.filter { it.argb != null }.forEach { color ->
            assertThat(EnvStroke(4.dp, color).color(colors).alpha).isEqualTo(1f)
            assertThat(EnvStroke(4.dp, color).color(colors)).isNotEqualTo(Color.Transparent)
        }
        assertThat(StrokeColor.entries).hasSize(12)
    }

    @Test
    fun longerDwellGivesABiggerRingUpToTwoSeconds() {
        assertThat(VectorDots.ringScale(1000)).isGreaterThan(VectorDots.ringScale(300))
        assertThat(VectorDots.ringScale(5000)).isEqualTo(VectorDots.ringScale(2000))
    }
}
