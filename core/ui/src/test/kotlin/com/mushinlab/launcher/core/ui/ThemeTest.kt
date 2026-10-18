package com.mushinlab.launcher.core.ui

import androidx.compose.ui.text.font.FontWeight
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppFont
import com.mushinlab.launcher.core.model.AppFontWeight
import com.mushinlab.launcher.core.model.Palettes
import org.junit.Test

class ThemeTest {
    private val colors = MushinColors.from(Palettes.Graphite)

    @Test
    fun rowsAlternateBetweenTheTwoZebraColours() {
        assertThat(colors.zebra(0)).isEqualTo(colors.zebraA)
        assertThat(colors.zebra(1)).isEqualTo(colors.zebraB)
        assertThat(colors.zebra(2)).isEqualTo(colors.zebraA)
    }

    @Test
    fun homeRowsAreSeeThroughOnlyOverAnImage() {
        assertThat(colors.row(0, overImage = false, alpha = 0.5f)).isEqualTo(colors.zebraA)
        assertThat(colors.row(1, overImage = false, alpha = 0.5f)).isEqualTo(colors.zebraB)
        assertThat(colors.row(0, overImage = true, alpha = 0.9f)).isEqualTo(colors.zebraA.copy(alpha = 0.9f))
        assertThat(colors.row(1, overImage = true, alpha = 0.5f)).isEqualTo(colors.zebraB.copy(alpha = 0.5f))
    }

    @Test
    fun rowOpacityOverAnImageIsLimitedTo20To100Percent() {
        assertThat(colors.row(0, overImage = true, alpha = 0.05f)).isEqualTo(colors.zebraA.copy(alpha = 0.2f))
        assertThat(colors.row(0, overImage = true, alpha = 2f)).isEqualTo(colors.zebraA.copy(alpha = 1f))
        assertThat(colors.row(0, overImage = true, alpha = Float.NaN)).isEqualTo(colors.zebraA.copy(alpha = 0.9f))
    }

    @Test
    fun fontWeightFollowsTheSetting() {
        assertThat(MushinFont.of(AppFont.INTER, AppFontWeight.MEDIUM).weight).isEqualTo(FontWeight(500))
    }
}
