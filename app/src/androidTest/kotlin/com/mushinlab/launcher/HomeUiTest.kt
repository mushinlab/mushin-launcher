package com.mushinlab.launcher

import android.content.IntentFilter
import android.database.sqlite.SQLiteDatabase
import android.provider.AlarmClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.apps.NotificationSource
import com.mushinlab.launcher.core.data.repo.HistoryRepository
import com.mushinlab.launcher.core.model.ClockPosition
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.HomeTextAlign
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.model.Usefulness
import com.mushinlab.launcher.feature.home.R as HomeR
import com.mushinlab.launcher.feature.settings.SettingsRoute
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
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
        rule.onNodeWithTag("popup_settings").assertDoesNotExist()
    }

    @Test
    fun settingsButtonNextToTheEditButtonOpensSettings() {
        val settings = rule.onNodeWithTag("home_settings").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val edit = rule.onNodeWithTag("home_edit").fetchSemanticsNode().boundsInRoot
        assertThat(settings.right).isAtMost(edit.left)
        assertThat(settings.center.y).isWithin(2f).of(edit.center.y)
        rule.onNodeWithTag("home_settings").performClick()
        rule.waitUntil(5_000) { rule.activity.launcher.navigator.settings.top == SettingsRoute.Root }
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
    fun editModeShowsTheClockSlotsAndDraggingDropsItIntoTheNearest() {
        rule.onNodeWithTag("clock_slot_END").assertDoesNotExist()
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("clock_slot_START").assertExists()
        rule.onNodeWithTag("clock_slot_CENTER").assertExists()
        rule.onNodeWithTag("clock_slot_END").assertExists()
        val width = rule.onNodeWithTag("home").fetchSemanticsNode().boundsInRoot.width
        rule.onNodeWithTag("clock_block").performTouchInput {
            down(center)
            moveBy(Offset(width, 0f))
            up()
        }
        rule.waitUntil(5_000) { container.settings.settings.value.clockPosition == ClockPosition.END }
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("clock_slot_END").assertDoesNotExist()
    }

    @Test
    fun clockEditButtonAppearsOnlyInEditModeAndOpensTheClockSheet() {
        rule.onNodeWithTag("clock_edit").assertDoesNotExist()
        rule.onNodeWithTag("home_edit").performClick()
        val clock = rule.onNodeWithTag("clock_block").fetchSemanticsNode().boundsInRoot
        val button = rule.onNodeWithTag("clock_edit").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertThat(button.left).isAtLeast(clock.right)
        rule.onNodeWithTag("clock_edit").performClick()
        rule.waitUntil(5_000) { rule.activity.launcher.navigator.settings.top == SettingsRoute.ClockSheet }
        rule.onNodeWithTag("clock_sheet").assertIsDisplayed()
        assertThat(container.settings.settings.value.clockPosition).isEqualTo(ClockPosition.START)
    }

    @Test
    fun outsideEditModeHoldingTheClockOpensThePopupAndKeepsItInPlace() {
        val width = rule.onNodeWithTag("home").fetchSemanticsNode().boundsInRoot.width
        rule.onNodeWithTag("clock_block").performTouchInput {
            down(center)
            advanceEventTime(900)
            moveBy(Offset(width, 0f))
            up()
        }
        rule.waitUntil(5_000) { rule.activity.launcher.navigator.overlay is Overlay.Popup }
        assertThat(container.settings.settings.value.clockPosition).isEqualTo(ClockPosition.START)
        rule.onNodeWithTag("clock_slot_END").assertDoesNotExist()
    }

    @Test
    fun frequentlyUsedAppIsSuggestedInsteadOfTheWeakestHomeApp() {
        val home = spaceItems()
        val frequent = runBlocking { container.catalog.apps.first() }.first { it.key !in home }.key
        runBlocking {
            container.history.clearUsage()
            SQLiteDatabase.openDatabase(
                rule.activity.getDatabasePath("env.db").path,
                null,
                SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING,
            ).use { it.execSQL("UPDATE environment SET created_at = 0 WHERE id = $normalId") }
            val past = CoroutineScope(Job())
            HistoryRepository(container.database, past, clock = { 0L }).recordHint(normalId)
            (Usefulness.MIN_LAUNCH_DAYS - 1 downTo 1).forEach { daysAgo ->
                val noon = LocalDate.now().minusDays(daysAgo.toLong()).atTime(12, 0).atZone(ZoneId.systemDefault())
                HistoryRepository(container.database, past, clock = { noon.toInstant().toEpochMilli() })
                    .recordLaunch(frequent, normalId, fromSearch = false)
            }
            past.cancel()
            container.history.recordLaunch(frequent, normalId, fromSearch = false)
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
    fun inEditModeDraggingTheHandleMovesTheListTogetherWithTheHandle() {
        val before = rule.onNodeWithTag("row_0").fetchSemanticsNode().boundsInRoot.top
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("list_handle").performTouchInput {
            down(center)
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
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("list_handle").performTouchInput {
            down(center)
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
    fun addRowUnderTheListShowsOnlyInEditModeAndOpensTheAppPicker() {
        rule.onNodeWithTag("home_add").assertDoesNotExist()
        rule.onNodeWithTag("home_edit").performClick()
        val lastRow = rule.onNodeWithTag("row_2").fetchSemanticsNode().boundsInRoot
        val add = rule.onNodeWithTag("home_add").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertThat(add.top).isWithin(1f).of(lastRow.bottom)
        assertThat(add.height).isWithin(1f).of(lastRow.height)
        assertThat(add.width).isWithin(1f).of(lastRow.width)
        val folder = rule.onNodeWithTag("home_add_folder").fetchSemanticsNode().boundsInRoot
        val app = rule.onNodeWithTag("home_add_app").fetchSemanticsNode().boundsInRoot
        assertThat(folder.right).isAtMost(app.left + 1f)
        assertThat(folder.center.y).isWithin(1f).of(app.center.y)
        rule.onNodeWithTag("home_add_folder").assertTextEquals(rule.activity.getString(HomeR.string.home_add_folder))
        rule.onNodeWithTag("home_add_app").performClick()
        rule.waitUntil(5_000) {
            rule.activity.launcher.navigator.settings.top == SettingsRoute.PickApps(normalId)
        }
        assertThat(rule.activity.launcher.navigator.overlay).isEqualTo(Overlay.None)
    }

    @Test
    fun editModeRowsCanBeReplacedAndRemoved() {
        val first = spaceItems().first()
        rule.onNodeWithTag("row_edit_0", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("row_edit_0", useUnmergedTree = true).performClick()
        rule.waitUntil(5_000) {
            rule.activity.launcher.navigator.settings.top == SettingsRoute.ReplaceApp(normalId, first)
        }
        rule.runOnIdle { rule.activity.launcher.navigator.settings.close() }
        rule.onNodeWithTag("row_remove_0", useUnmergedTree = true).performClick()
        rule.waitUntil(5_000) { first !in spaceItems() }
    }

    @Test
    fun newFolderOpensItsSheetWithTheNameFocusedAndOpensUnderTheFinger() {
        val apps = spaceItems()
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("home_add_folder").performClick()
        rule.waitUntil(5_000) { rule.activity.launcher.navigator.settings.top is SettingsRoute.HomeFolder }
        val id = (rule.activity.launcher.navigator.settings.top as SettingsRoute.HomeFolder).folderId
        rule.onNodeWithTag("folder_name").assertIsFocused().performTextInput("Banks")
        runBlocking { container.folders.setApps(id, apps.take(2)) }
        rule.waitUntil(5_000) { container.folders.folders.value[id]?.name == "Banks" }
        rule.runOnIdle { rule.activity.launcher.navigator.settings.close() }
        rule.onNodeWithTag("home_edit").performClick()
        val row = "row_${apps.size}"
        rule.onNodeWithTag(row).assertContentDescriptionEquals("Banks")
        val tap = rule.onNodeWithTag(row).fetchSemanticsNode().boundsInRoot.center
        rule.onNodeWithTag(row).performClick()
        val item = rule.onNodeWithTag("folder_item_0").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertThat(item.center.y).isWithin(2f).of(tap.y)
        rule.onNodeWithTag("folder_item_1").assertIsDisplayed()
    }

    @Test
    fun anUntouchedNewFolderIsDroppedWhenItsSheetCloses() {
        val apps = spaceItems()
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("home_add_folder").performClick()
        rule.waitUntil(5_000) { rule.activity.launcher.navigator.settings.top is SettingsRoute.HomeFolder }
        val id = (rule.activity.launcher.navigator.settings.top as SettingsRoute.HomeFolder).folderId
        rule.runOnIdle { rule.activity.launcher.navigator.handleBack() }
        rule.waitUntil(5_000) { spaceItems() == apps && id !in container.folders.folders.value }
    }

    @Test
    fun iconsFollowTheLabelAndCentredLabelsSitOnTheScreenCentre() {
        val label = rule.onNodeWithTag("row_0").fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription]
            .first()
        val name = {
            rule.onAllNodes(hasText(label) and hasAnyAncestor(hasTestTag("row_0")), useUnmergedTree = true)
                .onFirst()
                .fetchSemanticsNode()
                .boundsInRoot
        }
        val icon = { rule.onNodeWithTag("icon_0", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot }
        val padding = with(rule.density) { 24.dp.toPx() }
        val gap = with(rule.density) { 16.dp.toPx() }
        fun show(align: HomeTextAlign) {
            runBlocking { container.settings.update { it.copy(iconMode = IconMode.RIGHT, homeTextAlign = align) } }
            rule.waitUntil(5_000) {
                rule.activity.launcher.home.value.let { it.iconMode == IconMode.RIGHT && it.textAlign == align }
            }
            rule.waitForIdle()
        }
        try {
            val row = rule.onNodeWithTag("row_0").fetchSemanticsNode().boundsInRoot
            show(HomeTextAlign.START)
            assertThat(name().left - row.left).isWithin(1f).of(padding)
            assertThat(icon().left - name().right).isWithin(1f).of(gap)
            show(HomeTextAlign.CENTER)
            assertThat(name().center.x).isWithin(1f).of(row.center.x)
            assertThat(icon().left - name().right).isWithin(1f).of(gap)
            show(HomeTextAlign.END)
            assertThat(row.right - icon().right).isWithin(1f).of(padding)
        } finally {
            runBlocking {
                container.settings.update { it.copy(iconMode = IconMode.NONE, homeTextAlign = HomeTextAlign.START) }
            }
        }
    }

    @Test
    fun emptySpaceInEditModeShowsOnlyTheAddRowThatOpensTheAppPicker() {
        runBlocking { container.spaces.setItems(normalId, emptyList()) }
        rule.waitUntil(5_000) {
            runCatching { rule.onNodeWithTag("row_0").assertDoesNotExist() }.isSuccess
        }
        rule.onNodeWithTag("empty_env").assertDoesNotExist()
        rule.onNodeWithTag("home_edit").performClick()
        val handle = rule.onNodeWithTag("list_handle").fetchSemanticsNode().boundsInRoot
        val add = rule.onNodeWithTag("home_add").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertThat(add.top).isWithin(1f).of(handle.bottom)
        assertThat(add.height).isAtLeast(with(rule.density) { 48.dp.toPx() })
        rule.onNodeWithTag("home_add_app").performClick()
        rule.waitUntil(5_000) {
            rule.activity.launcher.navigator.settings.top == SettingsRoute.PickApps(normalId)
        }
    }
}
