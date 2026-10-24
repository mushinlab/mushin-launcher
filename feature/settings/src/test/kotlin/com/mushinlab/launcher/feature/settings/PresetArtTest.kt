package com.mushinlab.launcher.feature.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PresetArtTest {
    private fun one(argb: Int, dark: Boolean): Triple<Int, Int, Int> {
        val out = PresetArt.compose(intArrayOf(argb), width = 1, dark = dark).single()
        assertThat(out ushr 24).isEqualTo(0xFF)
        return Triple(out shr 16 and 0xFF, out shr 8 and 0xFF, out and 0xFF)
    }

    private fun assertNear(actual: Triple<Int, Int, Int>, r: Int, g: Int, b: Int, tolerance: Int = 4) {
        assertThat(actual.first).isWithin(tolerance).of(r)
        assertThat(actual.second).isWithin(tolerance).of(g)
        assertThat(actual.third).isWithin(tolerance).of(b)
    }

    @Test
    fun transparentPixelsAreWhitePaintInBothThemes() {
        assertNear(one(0x00000000, dark = false), 255, 255, 255, tolerance = 0)
        assertNear(one(0x00000000, dark = true), 255, 255, 255, tolerance = 0)
    }

    @Test
    fun emptyPaperIsLightOrDark() {
        assertNear(one(0xFFFFFFFF.toInt(), dark = false), 250, 248, 245)
        assertNear(one(0xFFFFFFFF.toInt(), dark = true), 14, 14, 15)
    }

    @Test
    fun blackInkTurnsPaleOnTheDarkPaper() {
        assertNear(one(0xFF000000.toInt(), dark = false), 0, 0, 0, tolerance = 0)
        assertNear(one(0xFF000000.toInt(), dark = true), 200, 200, 200)
    }

    @Test
    fun vermilionStaysTheSameOnTheDarkPaper() {
        assertNear(one(0xFFE6321E.toInt(), dark = true), 0xE6, 0x32, 0x1E, tolerance = 0)
        assertNear(one(0xFFE6321E.toInt(), dark = false), 0xE3, 0x31, 0x1D)
    }

    @Test
    fun grainIsSubtleAndRepeatable() {
        val paper = IntArray(64 * 64) { 0xFFFFFFFF.toInt() }
        val first = PresetArt.compose(paper, width = 64, dark = true)
        val reds = first.map { it shr 16 and 0xFF }

        assertThat(first).isEqualTo(PresetArt.compose(paper, width = 64, dark = true))
        assertThat(reds.toSet().size).isGreaterThan(1)
        assertThat(reds.all { it in 11..17 }).isTrue()
    }
}
