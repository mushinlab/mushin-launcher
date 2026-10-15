package com.mushinlab.launcher.core.ui

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnchoredPositionTest {
    private val window = IntSize(1080, 2400)
    private val insets = IntRect(0, 110, 0, 63)
    private val margin = 21
    private val gap = 21
    private val popup = IntSize(630, 900)
    private val safe = AnchoredPosition.safeArea(window, insets, margin)

    private val anchors = listOf(
        0 to 0, 540 to 0, 1080 to 0,
        0 to 1200, 540 to 1200, 1080 to 1200,
        0 to 2400, 540 to 2400, 1080 to 2400,
    )

    @Test
    fun AC_06_4_popupStaysInsideSafeAreaForNineAnchors() {
        anchors.forEach { (x, y) ->
            val pos = AnchoredPosition.belowPoint(x, y, popup, safe, gap)
            assertThat(pos.x).isAtLeast(safe.left)
            assertThat(pos.y).isAtLeast(safe.top)
            assertThat(pos.x + popup.width).isAtMost(safe.right)
            assertThat(pos.y + popup.height).isAtMost(safe.bottom)
        }
    }

    @Test
    fun popupGoesBelowFingerWhenItFits() {
        val pos = AnchoredPosition.belowPoint(540, 600, popup, safe, gap)
        assertThat(pos.y).isEqualTo(621)
        assertThat(pos.x).isEqualTo(540 - 315)
    }

    @Test
    fun popupGoesAboveFingerNearBottom() {
        val pos = AnchoredPosition.belowPoint(540, 2000, popup, safe, gap)
        assertThat(pos.y).isEqualTo(2000 - 21 - 900)
    }

    @Test
    fun popupIsPinnedToBottomWhenNeitherFits() {
        val tall = IntSize(630, 1500)
        val pos = AnchoredPosition.belowPoint(540, 1200, tall, safe, gap)
        assertThat(pos.y).isEqualTo(safe.bottom - 1500)
    }

    @Test
    fun safeAreaAddsMarginToInsets() {
        assertThat(safe).isEqualTo(IntRect(21, 131, 1059, 2316))
    }
}
