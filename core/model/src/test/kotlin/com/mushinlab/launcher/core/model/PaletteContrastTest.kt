package com.mushinlab.launcher.core.model

import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.pow
import org.junit.Test

class PaletteContrastTest {
    @Test
    fun textPrimaryOnBackgroundIsAtLeast7to1() {
        Palettes.presets.forEach { (preset, palette) ->
            assertWithMessage("$preset textPrimary")
                .that(contrast(palette.textPrimary, palette.background))
                .isAtLeast(7.0)
        }
    }

    @Test
    fun textSecondaryOnBackgroundIsAtLeast4_5to1() {
        Palettes.presets.forEach { (preset, palette) ->
            assertWithMessage("$preset textSecondary")
                .that(contrast(palette.textSecondary, palette.background))
                .isAtLeast(4.5)
        }
    }

    @Test
    fun elevenPresetsWithAccentReadableAsTextAndNoPureWhiteText() {
        assertWithMessage("presets").that(Palettes.presets.size).isEqualTo(11)
        Palettes.presets.forEach { (preset, palette) ->
            assertWithMessage("$preset accent")
                .that(contrast(palette.accent, palette.background))
                .isAtLeast(4.5)
            assertWithMessage("$preset textPrimary is not pure white or black")
                .that(palette.textPrimary and 0xFFFFFF)
                .isNoneOf(0xFFFFFF, 0x000000)
        }
    }

    @Test
    fun zebraBDiffersFromZebraAByThreeToFivePercentLightness() {
        Palettes.presets.forEach { (preset, palette) ->
            val delta = kotlin.math.abs(lightness(palette.zebraB) - lightness(palette.zebraA))
            assertWithMessage(
                "$preset zebra delta",
            ).that(delta).isIn(com.google.common.collect.Range.closed(0.02, 0.06))
        }
    }

    private fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun luminance(color: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((color shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    private fun lightness(color: Int): Double {
        val r = ((color shr 16) and 0xFF) / 255.0
        val g = ((color shr 8) and 0xFF) / 255.0
        val b = (color and 0xFF) / 255.0
        return (maxOf(r, g, b) + minOf(r, g, b)) / 2
    }
}
