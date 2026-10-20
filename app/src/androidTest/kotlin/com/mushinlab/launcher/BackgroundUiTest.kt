package com.mushinlab.launcher

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.CropRect
import com.mushinlab.launcher.core.ui.MushinColors
import com.mushinlab.launcher.feature.settings.SettingsRoute
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackgroundUiTest : LauncherUiTestBase() {
    @After
    fun removeBackground() = runBlocking {
        container.backgrounds.delete(normalId)
        container.looks.setBackgroundLook(normalId, brightness = 0f, blur = 0f)
    }

    private fun seedRedBackground() {
        val bitmap = Bitmap.createBitmap(SEED_WIDTH, SEED_HEIGHT, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }
        val region = CropRect(0f, 0f, SEED_WIDTH.toFloat(), SEED_HEIGHT.toFloat())
        assertThat(container.backgrounds.save(normalId, bitmap, region, SEED_WIDTH, SEED_HEIGHT)).isTrue()
    }

    private fun normalSpace() = container.spaces.spaces.value.first { it.id == normalId }

    private fun openEditor() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.BackgroundEditor(normalId)) }
        rule.waitUntil(5_000) {
            runCatching { rule.onNodeWithTag("background_preview_image").assertIsDisplayed() }.isSuccess
        }
    }

    private fun assertRowOverRed(tag: String, zebra: ComposeColor, alpha: Float = DEFAULT_ROW_ALPHA) {
        val image = rule.onNodeWithTag(tag).captureToImage().asAndroidBitmap()
        val pixel = image.getPixel(2, image.height / 2)
        val expected = zebra.toArgb()
        fun blend(row: Int, under: Int) = (row * alpha + under * (1 - alpha)).roundToInt()
        assertThat(abs(Color.red(pixel) - blend(Color.red(expected), 255))).isAtMost(TOLERANCE)
        assertThat(abs(Color.green(pixel) - blend(Color.green(expected), 0))).isAtMost(TOLERANCE)
        assertThat(abs(Color.blue(pixel) - blend(Color.blue(expected), 0))).isAtMost(TOLERANCE)
    }

    @Test
    fun everyHomeRowHasItsZebraBackgroundOverTheImage() {
        rule.onNodeWithTag("home_background").assertDoesNotExist()
        seedRedBackground()
        rule.waitUntil(5_000) { runCatching { rule.onNodeWithTag("home_background").assertIsDisplayed() }.isSuccess }
        rule.waitForIdle()
        val colors = MushinColors.from(normalSpace().palette)
        assertRowOverRed("row_0", colors.zebraA)
        assertRowOverRed("row_1", colors.zebraB)
        assertRowOverRed("row_2", colors.zebraA)
        rule.onNodeWithTag("home_add").assertIsDisplayed()
        assertRowOverRed("home_add", colors.zebraB)
    }

    @Test
    fun homeRowsFollowTheRowOpacityOfTheSpace() {
        runBlocking { container.looks.setBackgroundLook(normalId, brightness = 0f, blur = 0f, rowAlpha = 0.5f) }
        seedRedBackground()
        rule.waitUntil(5_000) { runCatching { rule.onNodeWithTag("home_background").assertIsDisplayed() }.isSuccess }
        rule.waitForIdle()
        val colors = MushinColors.from(normalSpace().palette)
        assertRowOverRed("row_0", colors.zebraA, alpha = 0.5f)
        assertRowOverRed("row_1", colors.zebraB, alpha = 0.5f)
        rule.onNodeWithTag("home_add").assertIsDisplayed()
        assertRowOverRed("home_add", colors.zebraB, alpha = 0.5f)
    }

    @Test
    fun styleNoLongerHasAGlobalBackgroundSection() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.Style) }
        rule.onNodeWithTag("stroke_dots").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("background_pick").assertDoesNotExist()
        rule.onNodeWithTag("background_opacity").assertDoesNotExist()
    }

    @Test
    fun spaceEditorNoLongerHasTheBackgroundSection() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.SpaceEditor(normalId)) }
        rule.onNodeWithTag("env_delete").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("space_background_change").assertDoesNotExist()
    }

    @Test
    fun editModeBackgroundButtonOpensTheEditorOfTheCurrentSpace() {
        seedRedBackground()
        rule.waitUntil(5_000) { normalId in container.backgrounds.versions.value }
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("home_edit_background").performClick()
        val navigator = rule.activity.launcher.navigator
        rule.waitUntil(5_000) { navigator.settings.top == SettingsRoute.BackgroundEditor(normalId) }
        rule.onNodeWithTag("background_remove").assertIsDisplayed()
        rule.onNodeWithTag("background_save").assertIsDisplayed()
        rule.onNodeWithTag("background_cancel").performClick()
        rule.waitUntil(5_000) { !navigator.settings.isOpen }
        assertThat(navigator.homeEditing).isTrue()
    }

    @Test
    fun editorShowsTheMockHomeAndSavesTheLook() {
        seedRedBackground()
        rule.waitUntil(5_000) { normalId in container.backgrounds.versions.value }
        openEditor()
        rule.onNodeWithTag("background_mock_clock").assertIsDisplayed()
        repeat(5) { rule.onNodeWithTag("background_mock_row_$it").assertIsDisplayed() }
        rule.onNodeWithTag("background_brightness").assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(-0.5f) }
        rule.onNodeWithTag("background_blur").assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.25f) }
        rule.onNodeWithTag("background_row_alpha").assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        rule.onNodeWithTag("background_save").performClick()
        rule.waitUntil(5_000) {
            normalSpace().let {
                abs(it.backgroundBrightness + 0.5f) < 0.01f &&
                    abs(it.backgroundBlur - 0.25f) < 0.01f &&
                    abs(it.backgroundRowAlpha - 0.5f) < 0.01f
            }
        }
        rule.onNodeWithTag("background_editor").assertDoesNotExist()
        assertThat(normalSpace().id in container.backgrounds.versions.value).isTrue()
    }

    @Test
    fun cancelLeavesTheLookUnchanged() {
        seedRedBackground()
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.BackgroundEditor(normalId)) }
        rule.waitUntil(5_000) {
            runCatching { rule.onNodeWithTag("background_preview_image").assertIsDisplayed() }.isSuccess
        }
        rule.onNodeWithTag("background_brightness").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        rule.onNodeWithTag("background_row_alpha").performSemanticsAction(SemanticsActions.SetProgress) { it(0.3f) }
        rule.onNodeWithTag("background_cancel").performClick()
        rule.onNodeWithTag("background_editor").assertDoesNotExist()
        rule.waitForIdle()
        assertThat(normalSpace().backgroundBrightness).isEqualTo(0f)
        assertThat(normalSpace().backgroundRowAlpha).isEqualTo(DEFAULT_ROW_ALPHA)
    }

    @Test
    fun removeDeletesTheImageAndResetsTheLook() {
        seedRedBackground()
        runBlocking { container.looks.setBackgroundLook(normalId, brightness = 0.3f, blur = 0.3f, rowAlpha = 0.4f) }
        rule.waitUntil(5_000) { normalId in container.backgrounds.versions.value }
        openEditor()
        rule.onNodeWithTag("background_remove").performClick()
        rule.waitUntil(5_000) {
            normalId !in container.backgrounds.versions.value &&
                normalSpace().backgroundBrightness == 0f &&
                normalSpace().backgroundBlur == 0f &&
                normalSpace().backgroundRowAlpha == DEFAULT_ROW_ALPHA
        }
        rule.onNodeWithTag("background_editor").assertDoesNotExist()
        rule.onNodeWithTag("home_background").assertDoesNotExist()
    }

    private companion object {
        const val SEED_WIDTH = 108
        const val SEED_HEIGHT = 240
        const val DEFAULT_ROW_ALPHA = 0.9f
        const val TOLERANCE = 12
    }
}
