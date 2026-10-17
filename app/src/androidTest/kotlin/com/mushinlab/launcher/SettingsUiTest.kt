package com.mushinlab.launcher

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppFont
import com.mushinlab.launcher.core.model.AppFontWeight
import com.mushinlab.launcher.core.model.GestureTuning
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.PressFeedback
import com.mushinlab.launcher.core.model.StrokeColor
import com.mushinlab.launcher.feature.settings.SettingsRoute
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsUiTest : LauncherUiTestBase() {
    @Test
    fun selectedOptionIsMarkedByBackgroundAndPressStyleApplies() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open() }
        rule.onNodeWithTag("nav_system").performScrollTo().performClick()
        rule.onNodeWithTag("nav_advanced").performScrollTo().performClick()
        rule.onNodeWithTag("nav_press").performClick()
        rule.onNodeWithTag("press_dropdown").performClick()
        rule.onNodeWithTag("press_FLASH").assertIsSelected()
        rule.onNodeWithTag("press_DIM").assertIsNotSelected().performClick()
        rule.waitUntil(5_000) { container.settings.settings.value.pressFeedback == PressFeedback.DIM }
        rule.onNodeWithTag("press_menu").assertDoesNotExist()
        rule.onNodeWithTag("press_dropdown").performClick()
        rule.onNodeWithTag("press_DIM").assertIsSelected()
        rule.onNodeWithTag("press_FLASH").assertIsNotSelected()
    }

    @Test
    fun rootEndsWithTipsThenSystemAndHasNoSpacesEntry() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open() }
        val order = listOf(
            "nav_style",
            "nav_keyboard",
            "nav_gestures",
            "nav_apps",
            "nav_tips",
            "nav_system",
        )
        val tops = order.map { rule.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot.top }
        assertThat(tops).isInStrictOrder()
        val navRow = SemanticsMatcher("nav row") {
            it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("nav_") == true
        }
        val navTags = rule.onAllNodes(navRow).fetchSemanticsNodes().map { it.config[SemanticsProperties.TestTag] }
        assertThat(navTags).containsExactlyElementsIn(order)
        rule.onNodeWithTag("nav_envs").assertDoesNotExist()
    }

    @Test
    fun gesturesSectionHoldsOnlySwipes() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open() }
        rule.onNodeWithTag("nav_gestures").performClick()
        rule.onNodeWithTag("gesture_UP_dropdown").assertIsDisplayed()
        rule.onNodeWithTag("kb_gesture_letters").assertDoesNotExist()
        rule.onNodeWithTag("anywhere_assistant").assertDoesNotExist()
        rule.onNodeWithTag("anywhere_tile").assertDoesNotExist()
        rule.onNodeWithTag("kb_gesture_shift").assertDoesNotExist()
        rule.onNodeWithTag("kb_gesture_margin").assertDoesNotExist()
    }

    @Test
    fun keyboardSectionHoldsSwipeTypingSettings() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open() }
        rule.onNodeWithTag("nav_keyboard").performClick()
        rule.onNodeWithTag("kb_gesture_shift").performScrollTo().performTouchInput { click(centerLeft) }
        rule.waitUntil(5_000) {
            container.settings.settings.value.gestureShiftPenalty == GestureTuning.SHIFT_PENALTY_MIN
        }
        rule.onNodeWithTag("kb_gesture_margin").performScrollTo().performTouchInput { click(centerRight) }
        rule.waitUntil(5_000) {
            container.settings.settings.value.gestureSuggestMargin == GestureTuning.SUGGEST_MARGIN_MAX
        }
        rule.onNodeWithTag("kb_gesture_tuning_reset").performScrollTo().performClick()
        rule.waitUntil(5_000) {
            container.settings.settings.value.gestureShiftPenalty == GestureTuning.SHIFT_PENALTY &&
                container.settings.settings.value.gestureSuggestMargin == GestureTuning.SUGGEST_MARGIN
        }
    }

    @Test
    fun styleChangesTheFontOfTheWholeApp() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open() }
        rule.onNodeWithTag("nav_style").performClick()
        rule.onNodeWithTag("palette_picker").assertIsDisplayed()
        rule.onNodeWithTag("style_colored_icons").assertIsDisplayed()
        rule.onNodeWithTag("font_dropdown").performScrollTo().performClick()
        rule.onNodeWithTag("font_MONTSERRAT").performClick()
        rule.onNodeWithTag("font_weight_dropdown").performScrollTo().performClick()
        rule.onNodeWithTag("font_weight_LIGHT").performClick()
        rule.waitUntil(5_000) {
            container.settings.settings.value.let {
                it.font == AppFont.MONTSERRAT && it.fontWeight == AppFontWeight.LIGHT
            }
        }
    }

    @Test
    fun styleOffersElevenThemesIncludingTheNewOnes() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open() }
        rule.onNodeWithTag("nav_style").performClick()
        listOf("LAGOON", "PLUM", "EMBER", "MIST", "ROSE").forEach {
            rule.onNodeWithTag("palette_$it").performScrollTo().assertIsDisplayed()
        }
        val original = container.spaces.spaces.value.first { it.id == normalId }.preset
        rule.onNodeWithTag("palette_ROSE").performScrollTo().performClick()
        try {
            rule.waitUntil(5_000) {
                container.spaces.spaces.value.first { it.id == normalId }.preset == PalettePreset.ROSE
            }
        } finally {
            runBlocking { container.spaces.setPalette(normalId, original) }
        }
    }

    @Test
    fun styleChangesStrokeColour() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.Style) }
        rule.onNodeWithTag("stroke_color_dropdown").performScrollTo().performClick()
        rule.onNodeWithTag("stroke_color_PINK").performScrollTo().performClick()
        rule.waitUntil(5_000) { container.settings.settings.value.strokeColor == StrokeColor.PINK }
    }

    @Test
    fun styleTurnsOffDotsOnTheLine() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.Style) }
        rule.onNodeWithTag("stroke_dots").performScrollTo().performClick()
        rule.waitUntil(5_000) { !container.settings.settings.value.strokeDots }
        runBlocking { container.settings.update { it.copy(strokeDots = true) } }
    }
}
