package com.mushinlab.launcher.feature.settings.tips

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OnboardingPagerTest {
    @Test
    fun nextWalksThroughAllSlidesAndStopsAtTheLast() {
        val pager = OnboardingPager(OnboardingPager.SLIDES)

        repeat(OnboardingPager.SLIDES - 1) { assertThat(pager.next()).isTrue() }

        assertThat(pager.page).isEqualTo(3)
        assertThat(pager.isLast).isTrue()
        assertThat(pager.next()).isFalse()
        assertThat(pager.page).isEqualTo(3)
    }

    @Test
    fun previousStopsAtTheFirstSlide() {
        val pager = OnboardingPager(4, initial = 1)

        assertThat(pager.previous()).isTrue()
        assertThat(pager.previous()).isFalse()
        assertThat(pager.page).isEqualTo(0)
    }

    @Test
    fun swipeLeftGoesForwardSwipeRightGoesBackShortDragsAreIgnored() {
        val pager = OnboardingPager(4)

        assertThat(pager.swipe(dragX = -100f, threshold = 50f)).isTrue()
        assertThat(pager.page).isEqualTo(1)
        assertThat(pager.swipe(dragX = 30f, threshold = 50f)).isFalse()
        assertThat(pager.page).isEqualTo(1)
        assertThat(pager.swipe(dragX = 80f, threshold = 50f)).isTrue()
        assertThat(pager.page).isEqualTo(0)
    }

    @Test
    fun restoredPageIsClampedToTheSlides() {
        assertThat(OnboardingPager(4, initial = 9).page).isEqualTo(3)
    }
}
