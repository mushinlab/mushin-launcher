package com.mushinlab.launcher

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.feature.settings.R
import com.mushinlab.launcher.feature.settings.tips.Tip
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingUiTest : LauncherUiTestBase() {
    @After
    fun markOnboardingDone() = runBlocking {
        container.settings.update { it.copy(onboardingDone = true) }
    }

    @Test
    fun firstRunShowsFourSlidesAndDoneClosesThemForGood() {
        showOnboarding()
        repeat(SLIDES) { page ->
            rule.onNodeWithTag("onboarding_slide_$page").assertIsDisplayed()
            rule.onNodeWithTag("onboarding_next").performClick()
        }
        rule.waitUntil(5_000) { !exists("onboarding") }
        rule.waitUntil(5_000) { container.settings.settings.value.onboardingDone }

        rule.activityRule.scenario.recreate()
        rule.waitUntil(5_000) { exists("home") }

        rule.onNodeWithTag("onboarding").assertDoesNotExist()
        rule.onNodeWithTag("home").assertIsDisplayed()
    }

    @Test
    fun backReturnsToThePreviousSlideAndSkipClosesOnboarding() {
        showOnboarding()
        rule.onNodeWithTag("onboarding_next").performClick()
        rule.onNodeWithTag("onboarding_slide_1").assertIsDisplayed()

        Espresso.pressBack()
        rule.onNodeWithTag("onboarding_slide_0").assertIsDisplayed()

        rule.onNodeWithTag("onboarding_skip").performClick()
        rule.waitUntil(5_000) { !exists("onboarding") }
        rule.waitUntil(5_000) { container.settings.settings.value.onboardingDone }
    }

    @Test
    fun onboardingTakesSwipesAndKeepsHomeGesturesAway() {
        showOnboarding()
        rule.onNodeWithTag("onboarding").performTouchInput { doubleClick(center) }
        rule.onNodeWithTag("keyboard").assertDoesNotExist()
        assertThat(rule.activity.launcher.navigator.overlay).isEqualTo(Overlay.None)

        rule.onNodeWithTag("onboarding").performTouchInput { swipeLeft() }
        rule.onNodeWithTag("onboarding_slide_1").assertIsDisplayed()
    }

    @Test
    fun settingsTipsListsThreeTipsAndPlaysOne() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open() }
        rule.onNodeWithTag("nav_tips").performScrollTo().performClick()
        Tip.entries.forEach { rule.onNodeWithTag("tip_${it.id}").assertIsDisplayed() }
        assertThat(Tip.entries).hasSize(3)

        rule.onNodeWithTag("tip_memory").performClick()

        rule.onNodeWithTag("tip_player_memory").assertIsDisplayed()
        rule.onNodeWithText(rule.activity.getString(R.string.tip_memory_title)).assertIsDisplayed()
        Espresso.pressBack()
        rule.onNodeWithTag("tip_space").assertIsDisplayed()
    }

    private fun showOnboarding() {
        runBlocking { container.settings.update { it.copy(onboardingDone = false) } }
        rule.waitUntil(5_000) { exists("onboarding") }
    }

    private fun exists(tag: String) = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        const val SLIDES = 4
    }
}
