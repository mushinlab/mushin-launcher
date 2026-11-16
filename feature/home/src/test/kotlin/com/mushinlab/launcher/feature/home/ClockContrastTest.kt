package com.mushinlab.launcher.feature.home

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ClockContrastTest {
    private val screen = Rect(0f, 0f, 100f, 200f)
    private val clock = Rect(10f, 30f, 40f, 50f)

    @Test
    fun AC_49_4_plainThemesUseContrastingClockInk() {
        assertThat(ClockBackdrop(surface = Color.Black).ink(clock)).isEqualTo(Color.White)
        assertThat(ClockBackdrop(surface = Color.White).ink(clock)).isEqualTo(Color.Black)
    }

    @Test
    fun AC_49_4_wallpaperOverridesThemeBrightness() {
        assertThat(backdrop(Color.White).ink(clock)).isEqualTo(Color.Black)
        assertThat(backdrop(Color.Black).copy(surface = Color.White).ink(clock)).isEqualTo(Color.White)
    }

    @Test
    fun AC_49_4_brightnessAdjustmentChangesClockInk() {
        assertThat(backdrop(Color.White).copy(brightness = -1f).ink(clock)).isEqualTo(Color.White)
        assertThat(backdrop(Color.Black).copy(brightness = 1f).ink(clock)).isEqualTo(Color.Black)
    }

    @Test
    fun AC_49_4_clockUsesItsOwnAreaAfterMoving() {
        val pixels = pixels(10, 20) { x, _ -> if (x < 5) Color.White else Color.Black }
        val backdrop = ClockBackdrop(pixels, frame = screen)
        assertThat(backdrop.ink(clock)).isEqualTo(Color.Black)
        assertThat(backdrop.ink(clock.translate(50f, 0f))).isEqualTo(Color.White)
    }

    @Test
    fun AC_49_4_centreCropAndFrameOffsetMatchTheDisplayedImage() {
        val pixels = pixels(20, 20) { x, _ -> if (x in 5..14) Color.Black else Color.White }
        val backdrop = ClockBackdrop(pixels, surface = Color.White, frame = screen.translate(30f, 60f))
        assertThat(backdrop.ink(clock.translate(30f, 60f))).isEqualTo(Color.White)
    }

    @Test
    fun AC_49_4_transparentWallpaperUsesTheThemeUnderneath() {
        val backdrop = backdrop(Color.Transparent)
        assertThat(backdrop.ink(clock)).isEqualTo(Color.White)
        assertThat(backdrop.copy(surface = Color.White).ink(clock)).isEqualTo(Color.Black)
    }

    @Test
    fun AC_49_4_unmeasuredLabelIgnoresBlurAndUsesTheSurface() {
        val backdrop = ClockBackdrop(
            pixels(10, 20) { _, _ -> Color.White },
            surface = Color.Black,
            blurPx = 30f,
            frame = screen,
        )
        assertThat(backdrop.ink(Rect.Zero)).isEqualTo(Color.White)
    }

    @Test
    fun AC_49_4_blurSamplesNearbyBackgroundAndClampsToScreen() {
        val pixels = pixels(10, 20) { _, y -> if (y in 3..4) Color.Black else Color.White }
        val backdrop = ClockBackdrop(pixels, frame = screen)
        assertThat(backdrop.ink(clock)).isEqualTo(Color.White)
        assertThat(backdrop.copy(blurPx = 30f).ink(clock)).isEqualTo(Color.Black)
    }

    private fun backdrop(color: Color) = ClockBackdrop(pixels(10, 20) { _, _ -> color }, frame = screen)

    private fun pixels(width: Int, height: Int, color: (Int, Int) -> Color) = PixelMap(
        IntArray(width * height) { color(it % width, it / width).toArgb() },
        width,
        height,
        0,
        width,
    )
}
