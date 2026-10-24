package com.mushinlab.launcher

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppFont
import com.mushinlab.launcher.core.model.AppFontWeight
import com.mushinlab.launcher.core.model.ClockFormat
import com.mushinlab.launcher.core.model.GestureTuning
import com.mushinlab.launcher.core.model.HomeTextAlign
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.PressFeedback
import com.mushinlab.launcher.core.model.StrokeColor
import com.mushinlab.launcher.feature.settings.R as SettingsR
import com.mushinlab.launcher.feature.settings.SettingsRoute
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsUiTest : LauncherUiTestBase() {
    @Test
    fun selectedOptionIsMarkedByBackgroundAndPressStyleApplies() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.ThemeSheet) }
        rule.onNodeWithTag("press_dropdown").performScrollTo().performClick()
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
        rule.onNodeWithTag("nav_style").assertDoesNotExist()
        val order = listOf(
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
    fun gestureInputHidesItsSettingsWhileOff() {
        runBlocking { container.settings.update { it.copy(gestureInput = false) } }
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.Gestures) }
        rule.onNodeWithTag("gesture_input").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("learn_hints").assertDoesNotExist()
        rule.onNodeWithTag("memory_add_gesture").assertDoesNotExist()
        rule.onNodeWithTag("gesture_input").performClick()
        rule.waitUntil(5_000) { container.settings.settings.value.gestureInput }
        rule.onNodeWithTag("learn_hints").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("memory_add_gesture").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun systemHasPrivacyWithStoredDataFoldedAndNoAdvanced() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.System) }
        rule.onNodeWithTag("nav_advanced").assertDoesNotExist()
        rule.onNodeWithTag("nav_privacy").performScrollTo().performClick()
        rule.onNodeWithTag("privacy_delete_memory").assertIsDisplayed()
        val history = rule.activity.getString(SettingsR.string.memory_usage)
        rule.onNodeWithText(history).assertDoesNotExist()
        rule.onNodeWithTag("privacy_data").performScrollTo().performClick()
        rule.waitUntil(5_000) {
            runCatching { rule.onNodeWithText(history).assertExists() }.isSuccess
        }
    }

    @Test
    fun gesturesHoldTheStrokeSettings() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open() }
        rule.onNodeWithTag("nav_gestures").performClick()
        rule.onNodeWithTag("stroke_width").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("stroke_dots").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun clockSheetChangesTheFormatAndHidesTheDate() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.ClockSheet) }
        rule.onNodeWithTag("clock_dropdown").performScrollTo().performClick()
        rule.onNodeWithTag("clock_H24").performClick()
        rule.onNodeWithTag("show_date").performScrollTo().performClick()
        try {
            rule.waitUntil(5_000) {
                container.settings.settings.value.let { it.clockFormat == ClockFormat.H24 && !it.showDate }
            }
            rule.runOnIdle { rule.activity.launcher.navigator.settings.close() }
            rule.onNodeWithTag("date").assertDoesNotExist()
        } finally {
            runBlocking {
                container.settings.update { it.copy(clockFormat = ClockFormat.SYSTEM, showDate = true) }
            }
        }
    }

    @Test
    fun themeSheetHoldsTheNotificationCount() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.ThemeSheet) }
        rule.onNodeWithTag("notification_counts").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun textSheetChangesTheFontOfTheWholeAppAndTheAlignment() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.TextSheet) }
        rule.onNodeWithTag("font_dropdown").performScrollTo().performClick()
        rule.onNodeWithTag("font_MONTSERRAT").performClick()
        rule.onNodeWithTag("font_weight_dropdown").performScrollTo().performClick()
        rule.onNodeWithTag("font_weight_LIGHT").performClick()
        rule.onNodeWithTag("text_align_dropdown").performScrollTo().performClick()
        rule.onNodeWithTag("text_align_END").performClick()
        rule.waitUntil(5_000) {
            container.settings.settings.value.let {
                it.font == AppFont.MONTSERRAT &&
                    it.fontWeight == AppFontWeight.LIGHT &&
                    it.homeTextAlign == HomeTextAlign.END
            }
        }
    }

    @Test
    fun themeSheetOffersElevenThemesIncludingTheNewOnes() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.ThemeSheet) }
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
    fun gesturesChangeStrokeColour() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.Gestures) }
        rule.onNodeWithTag("stroke_color_dropdown").performScrollTo().performClick()
        rule.onNodeWithTag("stroke_color_PINK").performScrollTo().performClick()
        rule.waitUntil(5_000) { container.settings.settings.value.strokeColor == StrokeColor.PINK }
    }

    @Test
    fun gesturesTurnOffDotsOnTheLine() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.Gestures) }
        rule.onNodeWithTag("stroke_dots").performScrollTo().performClick()
        rule.waitUntil(5_000) { !container.settings.settings.value.strokeDots }
        runBlocking { container.settings.update { it.copy(strokeDots = true) } }
    }
}
