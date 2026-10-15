package com.mushinlab.launcher

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppFont
import com.mushinlab.launcher.core.model.AppFontWeight
import com.mushinlab.launcher.core.model.BackgroundOpacity
import com.mushinlab.launcher.core.model.GestureTuning
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.PressFeedback
import com.mushinlab.launcher.core.model.StrokeColor
import com.mushinlab.launcher.feature.settings.SettingsRoute
import java.io.File
import kotlin.math.abs
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
    fun gesturesSectionHoldsSwipesAndSwipeTypingSettings() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open() }
        rule.onNodeWithTag("nav_gestures").performClick()
        rule.onNodeWithTag("gesture_UP_dropdown").assertIsDisplayed()
        rule.onNodeWithTag("kb_gesture_letters").assertDoesNotExist()
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
        rule.onNodeWithTag("palette_dropdown").assertIsDisplayed()
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
        rule.onNodeWithTag("palette_dropdown").performClick()
        listOf("LAGOON", "PLUM", "EMBER", "MIST", "ROSE").forEach {
            rule.onNodeWithTag("palette_$it").performScrollTo().assertIsDisplayed()
        }
        val original = container.spaces.spaces.value.first { it.id == normalId }.preset
        rule.onNodeWithTag("palette_ROSE").performClick()
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

    @Test
    fun backgroundImageShowsOnHomeWithAVisibilitySliderUntilRemoved() {
        rule.onNodeWithTag("home_background").assertDoesNotExist()
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.Style) }
        rule.onNodeWithTag("background_pick").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("background_opacity").assertDoesNotExist()
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.RED)
        }
        val file = File(rule.activity.cacheDir, "picked.png").apply {
            outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        try {
            assertThat(container.background.save(ImageDecoder.createSource(file))).isTrue()
            rule.onNodeWithTag("background_opacity").performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(0.8f) }
            rule.waitUntil(5_000) { abs(container.settings.settings.value.backgroundOpacity - 0.8f) < 0.01f }
            rule.runOnIdle { rule.activity.launcher.navigator.settings.close() }
            rule.onNodeWithTag("home_background").assertIsDisplayed()
            rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.Style) }
            rule.onNodeWithTag("background_clear").performScrollTo().performClick()
            rule.waitUntil(5_000) { container.background.image.value == null }
            rule.onNodeWithTag("background_opacity").assertDoesNotExist()
        } finally {
            container.background.clear()
            file.delete()
            runBlocking { container.settings.update { it.copy(backgroundOpacity = BackgroundOpacity.DEFAULT) } }
        }
    }
}
