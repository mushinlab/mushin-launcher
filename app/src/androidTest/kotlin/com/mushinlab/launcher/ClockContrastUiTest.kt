package com.mushinlab.launcher

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.CropRect
import com.mushinlab.launcher.core.model.HostedWidget
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.WidgetCell
import com.mushinlab.launcher.core.model.WidgetLayout
import com.mushinlab.launcher.core.model.WidgetState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClockContrastUiTest : LauncherUiTestBase() {
    @Before
    fun clearBackdrop() = runBlocking {
        container.backgrounds.delete(normalId)
        container.looks.setBackgroundLook(normalId, brightness = 0f, blur = 0f)
        container.widgets.update { WidgetState() }
    }

    @After
    fun restoreClock() = runBlocking {
        clearBackdrop()
        container.spaces.setPalette(normalId, PalettePreset.GRAPHITE)
    }

    @Test
    fun AC_49_4_clockAndDateFollowThemeWallpaperAndBrightness() {
        runBlocking { container.spaces.setPalette(normalId, PalettePreset.PAPER) }
        awaitInk(ComposeColor.Black)
        seedBackground { _, _ -> Color.BLACK }
        awaitInk(ComposeColor.White)
        runBlocking { container.looks.setBackgroundLook(normalId, brightness = 1f, blur = 0f) }
        awaitInk(ComposeColor.Black)
        runBlocking { container.looks.setBackgroundLook(normalId, brightness = 0f, blur = 0f) }
        seedBackground { _, _ -> Color.WHITE }
        awaitInk(ComposeColor.Black)
        runBlocking { container.looks.setBackgroundLook(normalId, brightness = -1f, blur = 1f) }
        awaitInk(ComposeColor.White)
    }

    @Test
    fun AC_49_4_clockRechecksTheBackgroundAfterMovingToAnotherCell() {
        seedBackground { x, width -> if (x < width / 2) Color.WHITE else Color.BLACK }
        awaitInk(ComposeColor.Black)
        runBlocking {
            container.widgets.update {
                val clock = HostedWidget.clock.copy(cell = WidgetCell(1, 0))
                WidgetState(layouts = mapOf(normalId to WidgetLayout(listOf(clock))))
            }
        }
        awaitInk(ComposeColor.White)
    }

    private fun seedBackground(color: (Int, Int) -> Int) {
        val display = rule.activity.resources.displayMetrics
        val width = display.widthPixels
        val height = display.heightPixels
        val pixels = IntArray(width * height) { color(it % width, width) }
        val bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        val region = CropRect(0f, 0f, width.toFloat(), height.toFloat())
        assertThat(container.backgrounds.save(normalId, bitmap, region, width, height)).isTrue()
        bitmap.recycle()
    }

    private fun awaitInk(expected: ComposeColor) {
        rule.waitUntil(5_000) {
            listOf("clock", "date").all { tag ->
                val results = mutableListOf<TextLayoutResult>()
                rule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
                results.single().layoutInput.style.color == expected
            }
        }
    }
}
