package com.mushinlab.launcher.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppFont
import com.mushinlab.launcher.core.model.AppFontWeight
import com.mushinlab.launcher.core.model.Palettes
import org.junit.Test

class ThemeTest {
    private val colors = MushinColors.from(Palettes.Graphite)

    @Test
    fun AC_74_1_bareIconsKeepContrastOnTheSpaceBackgroundInEveryFixedTheme() {
        Palettes.presets.values.forEach { palette ->
            val colors = MushinColors.from(palette)
            listOf(colors.textPrimary, colors.textSecondary, colors.accent, colors.danger).forEach { tint ->
                assertThat(contrast(tint, colors.background)).isAtLeast(3f)
            }
            assertThat(contrast(colors.background, colors.accent)).isAtLeast(3f)
        }
    }

    private fun contrast(a: Color, b: Color): Float =
        (maxOf(a.luminance(), b.luminance()) + 0.05f) / (minOf(a.luminance(), b.luminance()) + 0.05f)

    @Test
    fun AC_65_2_homeRowsShareOneBackgroundSeeThroughOnlyOverAnImage() {
        assertThat(colors.row(overImage = false, alpha = 0.5f)).isEqualTo(colors.zebraA)
        assertThat(colors.row(overImage = true, alpha = 0.9f)).isEqualTo(colors.zebraA.copy(alpha = 0.9f))
    }

    @Test
    fun rowOpacityOverAnImageIsLimitedTo20To100Percent() {
        assertThat(colors.row(overImage = true, alpha = 0.05f)).isEqualTo(colors.zebraA.copy(alpha = 0.2f))
        assertThat(colors.row(overImage = true, alpha = 2f)).isEqualTo(colors.zebraA.copy(alpha = 1f))
        assertThat(colors.row(overImage = true, alpha = Float.NaN)).isEqualTo(colors.zebraA.copy(alpha = 0.9f))
    }

    @Test
    fun fontWeightFollowsTheSetting() {
        assertThat(MushinFont.of(AppFont.INTER, AppFontWeight.MEDIUM).weight).isEqualTo(FontWeight(500))
    }
}
