package com.bluecubelabs.env.core.apps

import com.bluecubelabs.env.core.apps.icons.IconMath
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IconMathTest {
    @Test
    fun bucketSizeRoundsUpToMultipleOf8() {
        assertThat(IconMath.bucketSize(74)).isEqualTo(80)
        assertThat(IconMath.bucketSize(53)).isEqualTo(56)
        assertThat(IconMath.bucketSize(80)).isEqualTo(80)
    }

    @Test
    fun brightnessIsStretchedIntoAlphaFrom15To100Percent() {
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        val transparent = 0x00000000
        val alpha = IconMath.brightnessToAlpha(intArrayOf(black, white, transparent))
        assertThat(alpha[0].toInt() and 0xFF).isEqualTo(38)
        assertThat(alpha[1].toInt() and 0xFF).isEqualTo(255)
        assertThat(alpha[2].toInt() and 0xFF).isEqualTo(0)
    }

    @Test
    fun uniformColorBecomesFullyOpaqueMask() {
        val red = 0xFFFF0000.toInt()
        val alpha = IconMath.brightnessToAlpha(IntArray(4) { red })
        assertThat(alpha.map { it.toInt() and 0xFF }).containsExactly(255, 255, 255, 255)
    }

    @Test
    fun AC_04_edge_coverageAbove92PercentTriggersLetterFallback() {
        val full = ByteArray(100) { 0xFF.toByte() }
        val ring = ByteArray(100) { i -> if (i < 80) 0xFF.toByte() else 0 }
        assertThat(IconMath.coverage(full)).isGreaterThan(IconMath.COVERAGE_LIMIT)
        assertThat(IconMath.coverage(ring)).isLessThan(IconMath.COVERAGE_LIMIT)
    }

    @Test
    fun monochromeLayerKeepsOnlyAlpha() {
        val alpha = IconMath.alphaFromPixels(intArrayOf(0x80123456.toInt(), 0x00FFFFFF))
        assertThat(alpha.map { it.toInt() and 0xFF }).containsExactly(0x80, 0).inOrder()
    }
}
