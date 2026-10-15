package com.mushinlab.launcher

import android.content.IntentFilter
import android.provider.AlarmClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.apps.NotificationSource
import com.mushinlab.launcher.core.model.ClockPosition
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.model.Usefulness
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeUiTest : LauncherUiTestBase() {
    @Test
    fun AC_03_2_rowsAreAtLeast48dpTall() {
        (0..2).forEach { rule.onNodeWithTag("row_$it").assertHeightIsAtLeast(48.dp) }
    }

    @Test
    fun AC_06_1_longPressOnBackgroundShowsPopupInsideWindow() {
        backgroundPoint().performTouchInput { longClick(bottomCenter.copy(y = bottom - height * 0.1f)) }
        val popup = rule.onNodeWithTag("popup").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val root = rule.onNodeWithTag("home").fetchSemanticsNode().boundsInRoot
        assertThat(popup.top).isAtLeast(root.top)
        assertThat(popup.bottom).isAtMost(root.bottom)
        rule.onNodeWithTag("popup_settings").assertIsDisplayed()
    }

    @Test
    fun AC_06_3_longPressOnRowShowsAppSection() {
        rule.onNodeWithTag("row_0").performTouchInput { longClick() }
        rule.onNodeWithTag("popup_about").assertIsDisplayed()
        rule.onNodeWithTag("popup_rename").assertIsDisplayed()
        rule.onNodeWithTag("popup_hide").assertIsDisplayed()
        rule.onNodeWithTag("popup_remove").assertIsDisplayed()
    }

    @Test
    fun notificationCountShowsNextToRowLabelWhenEnabled() {
        val key = spaceItems().first()
        container.notificationCounts.value = mapOf(NotificationSource(key.packageName, key.userSerial) to 3)
        rule.onNodeWithTag("badge_0", useUnmergedTree = true).assertDoesNotExist()
        runBlocking { container.settings.update { it.copy(notificationCounts = true) } }
        rule.waitUntil(5_000) {
            runCatching {
                rule.onNodeWithTag("badge_0", useUnmergedTree = true).onChildAt(0).assertTextEquals("3")
            }.isSuccess
        }
    }

    @Test
    fun appBoundToASwipeShowsTheArrowNextToItsName() {
        val key = spaceItems().first()
        runBlocking { container.gestures.set(SwipeDirection.LEFT, GestureAction.LaunchApp(key)) }
        try {
            rule.waitUntil(5_000) {
                runCatching { rule.onNodeWithTag("swipe_0", useUnmergedTree = true).assertExists() }.isSuccess
            }
            val icon = rule.onNodeWithTag("swipe_0", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val size = container.settings.settings.value.homeTextSize.sp
            with(rule.density) { assertThat(icon.height).isWithin(2f).of(size.sp.toPx() * 0.82f) }
        } finally {
            runBlocking { container.gestures.set(SwipeDirection.LEFT, GestureAction.None) }
        }
    }

    @Test
    fun tappingTheClockOpensTheClockApp() {
        val monitor = InstrumentationRegistry.getInstrumentation().addMonitor(
            IntentFilter(AlarmClock.ACTION_SHOW_ALARMS),
            null,
            true,
        )
        try {
            rule.onNodeWithTag("clock_block").performClick()
            rule.waitUntil(5_000) { monitor.hits > 0 }
            assertThat(rule.activity.launcher.navigator.overlay).isEqualTo(Overlay.None)
        } finally {
            InstrumentationRegistry.getInstrumentation().removeMonitor(monitor)
        }
    }

    @Test
    fun holdingTheClockShowsThreeSlotsAndDropsItIntoTheNearest() {
        rule.onNodeWithTag("clock_block").performTouchInput {
            down(center)
            advanceEventTime(900)
            moveBy(Offset(1f, 0f))
        }
        rule.onNodeWithTag("clock_slot_START").assertExists()
        rule.onNodeWithTag("clock_slot_CENTER").assertExists()
        rule.onNodeWithTag("clock_slot_END").assertExists()
        val width = rule.onNodeWithTag("home").fetchSemanticsNode().boundsInRoot.width
        rule.onNodeWithTag("clock_block").performTouchInput {
            moveBy(Offset(width, 0f))
            up()
        }
        rule.waitUntil(5_000) { container.settings.settings.value.clockPosition == ClockPosition.END }
        rule.onNodeWithTag("clock_slot_END").assertDoesNotExist()
    }

    @Test
    fun frequentlyUsedAppIsSuggestedInsteadOfTheWeakestHomeApp() {
        val home = spaceItems()
        val frequent = runBlocking { container.catalog.apps.first() }.first { it.key !in home }.key
        runBlocking {
            container.history.clearUsage()
            repeat(Usefulness.MIN_LAUNCHES) { container.history.recordLaunch(frequent, normalId, fromSearch = false) }
        }
        rule.waitUntil(5_000) {
            runCatching { rule.onNodeWithTag("home_suggestion").assertIsDisplayed() }.isSuccess
        }
        rule.onNodeWithTag("home_suggestion").performClick()
        rule.onNodeWithTag("suggest_replace").performClick()
        rule.waitUntil(5_000) { spaceItems() == home.dropLast(1) + frequent }
        rule.waitUntil(5_000) {
            runCatching { rule.onNodeWithTag("home_suggestion").assertDoesNotExist() }.isSuccess
        }
    }

    @Test
    fun holdingTheHandleAndDraggingMovesTheListTogetherWithTheHandle() {
        val before = rule.onNodeWithTag("row_0").fetchSemanticsNode().boundsInRoot.top
        rule.onNodeWithTag("list_handle").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            moveBy(Offset(0f, 1f))
        }
        rule.onNodeWithTag("list_handle").performTouchInput {
            moveBy(Offset(0f, 200f))
            up()
        }
        rule.waitUntil(5_000) { listOffsetOf(normalId) > 0 }
        val after = rule.onNodeWithTag("row_0").fetchSemanticsNode().boundsInRoot.top
        assertThat(after - before).isWithin(4f).of(200f)
        val handle = rule.onNodeWithTag("list_handle").fetchSemanticsNode().boundsInRoot
        assertThat(after - handle.bottom).isWithin(2f).of(0f)
    }
}
