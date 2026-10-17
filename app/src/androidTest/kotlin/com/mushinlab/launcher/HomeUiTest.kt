package com.mushinlab.launcher

import android.content.IntentFilter
import android.provider.AlarmClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onFirst
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
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.model.Usefulness
import com.mushinlab.launcher.feature.home.R as HomeR
import com.mushinlab.launcher.feature.settings.SettingsRoute
import kotlin.math.abs
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
        rule.waitUntil(5_000) { (listOffsetOf(normalId) ?: 0) > 0 }
        val after = rule.onNodeWithTag("row_0").fetchSemanticsNode().boundsInRoot.top
        assertThat(after - before).isWithin(4f).of(200f)
        val handle = rule.onNodeWithTag("list_handle").fetchSemanticsNode().boundsInRoot
        assertThat(after - handle.bottom).isWithin(2f).of(0f)
    }

    @Test
    fun listStartsLowerByDefaultAndDraggingTheHandleSavesAnOffset() {
        val handle = { rule.onNodeWithTag("list_handle").fetchSemanticsNode().boundsInRoot }
        val natural = handle().top
        runBlocking { container.settings.update { it.copy(homeListOffset = null) } }
        rule.waitUntil(5_000) { rule.activity.launcher.home.value.homeListOffset == null }
        rule.waitForIdle()
        val auto = handle()
        val screen = rule.onNodeWithTag("home").fetchSemanticsNode().boundsInRoot
        val gap = with(rule.density) { 32.dp.toPx() }
        assertThat(auto.top - natural).isWithin(2f).of(gap * 2)
        assertThat(auto.top).isLessThan(screen.height * 0.5f)
        val rowHeight = rule.onNodeWithTag("row_0").fetchSemanticsNode().boundsInRoot.height
        val bar = rule.onNodeWithTag("home_edit").fetchSemanticsNode().boundsInRoot
        assertThat(bar.top - auto.bottom).isAtLeast(rowHeight * 4 - 1f)
        rule.onNodeWithTag("list_handle").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            moveBy(Offset(0f, -1f))
        }
        rule.onNodeWithTag("list_handle").performTouchInput {
            moveBy(Offset(0f, -150f))
            up()
        }
        rule.waitUntil(5_000) { listOffsetOf(normalId) != null }
        val saved = listOffsetOf(normalId)!!
        with(rule.density) {
            assertThat(saved.dp.toPx()).isWithin(6f).of(auto.top - natural - 150f)
            rule.waitUntil(5_000) { abs(handle().top - (natural + saved.dp.toPx())) < 3f }
        }
    }

    @Test
    fun addAppsRowUnderTheListOpensTheAppPickerOfTheCurrentSpace() {
        val lastRow = rule.onNodeWithTag("row_2").fetchSemanticsNode().boundsInRoot
        val add = rule.onNodeWithTag("home_add").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertThat(add.top).isWithin(1f).of(lastRow.bottom)
        assertThat(add.height).isWithin(1f).of(lastRow.height)
        assertThat(add.left).isWithin(1f).of(lastRow.left)
        assertThat(add.width).isWithin(1f).of(lastRow.width)
        rule.onNodeWithTag("home_add").assertTextEquals(rule.activity.getString(HomeR.string.home_add_apps))
        rule.onNodeWithTag("home_add").performClick()
        rule.waitUntil(5_000) {
            rule.activity.launcher.navigator.settings.top == SettingsRoute.PickApps(normalId)
        }
        assertThat(rule.activity.launcher.navigator.overlay).isEqualTo(Overlay.None)
    }

    @Test
    fun addAppsLabelLinesUpWithAppNamesWhenIconsAreShown() {
        val label = rule.onNodeWithTag("row_0").fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription]
            .first()
        val nameLeft = {
            rule.onAllNodes(hasText(label) and hasAnyAncestor(hasTestTag("row_0")), useUnmergedTree = true)
                .onFirst()
                .fetchSemanticsNode()
                .boundsInRoot
                .left
        }
        val addLeft = {
            rule.onNodeWithTag("home_add_label", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.left
        }
        try {
            listOf(IconMode.LEFT, IconMode.RIGHT).forEach { mode ->
                runBlocking { container.settings.update { it.copy(iconMode = mode) } }
                rule.waitUntil(5_000) { rule.activity.launcher.home.value.iconMode == mode }
                rule.waitForIdle()
                assertThat(addLeft()).isWithin(1f).of(nameLeft())
            }
            val row = rule.onNodeWithTag("row_0").fetchSemanticsNode().boundsInRoot
            assertThat(addLeft() - row.left).isLessThan(with(rule.density) { 40.dp.toPx() })
        } finally {
            runBlocking { container.settings.update { it.copy(iconMode = IconMode.NONE) } }
        }
    }

    @Test
    fun emptySpaceShowsOnlyTheAddAppsRowThatOpensTheAppPicker() {
        runBlocking { container.spaces.setItems(normalId, emptyList()) }
        rule.waitUntil(5_000) {
            runCatching { rule.onNodeWithTag("row_0").assertDoesNotExist() }.isSuccess
        }
        rule.onNodeWithTag("empty_env").assertDoesNotExist()
        val handle = rule.onNodeWithTag("list_handle").fetchSemanticsNode().boundsInRoot
        val add = rule.onNodeWithTag("home_add").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertThat(add.top).isWithin(1f).of(handle.bottom)
        assertThat(add.height).isAtLeast(with(rule.density) { 48.dp.toPx() })
        rule.onNodeWithTag("home_add").performClick()
        rule.waitUntil(5_000) {
            rule.activity.launcher.navigator.settings.top == SettingsRoute.PickApps(normalId)
        }
    }
}
