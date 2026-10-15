package com.mushinlab.launcher

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.mushinlab.launcher.core.model.AppFont
import com.mushinlab.launcher.core.model.AppFontWeight
import com.mushinlab.launcher.core.model.ClockPosition
import com.mushinlab.launcher.core.model.GestureTuning
import com.mushinlab.launcher.core.model.KeyboardFit
import com.mushinlab.launcher.core.model.PressFeedback
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.model.StrokeColor
import com.mushinlab.launcher.feature.search.KeyboardDimens
import com.mushinlab.launcher.feature.search.glideCenters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule

abstract class LauncherUiTestBase {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    protected val container get() = ApplicationProvider.getApplicationContext<MushinApp>().container
    protected var normalId = 0L

    @Before
    fun seedFavorites() = runBlocking {
        val normal = container.spaces.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.NORMAL }
        normalId = normal.id
        container.settings.update { it.copy(currentSpaceId = normal.id) }
        val apps = container.catalog.apps.first { it.size >= 3 }.sortedBy { it.label }.take(3)
        container.spaces.setItems(normal.id, apps.map { it.key })
        rule.waitUntil(5_000) { rule.activity.launcher.navigator.overlay == Overlay.None }
        rule.waitUntil(5_000) {
            runCatching { rule.onNodeWithTag("row_2").assertIsDisplayed() }.isSuccess
        }
    }

    @After
    fun resetSettings() = runBlocking {
        container.settings.update {
            it.copy(
                notificationCounts = false,
                keyboardWidth = KeyboardFit.WIDTH_MAX,
                keyboardShift = 0f,
                homeListOffset = 0,
                searchColoredIcons = false,
                pressFeedback = PressFeedback.FLASH,
                gestureShiftPenalty = GestureTuning.SHIFT_PENALTY,
                gestureSuggestMargin = GestureTuning.SUGGEST_MARGIN,
                clockPosition = ClockPosition.START,
                font = AppFont.SYSTEM,
                fontWeight = AppFontWeight.REGULAR,
                searchFuzzy = true,
                strokeColor = StrokeColor.ACCENT,
            )
        }
        container.spaces.spaces.value.forEach { container.spaces.setListOffset(it.id, 0) }
        container.history.clearUsage()
        container.notificationCounts.value = emptyMap()
    }

    protected fun backgroundPoint() = rule.onNodeWithTag("home")

    protected fun openSearch() {
        backgroundPoint().performTouchInput { doubleClick(bottomCenter.copy(y = bottom - height * 0.15f)) }
        rule.onNodeWithTag("search").assertIsDisplayed()
    }

    protected fun listOffsetOf(id: Long): Int =
        container.spaces.spaces.value.first { it.id == id }.listOffset ?: 0

    protected fun spaceItems(): List<com.mushinlab.launcher.core.model.AppKey> =
        runBlocking { container.spaces.itemsOf(normalId) }

    protected fun clearQuery() {
        rule.onNodeWithTag("key_BACKSPACE").performTouchInput {
            down(centerRight - Offset(4f, 0f))
            repeat(STEPS) { i -> moveTo(centerRight - Offset(4f + 200f * (i + 1) / STEPS, 0f)) }
            up()
        }
    }

    protected fun drawOnKeyboard(word: String) {
        rule.onNodeWithTag("keyboard").performTouchInput {
            val centers = glideCenters(
                width.toFloat(),
                KeyboardDimens.rowHeight.toPx(),
                KeyboardDimens.verticalPadding.toPx(),
                KeyboardDimens.gap.toPx(),
            )
            val points = word.map { c -> centers.getValue(c).let { Offset(it.x, it.y) } }
            down(points.first())
            points.zipWithNext { from, target ->
                repeat(STEPS) { i -> moveTo(from + (target - from) * ((i + 1f) / STEPS)) }
            }
            up()
        }
    }

    protected fun drawOnHome(word: String, scale: Float) {
        rule.onNodeWithTag("home").performTouchInput {
            val centers = glideCenters(
                width.toFloat(),
                KeyboardDimens.rowHeight.toPx(),
                KeyboardDimens.verticalPadding.toPx(),
                KeyboardDimens.gap.toPx(),
            )
            val raw = word.map { c -> centers.getValue(c).let { Offset(it.x, it.y) } }
            val cx = raw.map { it.x }.average().toFloat()
            val cy = raw.map { it.y }.average().toFloat()
            val origin = Offset(width / 2f, height * 0.7f)
            val points = raw.map { origin + Offset(it.x - cx, it.y - cy) * scale }
            down(points.first())
            points.zipWithNext { from, target ->
                repeat(STEPS) { i -> moveTo(from + (target - from) * ((i + 1f) / STEPS)) }
            }
            up()
        }
    }

    protected companion object {
        const val STEPS = 6
    }
}
