package com.mushinlab.launcher

import android.content.IntentFilter
import android.database.sqlite.SQLiteDatabase
import android.provider.AlarmClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.apps.NotificationSource
import com.mushinlab.launcher.core.data.repo.HistoryRepository
import com.mushinlab.launcher.core.model.ClockPosition
import com.mushinlab.launcher.core.model.ClockSize
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.HomeTextAlign
import com.mushinlab.launcher.core.model.HomeTextSize
import com.mushinlab.launcher.core.model.HostedWidget
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.model.SpaceTransition
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.model.Usefulness
import com.mushinlab.launcher.core.model.WidgetCell
import com.mushinlab.launcher.core.model.WidgetLayout
import com.mushinlab.launcher.core.ui.MushinColors
import com.mushinlab.launcher.feature.home.R as HomeR
import com.mushinlab.launcher.feature.settings.SettingsRoute
import java.time.LocalDate
import java.time.ZoneId
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
    fun AC_06_1_tappingTheBottomBarShowsPopupInsideWindow() {
        rule.onNodeWithTag("home_bar").performTouchInput { click(centerLeft + Offset(20f, 0f)) }
        val popup = rule.onNodeWithTag("popup").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val root = rule.onNodeWithTag("home").fetchSemanticsNode().boundsInRoot
        assertThat(popup.top).isAtLeast(root.top)
        assertThat(popup.bottom).isAtMost(root.bottom)
        rule.onNodeWithTag("popup_settings").assertDoesNotExist()
    }

    @Test
    fun AC_06_1_swipeUpFromTheBottomBarOpensSearch() {
        rule.onNodeWithTag("home_bar").performTouchInput {
            down(Offset(centerX, top + 8.dp.toPx()))
            moveBy(Offset(0f, -400f), 150)
            up()
        }
        rule.onNodeWithTag("search").assertIsDisplayed()
    }

    @Test
    fun AC_49_3_holdingTheBackgroundOpensNothing() {
        val home = rule.onNodeWithTag("home").fetchSemanticsNode().boundsInRoot
        val barTop = rule.onNodeWithTag("home_bar").fetchSemanticsNode().boundsInRoot.top - home.top
        backgroundPoint().performTouchInput { longClick(Offset(centerX, barTop - 8.dp.toPx())) }
        rule.onNodeWithTag("popup").assertDoesNotExist()
        rule.onNodeWithTag("search").assertDoesNotExist()
        assertThat(rule.activity.launcher.navigator.overlay).isEqualTo(Overlay.None)
    }

    @Test
    fun AC_49_3_bottomBarSelectsASpaceAndDoesNotOpenPopupWhileEditing() {
        val work = container.spaces.spaces.value.first { it.id != normalId }
        rule.onNodeWithTag("home_bar").performClick()
        rule.onNodeWithTag("popup_env_${work.id}").performClick()
        rule.waitUntil(5_000) { container.settings.settings.value.currentSpaceId == work.id }
        waitForSpaceName(work.name)
        rule.onNodeWithTag("popup").assertDoesNotExist()
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("home_bar").performTouchInput { click(center) }
        rule.onNodeWithTag("popup").assertDoesNotExist()
    }

    @Test
    fun AC_50_1_swipingTheBottomBarLeftShowsTheNextSpace() {
        val work = container.spaces.spaces.value.first { it.type == SpaceType.WORK }
        swipeBar(-1)
        rule.waitUntil(5_000) { container.settings.settings.value.currentSpaceId == work.id }
        waitForSpaceName(work.name)
    }

    @Test
    fun AC_57_2_switchingToASpaceWithFewerRowsSetsTheListHeightAtOnce() {
        val work = container.spaces.spaces.value.first { it.type == SpaceType.WORK }
        val original = runBlocking { container.spaces.itemsOf(work.id) }
        runBlocking {
            container.spaces.setItems(work.id, spaceItems().take(1))
            container.settings.update { it.copy(spaceTransition = SpaceTransition.NONE) }
        }
        rule.waitForIdle()
        val before = rule.onNodeWithTag("edit_list").fetchSemanticsNode().boundsInRoot.height
        rule.mainClock.autoAdvance = false
        try {
            runBlocking { container.settings.update { it.copy(currentSpaceId = work.id) } }
            var frames = 0
            while (rule.activity.launcher.home.value.spaceId != work.id && frames < SWITCH_FRAMES) {
                Thread.sleep(FRAME_MS)
                rule.mainClock.advanceTimeByFrame()
                frames++
            }
            assertThat(rule.activity.launcher.home.value.spaceId).isEqualTo(work.id)
            repeat(2) { rule.mainClock.advanceTimeByFrame() }
            val early = rule.onNodeWithTag("edit_list").fetchSemanticsNode().boundsInRoot.height
            rule.mainClock.advanceTimeBy(1_000)
            val settled = rule.onNodeWithTag("edit_list").fetchSemanticsNode().boundsInRoot.height
            assertThat(settled).isWithin(1f).of(before / 3)
            assertThat(early).isWithin(1f).of(settled)
        } finally {
            rule.mainClock.autoAdvance = true
            runBlocking { container.spaces.setItems(work.id, original) }
        }
    }

    @Test
    fun AC_50_1_swipingTheBottomBarRightShowsThePreviousSpace() {
        val work = container.spaces.spaces.value.first { it.type == SpaceType.WORK }
        runBlocking { container.settings.update { it.copy(currentSpaceId = work.id) } }
        rule.waitUntil(5_000) {
            runCatching {
                rule.onNodeWithTag("home_space", useUnmergedTree = true).assertTextEquals(work.name)
            }.isSuccess
        }
        swipeBar(1)
        rule.waitUntil(5_000) { container.settings.settings.value.currentSpaceId == normalId }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("space_preview_$normalId").fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun AC_50_3_swipingTheBottomBarWhileEditingDoesNotChangeTheSpace() {
        rule.onNodeWithTag("home_edit").performClick()
        rule.waitUntil(5_000) { rule.activity.launcher.navigator.homeEditing }
        swipeBar(1)
        rule.waitForIdle()
        assertThat(container.settings.settings.value.currentSpaceId).isEqualTo(normalId)
        rule.onNodeWithTag("space_preview_${container.spaces.spaces.value.first { it.type == SpaceType.WORK }.id}")
            .assertDoesNotExist()
    }

    @Test
    fun AC_50_7_swipingTheBottomBarDoesNotRunLeftRightBindings() {
        runBlocking { container.gestures.set(SwipeDirection.LEFT, GestureAction.Search) }
        try {
            swipeBar(-1)
            rule.waitUntil(5_000) { container.settings.settings.value.currentSpaceId != normalId }
            rule.onNodeWithTag("search").assertDoesNotExist()
        } finally {
            runBlocking { container.gestures.set(SwipeDirection.LEFT, GestureAction.None) }
        }
    }

    @Test
    fun AC_54_2_draggingTheBottomBarShowsTheNextSpaceAndShortReleaseStays() {
        val work = container.spaces.spaces.value.first { it.type == SpaceType.WORK }
        rule.onNodeWithTag("home_bar").performTouchInput {
            down(center)
            moveBy(Offset(0f, 1f), 16)
            moveBy(Offset(-width * 0.05f, 0f), 50)
            moveBy(Offset(-width * 0.03f, 0f), 400)
        }
        rule.onNodeWithTag("space_preview_${work.id}").assertExists()
        rule.onNodeWithTag("home_bar").performTouchInput { up() }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("space_preview_${work.id}").fetchSemanticsNodes().isEmpty() }
        assertThat(container.settings.settings.value.currentSpaceId ?: normalId).isEqualTo(normalId)
    }

    @Test
    fun AC_54_2_thePageEdgeStaysUnderTheFingerOnEveryMove() {
        val spaces = container.spaces.spaces.value
        val current = MushinColors.from(spaces.first { it.id == normalId }.palette).background
        val next = MushinColors.from(spaces.first { it.type == SpaceType.WORK }.palette).background
        val bar = rule.onNodeWithTag("home_bar").fetchSemanticsNode().boundsInRoot
        val width = rule.onNodeWithTag("home").fetchSemanticsNode().size.width
        val y = bar.top.toInt() + 3
        var dx = -1f
        rule.onRoot().performTouchInput {
            down(bar.center)
            moveBy(Offset(dx, 0f), 16)
        }
        repeat(8) {
            dx -= 40f
            rule.onRoot().performTouchInput { moveBy(Offset(-40f, 0f), 50) }
            val pixels = rule.onRoot().captureToImage().toPixelMap()
            val edge = (width + dx).toInt()
            assertThat(pixels[edge - 6, y]).isEqualTo(current)
            assertThat(pixels[edge + 6, y]).isEqualTo(next)
        }
        rule.onRoot().performTouchInput { up() }
    }

    @Test
    fun AC_54_3_motionButtonInEditModeChoosesTheSpaceTransition() {
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("home_edit_motion").performClick()
        rule.onNodeWithTag("motion_sheet").assertIsDisplayed()
        rule.onNodeWithTag("motion_space_dropdown").performClick()
        rule.onNodeWithTag("motion_space_NONE").performClick()
        rule.waitUntil(5_000) { container.settings.settings.value.spaceTransition == SpaceTransition.NONE }
    }

    @Test
    fun AC_54_3_withoutAnimationThePopupSwitchesWithoutAPreview() {
        runBlocking { container.settings.update { it.copy(spaceTransition = SpaceTransition.NONE) } }
        val work = container.spaces.spaces.value.first { it.type == SpaceType.WORK }
        rule.onNodeWithTag("home_bar").performClick()
        rule.onNodeWithTag("popup_env_${work.id}").performClick()
        rule.waitUntil(5_000) { container.settings.settings.value.currentSpaceId == work.id }
        rule.onNodeWithTag("space_preview_${work.id}").assertDoesNotExist()
        waitForSpaceName(work.name)
    }

    private fun waitForSpaceName(name: String) {
        rule.waitUntil(5_000) {
            runCatching { rule.onNodeWithTag("home_space", useUnmergedTree = true).assertTextEquals(name) }.isSuccess
        }
    }

    private fun swipeBar(direction: Int) {
        rule.onNodeWithTag("home_bar").performTouchInput {
            down(center)
            moveBy(Offset(direction * width * 0.4f, 0f), 300)
            up()
        }
    }

    @Test
    fun settingsButtonNextToTheEditButtonOpensSettings() {
        val settings = rule.onNodeWithTag("home_settings").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val edit = rule.onNodeWithTag("home_edit").fetchSemanticsNode().boundsInRoot
        assertThat(settings.right).isAtMost(edit.left)
        assertThat(settings.center.y).isWithin(2f).of(edit.center.y)
        rule.onNodeWithTag("home_settings").performTouchInput { click() }
        rule.waitUntil(5_000) { rule.activity.launcher.navigator.settings.top == SettingsRoute.Root }
    }

    @Test
    fun AC_06_3_longPressOnRowShowsAppSection() {
        rule.onNodeWithTag("row_0").performTouchInput { longClick() }
        rule.onNodeWithTag("popup_about").assertIsDisplayed()
        rule.onNodeWithTag("popup_rename").assertIsDisplayed()
        rule.onNodeWithTag("popup_hide").assertDoesNotExist()
        val info = rule.onNodeWithTag("popup_about").fetchSemanticsNode().boundsInRoot
        val rename = rule.onNodeWithTag("popup_rename").fetchSemanticsNode().boundsInRoot
        val remove = rule.onNodeWithTag("popup_remove").fetchSemanticsNode().boundsInRoot
        assertThat(rename.center.y).isWithin(1f).of(info.center.y)
        assertThat(rename.left).isAtLeast(info.right)
        assertThat(remove.left).isAtLeast(rename.right)
    }

    @Test
    fun AC_06_3_removeFromSpaceInTheAppMenuRemovesTheRow() {
        val key = spaceItems().first()
        rule.onNodeWithTag("row_0").performTouchInput { longClick() }
        rule.onNodeWithTag("popup_remove").performClick()
        rule.onNodeWithTag("popup").assertDoesNotExist()
        rule.waitUntil(5_000) { key !in spaceItems() }
    }

    @Test
    fun AC_37_10_draggingARowByItsHandleInEditModeReordersTheList() {
        val (first, second, third) = spaceItems()
        rule.onNodeWithTag("home_edit").performClick()
        val row = rule.onNodeWithTag("row_0").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("row_drag_0").performTouchInput {
            down(center)
            repeat(4) { moveBy(Offset(0f, row.height * 0.55f)) }
            up()
        }
        rule.waitUntil(5_000) { spaceItems() == listOf(second, third, first) }
        assertThat(rule.activity.launcher.navigator.overlay).isEqualTo(Overlay.None)
    }

    @Test
    fun AC_37_1_tappingOrHoldingARowInEditModeDoesNothing() {
        val items = spaceItems()
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("row_0").performClick()
        rule.onNodeWithTag("row_0").performTouchInput { longClick() }
        rule.waitForIdle()
        assertThat(rule.activity.launcher.navigator.overlay).isEqualTo(Overlay.None)
        assertThat(rule.activity.launcher.navigator.settings.isOpen).isFalse()
        assertThat(spaceItems()).isEqualTo(items)
    }

    @Test
    fun AC_03_2_rowHeightFollowsTheTextSize() {
        val height = { rule.onNodeWithTag("row_0").fetchSemanticsNode().boundsInRoot.height }
        val medium = height()
        assertThat(medium).isWithin(with(rule.density) { 4.dp.toPx() }).of(with(rule.density) { 64.dp.toPx() })
        runBlocking { container.settings.update { it.copy(homeTextSize = HomeTextSize.S) } }
        rule.waitUntil(5_000) { height() < medium }
        runBlocking { container.settings.update { it.copy(homeTextSize = HomeTextSize.L) } }
        rule.waitUntil(5_000) { height() > medium }
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
    fun clockEditButtonAppearsOnlyInEditModeAndOpensTheClockSheet() {
        val edit = "tile_edit_${com.mushinlab.launcher.core.model.HostedWidget.CLOCK_ID}"
        rule.onNodeWithTag(edit).assertDoesNotExist()
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag(edit).assertIsDisplayed().performClick()
        rule.waitUntil(5_000) { rule.activity.launcher.navigator.settings.top == SettingsRoute.ClockSheet }
        rule.onNodeWithTag("clock_sheet").assertIsDisplayed()
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
    fun AC_59_2_theListMovesUpAndDownWithTheWidgetRows() {
        rule.onNodeWithTag("list_handle").assertDoesNotExist()
        val panel = rule.onNodeWithTag("widget_panel_TOP").fetchSemanticsNode().boundsInRoot
        val before = rule.onNodeWithTag("row_0").fetchSemanticsNode().boundsInRoot.top
        val original = container.widgets.loaded.value
        val shortClock = HostedWidget.clock.copy(cell = WidgetCell(0, 0, 4, 2))
        try {
            runBlocking {
                container.widgets.update {
                    it.copy(layouts = it.layouts + (normalId to WidgetLayout(listOf(shortClock))))
                }
            }
            rule.waitUntil(5_000) {
                rule.onNodeWithTag("widget_panel_TOP").fetchSemanticsNode().boundsInRoot.height < panel.height - 1f
            }
            rule.waitForIdle()
            val shorter = rule.onNodeWithTag("widget_panel_TOP").fetchSemanticsNode().boundsInRoot
            val after = rule.onNodeWithTag("row_0").fetchSemanticsNode().boundsInRoot.top
            assertThat(before - after).isWithin(2f).of(panel.height - shorter.height)
            assertThat(after - shorter.bottom).isWithin(2f).of(before - panel.bottom)
        } finally {
            runBlocking { container.widgets.update { original ?: it } }
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
    fun inEditModeALongListScrollsSoEveryRowCanBeEdited() {
        val apps = runBlocking { container.catalog.apps.first { it.size >= 8 } }.sortedBy { it.label }.take(8)
        runBlocking { container.spaces.setItems(normalId, apps.map { it.key }) }
        rule.waitUntil(5_000) { rule.activity.launcher.home.value.rows.size == 8 }
        rule.onNodeWithTag("home_edit").performClick()
        val list = rule.onNodeWithTag("edit_list").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val row = rule.onNodeWithTag("row_0").fetchSemanticsNode().boundsInRoot
        assertThat(list.height).isAtMost(row.height * 5.5f + 2f)
        assertThat(list.height).isAtLeast(row.height * 2)
        rule.onNodeWithTag("row_7").assertIsNotDisplayed()
        rule.onNodeWithTag("edit_list").performTouchInput { swipeUp() }
        rule.onNodeWithTag("row_7").assertIsDisplayed()
        pullRowOpen(7)
        rule.onNodeWithTag("row_remove_7").assertIsDisplayed().performClick()
        rule.waitUntil(5_000) { apps[7].key !in spaceItems() }
    }

    @Test
    fun clockSizeChangesTheDigitsAndRemovingTheClockLeavesTheAddCell() {
        fun clockHeight() = rule.onNodeWithTag("clock").fetchSemanticsNode().boundsInRoot.height
        runBlocking { container.settings.update { it.copy(clockSize = ClockSize.S) } }
        rule.waitUntil(5_000) { rule.activity.launcher.home.value.clockSize == ClockSize.S }
        rule.waitForIdle()
        val small = clockHeight()
        runBlocking { container.settings.update { it.copy(clockSize = ClockSize.L) } }
        rule.waitUntil(5_000) { rule.activity.launcher.home.value.clockSize == ClockSize.L }
        rule.waitForIdle()
        assertThat(clockHeight()).isGreaterThan(small)
        runBlocking { container.settings.update { it.copy(clockSize = ClockSize.M) } }
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("tile_remove_${com.mushinlab.launcher.core.model.HostedWidget.CLOCK_ID}").performClick()
        rule.waitUntil(5_000) { rule.activity.launcher.home.value.widgetPanels.top.widgets.isEmpty() }
        rule.onNodeWithTag("clock").assertDoesNotExist()
        rule.onNodeWithTag("add_widget").assertIsDisplayed()
        runBlocking { container.widgets.update { com.mushinlab.launcher.core.model.WidgetState() } }
        rule.waitUntil(5_000) { rule.activity.launcher.home.value.widgetPanels.top.widgets.isNotEmpty() }
    }

    @Test
    fun AC_37_2_editTipsOpenOverHomeAndBackKeepsEditMode() {
        rule.onNodeWithTag("home_edit_tips").assertDoesNotExist()
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("home_edit_tips").assertIsDisplayed().performClick()
        rule.onNodeWithTag("edit_tips_sheet").assertIsDisplayed()
        rule.onNodeWithText(
            rule.activity.getString(com.mushinlab.launcher.feature.settings.R.string.edit_tip_list_title),
        )
            .assertIsDisplayed()
        rule.runOnIdle { rule.activity.launcher.navigator.handleBack() }
        rule.onNodeWithTag("edit_tips_sheet").assertDoesNotExist()
        assertThat(rule.activity.launcher.navigator.homeEditing).isTrue()
    }

    @Test
    fun AC_37_11_swipingARowLeftRevealsRemoveAndATapOnItRemovesTheRow() {
        val first = spaceItems().first()
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("row_edit_0").assertDoesNotExist()
        rule.onNodeWithTag("row_remove_0").assertDoesNotExist()
        pullRowOpen(0)
        assertThat(first).isIn(spaceItems())
        rule.onNodeWithTag("row_remove_0").assertIsDisplayed().performClick()
        rule.waitUntil(5_000) { first !in spaceItems() }
    }

    @Test
    fun AC_57_3_enteringEditModeHintsRemovalOnTheFirstRowOnly() {
        rule.mainClock.autoAdvance = false
        try {
            rule.onNodeWithTag("home_edit").performClick()
            rule.mainClock.advanceTimeBy(HINT_OUT_MS)
            rule.onNodeWithTag("row_remove_0").assertExists()
            rule.onNodeWithTag("row_remove_1").assertDoesNotExist()
            rule.mainClock.advanceTimeBy(HINT_DONE_MS)
            rule.onNodeWithTag("row_remove_0").assertDoesNotExist()
        } finally {
            rule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun AC_37_11_aShortSwipeClosesAgainAndPullingAnotherRowClosesTheOpenOne() {
        val items = spaceItems()
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("row_0").performTouchInput {
            down(Offset(width * 0.7f, centerY))
            moveBy(Offset(-viewConfiguration.touchSlop - 4f, 0f), 50)
            moveBy(Offset(-20.dp.toPx(), 0f), 100)
            up()
        }
        rule.onNodeWithTag("row_remove_0").assertDoesNotExist()
        pullRowOpen(0)
        rule.onNodeWithTag("row_remove_0").assertIsDisplayed()
        pullRowOpen(1)
        rule.onNodeWithTag("row_remove_0").assertDoesNotExist()
        rule.onNodeWithTag("row_remove_1").assertIsDisplayed()
        assertThat(spaceItems()).containsExactlyElementsIn(items).inOrder()
    }

    private fun pullRowOpen(index: Int) {
        rule.onNodeWithTag("row_$index").performTouchInput {
            down(Offset(width * 0.7f, centerY))
            moveBy(Offset(-viewConfiguration.touchSlop - 4f, 0f), 50)
            moveBy(Offset(-width * 0.25f, 0f), 100)
            moveBy(Offset(-width * 0.25f, 0f), 100)
            up()
        }
        rule.waitForIdle()
    }

    @Test
    fun AC_37_1_folderRowInEditModeOffersItsEditorAndRemoval() {
        val originalApps = spaceItems()
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("home_add_folder").performClick()
        rule.waitUntil(5_000) { rule.activity.launcher.navigator.settings.top is SettingsRoute.HomeFolder }
        val id = (rule.activity.launcher.navigator.settings.top as SettingsRoute.HomeFolder).folderId
        rule.onNodeWithTag("folder_name").performTextInput("Folder")
        rule.waitUntil(5_000) { container.folders.folders.value[id]?.name == "Folder" }
        rule.runOnIdle { rule.activity.launcher.navigator.settings.close() }
        rule.onNodeWithTag("row_edit_${originalApps.size}").performClick()
        rule.waitUntil(5_000) { rule.activity.launcher.navigator.settings.top == SettingsRoute.HomeFolder(id) }
        rule.runOnIdle { rule.activity.launcher.navigator.settings.close() }
        pullRowOpen(originalApps.size)
        rule.onNodeWithTag("row_remove_${originalApps.size}").performClick()
        rule.waitUntil(5_000) { id !in container.folders.folders.value }
        assertThat(spaceItems()).containsExactlyElementsIn(originalApps).inOrder()
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
    fun iconsFollowTheLabelAndCentredLabelWithIconSitsOnTheScreenCentre() {
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
            assertThat((name().left + icon().right) / 2).isWithin(1f).of(row.center.x)
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
        val add = rule.onNodeWithTag("home_add").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertThat(add.height).isAtLeast(with(rule.density) { 48.dp.toPx() })
        rule.onNodeWithTag("home_add_app").performClick()
        rule.waitUntil(5_000) {
            rule.activity.launcher.navigator.settings.top == SettingsRoute.PickApps(normalId)
        }
    }
}

private const val SWITCH_FRAMES = 300
private const val FRAME_MS = 16L
private const val HINT_OUT_MS = 800L
private const val HINT_DONE_MS = 1_500L
