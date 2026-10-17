package com.mushinlab.launcher.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackgroundLookTest {
    @Test
    fun zeroBrightnessLeavesTheImageAsIs() {
        assertThat(BackgroundLook.overlay(0f)).isEqualTo(Color.Transparent)
    }

    @Test
    fun negativeBrightnessDarkensUpTo70Percent() {
        assertThat(BackgroundLook.overlay(-1f)).isEqualTo(Color.Black.copy(alpha = 0.7f))
        assertThat(BackgroundLook.overlay(-0.5f)).isEqualTo(Color.Black.copy(alpha = 0.35f))
        assertThat(BackgroundLook.overlay(-3f)).isEqualTo(Color.Black.copy(alpha = 0.7f))
    }

    @Test
    fun positiveBrightnessLightensUpTo50Percent() {
        assertThat(BackgroundLook.overlay(1f)).isEqualTo(Color.White.copy(alpha = 0.5f))
        assertThat(BackgroundLook.overlay(0.5f)).isEqualTo(Color.White.copy(alpha = 0.25f))
    }

    @Test
    fun blurGoesUpTo24Dp() {
        assertThat(BackgroundLook.blurRadius(0f)).isEqualTo(0.dp)
        assertThat(BackgroundLook.blurRadius(0.5f)).isEqualTo(12.dp)
        assertThat(BackgroundLook.blurRadius(4f)).isEqualTo(24.dp)
    }
}
