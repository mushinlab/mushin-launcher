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
    fun homeRowsAreSlightlySeeThroughOnlyOverAnImage() {
        assertThat(colors.row(0, overImage = false)).isEqualTo(colors.zebraA)
        assertThat(colors.row(1, overImage = false)).isEqualTo(colors.zebraB)
        assertThat(colors.row(0, overImage = true)).isEqualTo(colors.zebraA.copy(alpha = 0.9f))
        assertThat(colors.row(1, overImage = true)).isEqualTo(colors.zebraB.copy(alpha = 0.9f))
    }

    @Test
    fun fontWeightFollowsTheSetting() {
        assertThat(MushinFont.of(AppFont.INTER, AppFontWeight.MEDIUM).weight).isEqualTo(FontWeight(500))
    }
}
