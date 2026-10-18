package com.mushinlab.launcher.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpaceBackgroundTest {
    @Test
    fun brightnessAndBlurAreClampedAndNanIsNeutral() {
        assertThat(SpaceBackground.brightness(-3f)).isEqualTo(-1f)
        assertThat(SpaceBackground.brightness(0.4f)).isEqualTo(0.4f)
        assertThat(SpaceBackground.brightness(Float.NaN)).isEqualTo(0f)
        assertThat(SpaceBackground.blur(2f)).isEqualTo(1f)
        assertThat(SpaceBackground.blur(-1f)).isEqualTo(0f)
        assertThat(SpaceBackground.blur(Float.NaN)).isEqualTo(0f)
    }

    @Test
    fun rowAlphaStaysBetween20And100Percent() {
        assertThat(SpaceBackground.rowAlpha(0.55f)).isEqualTo(0.55f)
        assertThat(SpaceBackground.rowAlpha(0f)).isEqualTo(0.2f)
        assertThat(SpaceBackground.rowAlpha(-1f)).isEqualTo(0.2f)
        assertThat(SpaceBackground.rowAlpha(1.5f)).isEqualTo(1f)
        assertThat(SpaceBackground.rowAlpha(Float.POSITIVE_INFINITY)).isEqualTo(1f)
    }

    @Test
    fun rowAlphaDefaultsTo90PercentAndNanFallsBackToIt() {
        val space = Space(1, "S", SpaceType.NORMAL, 0, PalettePreset.GRAPHITE, Palettes.Graphite, createdAt = 0L)
        assertThat(space.backgroundRowAlpha).isEqualTo(0.9f)
        assertThat(SpaceBackground.rowAlpha(Float.NaN)).isEqualTo(0.9f)
    }
}
