package com.mushinlab.launcher

import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.provider.AlarmClock
import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.apps.NotificationSource
import com.mushinlab.launcher.core.model.AppFont
import com.mushinlab.launcher.core.model.AppFontWeight
import com.mushinlab.launcher.core.model.BackgroundOpacity
import com.mushinlab.launcher.core.model.ClockPosition
import com.mushinlab.launcher.core.model.EnvType
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.GestureTuning
import com.mushinlab.launcher.core.model.KeyboardFit
import com.mushinlab.launcher.core.model.LearnedGesture
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.PressFeedback
import com.mushinlab.launcher.core.model.StrokeColor
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.model.Usefulness
import com.mushinlab.launcher.core.search.KeyboardLayouts
import com.mushinlab.launcher.feature.search.KeyboardDimens
import com.mushinlab.launcher.feature.search.glideCenters
import com.mushinlab.launcher.feature.settings.SettingsRoute
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherUiTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val container get() = ApplicationProvider.getApplicationContext<EnvApp>().container
    private var normalId = 0L

    @Before
    fun seedFavorites() = runBlocking {
        val normal = container.environments.environments.first { it.isNotEmpty() }.first { it.type == EnvType.NORMAL }
        normalId = normal.id
        container.settings.update { it.copy(currentEnvId = normal.id) }
        val apps = container.catalog.apps.first { it.size >= 3 }.sortedBy { it.label }.take(3)
        container.environments.setItems(normal.id, apps.map { it.key })
        rule.waitUntil(5_000) { rule.activity.overlay == Overlay.None }
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
        container.environments.environments.value.forEach { container.environments.setListOffset(it.id, 0) }
        container.history.clearUsage()
        container.notificationCounts.value = emptyMap()
    }

    private fun backgroundPoint() = rule.onNodeWithTag("home")

    private fun openSearch() {
        backgroundPoint().performTouchInput { doubleClick(bottomCenter.copy(y = bottom - height * 0.15f)) }
        rule.onNodeWithTag("search").assertIsDisplayed()
    }

    private fun listOffsetOf(id: Long): Int =
        container.environments.environments.value.first { it.id == id }.listOffset ?: 0

    private fun envItems(): List<com.mushinlab.launcher.core.model.AppKey> =
        runBlocking { container.environments.itemsOf(normalId) }

    @Test
    fun AC_03_2_rowsAreAtLeast48dpTall() {
        (0..2).forEach { rule.onNodeWithTag("row_$it").assertHeightIsAtLeast(48.dp) }
    }

    @Test
    fun AC_07_1_doubleTapOpensKeyboardWithoutIme() {
        backgroundPoint().performTouchInput { doubleClick(bottomCenter.copy(y = bottom - height * 0.15f)) }
        rule.onNodeWithTag("keyboard").assertIsDisplayed()
        val insets = ViewCompat.getRootWindowInsets(rule.activity.window.decorView)
        assertThat(insets?.isVisible(WindowInsetsCompat.Type.ime())).isFalse()
    }

    @Test
    fun AC_08_typingOnLauncherKeyboardUpdatesQuery() {
        backgroundPoint().performTouchInput { doubleClick(bottomCenter.copy(y = bottom - height * 0.15f)) }
        rule.onNodeWithTag("key_c").performClick()
        rule.onNodeWithTag("key_a").performClick()
        rule.onNodeWithTag("query", useUnmergedTree = true).assertTextEquals("ca")
        rule.onNodeWithTag("key_BACKSPACE").performClick()
        rule.onNodeWithTag("query", useUnmergedTree = true).assertTextEquals("c")
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
    fun AC_13_3_backClosesPopupThenSearch() {
        backgroundPoint().performTouchInput { longClick(bottomCenter.copy(y = bottom - height * 0.1f)) }
        rule.onNodeWithTag("popup").assertIsDisplayed()
        Espresso.pressBack()
        rule.onNodeWithTag("popup").assertDoesNotExist()
        backgroundPoint().performTouchInput { doubleClick(bottomCenter.copy(y = bottom - height * 0.15f)) }
        rule.onNodeWithTag("keyboard").assertIsDisplayed()
        Espresso.pressBack()
        rule.onNodeWithTag("keyboard").assertDoesNotExist()
        rule.onNodeWithTag("home").assertIsDisplayed()
    }

    @Test
    fun AC_12_1_swipeUpOpensSearch() {
        backgroundPoint().performTouchInput {
            swipeUp(startY = bottom - height * 0.2f, endY = bottom - height * 0.6f, durationMillis = 150)
        }
        rule.onNodeWithTag("keyboard").assertIsDisplayed()
    }

    @Test
    fun AC_14_1_hardwareLetterOpensSearchInBrowsingMode() {
        rule.runOnUiThread {
            rule.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_C))
            rule.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_C))
        }
        rule.onNodeWithTag("query", useUnmergedTree = true).assertTextEquals("c")
        rule.onNodeWithTag("keyboard").assertDoesNotExist()
        rule.runOnUiThread { rule.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE)) }
        rule.runOnIdle { assertThat(rule.activity.overlay).isEqualTo(Overlay.None) }
        rule.onNodeWithTag("search").assertDoesNotExist()
    }

    @Test
    fun AC_13_1_homeButtonClosesSettings() {
        backgroundPoint().performTouchInput { longClick(bottomCenter.copy(y = bottom - height * 0.1f)) }
        rule.onNodeWithTag("popup_settings").performClick()
        rule.onNodeWithTag("settings").assertIsDisplayed()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_MAIN)
                .addCategory(android.content.Intent.CATEGORY_HOME)
                .setClass(context, MainActivity::class.java)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        rule.waitUntil(5_000) { !rule.activity.settingsStack.isOpen }
        rule.onNodeWithTag("settings").assertDoesNotExist()
    }

    @Test
    fun AC_13_1_homeIntentClosesEverything() {
        backgroundPoint().performTouchInput { doubleClick(bottomCenter.copy(y = bottom - height * 0.15f)) }
        rule.onNodeWithTag("keyboard").assertIsDisplayed()
        rule.runOnUiThread {
            rule.activity.handleHomeIntent(
                android.content.Intent(
                    android.content.Intent.ACTION_MAIN,
                ).addCategory(android.content.Intent.CATEGORY_HOME),
            )
        }
        rule.onNodeWithTag("keyboard").assertDoesNotExist()
    }

    @Test
    fun bottomRowHasResizeAndSpaceOnly() {
        openSearch()
        val resize = rule.onNodeWithTag("key_RESIZE").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val space = rule.onNodeWithTag("key_SPACE").fetchSemanticsNode().boundsInRoot
        val keyboard = rule.onNodeWithTag("keyboard").fetchSemanticsNode().boundsInRoot
        assertThat(resize.right).isLessThan(space.left)
        assertThat(keyboard.right - space.right).isLessThan(20f)
        rule.onNodeWithTag("key_ENTER").assertDoesNotExist()
        rule.onNodeWithTag("enter").assertDoesNotExist()
        rule.onNodeWithTag("query_clear").assertDoesNotExist()
        rule.onNodeWithTag("result_count").assertDoesNotExist()
    }

    @Test
    fun swipeDownAboveTheKeyboardClosesSearchButDrawingDownFromAKeyDoesNot() {
        openSearch()
        rule.onNodeWithTag("key_y").performTouchInput {
            down(center)
            repeat(STEPS) { i -> moveTo(center + Offset(0f, 400f * (i + 1) / STEPS)) }
            up()
        }
        rule.runOnIdle { assertThat(rule.activity.overlay).isEqualTo(Overlay.Search) }
        rule.onNodeWithTag("query_row").performTouchInput {
            swipeDown(startY = centerY, endY = centerY + 600f, durationMillis = 200)
        }
        rule.runOnIdle { assertThat(rule.activity.overlay).isEqualTo(Overlay.None) }
    }

    @Test
    fun swipeDownOnRecentsClosesSearch() {
        openSearch()
        rule.onNodeWithTag("recents").performTouchInput {
            swipeDown(startY = top + height * 0.2f, endY = top + height * 0.7f, durationMillis = 200)
        }
        rule.runOnIdle { assertThat(rule.activity.overlay).isEqualTo(Overlay.None) }
    }

    @Test
    fun longPressOnResultTogglesAppInCurrentEnv() {
        openSearch()
        val key = rule.runOnIdle { rule.activity.search.rows.first().key }
        val before = key in envItems()
        rule.onNodeWithTag("result_0").performTouchInput { longClick() }
        rule.onNodeWithTag("menu_info").assertIsDisplayed()
        rule.onNodeWithTag("menu_env").performClick()
        rule.onNodeWithTag("popup").assertDoesNotExist()
        rule.waitUntil(5_000) { (key in envItems()) != before }
    }

    @Test
    fun pickerAppliesEachTapWithoutDone() {
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("env_add").performScrollTo().performClick()
        val app = runBlocking { container.catalog.apps.first() }.first { it.key !in envItems() }
        rule.onNodeWithTag("picker_filter").performTextInput(app.label)
        rule.onNodeWithTag("pick_${app.label}").performClick()
        rule.waitUntil(5_000) { app.key in envItems() }
        rule.onNodeWithTag("pick_${app.label}").assertIsSelected()
        rule.onNodeWithTag("pick_${app.label}").performClick()
        rule.waitUntil(5_000) { app.key !in envItems() }
        rule.onNodeWithTag("pick_${app.label}").assertIsNotSelected()
        Espresso.closeSoftKeyboard()
    }

    @Test
    fun spaceRowIsHalfAsTallAndKeyboardSitsAKeyAboveTheBottom() {
        openSearch()
        val q = rule.onNodeWithTag("key_q").fetchSemanticsNode().boundsInRoot
        val space = rule.onNodeWithTag("key_SPACE").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val keyboard = rule.onNodeWithTag("keyboard").fetchSemanticsNode().boundsInRoot
        val search = rule.onNodeWithTag("search").fetchSemanticsNode().boundsInRoot
        val navBar = ViewCompat.getRootWindowInsets(rule.activity.window.decorView)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        with(rule.density) {
            assertThat(space.height + KeyboardDimens.gap.toPx())
                .isWithin(2f).of((q.height + KeyboardDimens.gap.toPx()) / 2)
            assertThat(search.bottom - navBar - keyboard.bottom).isWithin(2f).of(KeyboardDimens.rowHeight.toPx())
        }
        rule.onNodeWithTag("key_s").performClick()
        rule.onNodeWithTag("key_SPACE").performClick()
        rule.onNodeWithTag("query", useUnmergedTree = true).assertTextEquals("s ")
    }

    @Test
    fun drawingAcrossTheKeyboardIsDecodedAndTappingStillTypes() {
        openSearch()
        rule.onNodeWithTag("input_toggle").assertDoesNotExist()
        rule.onNodeWithTag("key_c").performClick()
        rule.onNodeWithTag("query", useUnmergedTree = true).assertTextEquals("c")
        clearQuery()
        rule.onNodeWithTag("query", useUnmergedTree = true).assertTextEquals("")
        drawOnKeyboard("maps")
        rule.onNodeWithTag("query", useUnmergedTree = true).assertTextEquals("maps")
        val trail = rule.onNodeWithTag("glide_trail").fetchSemanticsNode().boundsInRoot
        val keyboard = rule.onNodeWithTag("keyboard").fetchSemanticsNode().boundsInRoot
        assertThat(trail).isEqualTo(keyboard)
        clearQuery()
        rule.onNodeWithTag("query", useUnmergedTree = true).assertTextEquals("")
    }

    @Test
    fun gestureLikeSeveralAppsShowsThemAllAsSuggestions() {
        val twins = runBlocking { container.catalog.apps.first().sortedBy { it.label }.take(2).map { it.key } }
        runBlocking {
            container.overrides.rename(twins[0], "Zebra One")
            container.overrides.rename(twins[1], "Zebra Two")
        }
        try {
            openSearch()
            rule.waitUntil(5_000) { rule.activity.search.rows.count { it.label.startsWith("Zebra") } == 2 }
            drawOnKeyboard("zebra")
            rule.onNodeWithTag("suggestion_0").assertIsDisplayed()
            rule.onNodeWithTag("suggestion_1").assertIsDisplayed()
            rule.onNodeWithTag("query", useUnmergedTree = true).assertDoesNotExist()
            assertThat(rule.activity.search.rows.map { it.label }).containsExactly("Zebra One", "Zebra Two")
            clearQuery()
            rule.onNodeWithTag("query", useUnmergedTree = true).assertTextEquals("")
            assertThat(rule.activity.search.rows.size).isGreaterThan(2)
        } finally {
            runBlocking { twins.forEach { container.overrides.rename(it, null) } }
        }
    }

    private fun clearQuery() {
        rule.onNodeWithTag("key_BACKSPACE").performTouchInput {
            down(centerRight - Offset(4f, 0f))
            repeat(STEPS) { i -> moveTo(centerRight - Offset(4f + 200f * (i + 1) / STEPS, 0f)) }
            up()
        }
    }

    private fun drawOnKeyboard(word: String) {
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

    @Test
    fun notificationCountShowsNextToRowLabelWhenEnabled() {
        val key = envItems().first()
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
    fun selectedOptionIsMarkedByBackgroundAndPressStyleApplies() {
        rule.runOnIdle { rule.activity.settingsStack.open() }
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
        rule.runOnIdle { rule.activity.settingsStack.open() }
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
    fun memoryShowsLearnedDrawingsPerAppAndForgetsThem() {
        val app = runBlocking { container.catalog.apps.first().sortedBy { it.label }.first() }
        runBlocking {
            container.gestureLearning.update { listOf(LearnedGesture("qwerty", app.key, 2.0)) }
        }
        try {
            rule.runOnIdle { rule.activity.settingsStack.open(SettingsRoute.Advanced) }
            rule.onNodeWithTag("nav_memory").performClick()
            rule.onNodeWithTag("memory_app_${app.label}").performClick()
            rule.onNodeWithTag("memory_drawing_0").assertIsDisplayed()
            rule.onNodeWithTag("memory_forget_0").performClick()
            rule.waitUntil(5_000) { container.gestureLearning.samples.value.isEmpty() }
        } finally {
            runBlocking { container.gestureLearning.update { emptyList() } }
        }
    }

    @Test
    fun holdingASpaceInThePopupForOneSecondOpensItsSettings() {
        backgroundPoint().performTouchInput { longClick(bottomCenter.copy(y = bottom - height * 0.1f)) }
        rule.onNodeWithTag("popup_env_$normalId").performTouchInput { longClick(durationMillis = 1_200) }
        rule.waitUntil(5_000) { rule.activity.settingsStack.top == SettingsRoute.EnvEditor(normalId) }
        assertThat(rule.activity.overlay).isEqualTo(Overlay.None)
    }

    @Test
    fun colouredIconsAreToggledInTheSpaceSettings() {
        rule.runOnIdle { rule.activity.settingsStack.open(SettingsRoute.EnvEditor(normalId)) }
        rule.onNodeWithTag("env_colored_icons").performScrollTo().performClick()
        rule.waitUntil(5_000) { container.environments.environments.value.first { it.id == normalId }.coloredIcons }
        rule.onNodeWithTag("env_colored_icons").performClick()
        rule.waitUntil(5_000) { !container.environments.environments.value.first { it.id == normalId }.coloredIcons }
    }

    @Test
    fun searchMenuHidesAppAndOptionsShowHiddenApps() {
        openSearch()
        rule.onNodeWithTag("result_0").performTouchInput { longClick() }
        val key = rule.activity.search.menu!!.key
        val label = rule.activity.search.menu!!.label
        rule.onNodeWithTag("menu_hide").performClick()
        try {
            rule.waitUntil(5_000) { container.overrides.overrides.value[key]?.hidden == true }
            rule.waitUntil(5_000) { rule.activity.search.rows.none { it.key == key } }
            rule.onNodeWithTag("search_options").performClick()
            rule.onNodeWithTag("search_option_show_hidden").performClick()
            rule.waitUntil(5_000) { rule.activity.search.rows.any { it.key == key && it.hidden && it.label == label } }
            rule.onNodeWithTag("search_option_show_hidden").performClick()
            rule.waitUntil(5_000) { rule.activity.search.rows.none { it.key == key } }
        } finally {
            runBlocking { container.overrides.setHidden(key, false) }
        }
    }

    @Test
    fun appBoundToASwipeShowsTheArrowNextToItsName() {
        val key = envItems().first()
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
            assertThat(rule.activity.overlay).isEqualTo(Overlay.None)
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
    fun editButtonOpensTheCurrentSpace() {
        rule.onNodeWithTag("home_edit").performClick()
        rule.waitUntil(5_000) { rule.activity.settingsStack.top == SettingsRoute.EnvEditor(normalId) }
    }

    @Test
    fun frequentlyUsedAppIsSuggestedInsteadOfTheWeakestHomeApp() {
        val home = envItems()
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
        rule.waitUntil(5_000) { envItems() == home.dropLast(1) + frequent }
        rule.waitUntil(5_000) {
            runCatching { rule.onNodeWithTag("home_suggestion").assertDoesNotExist() }.isSuccess
        }
    }

    @Test
    fun settingsRootListsSpacesAsRadioRowWithCurrentFirst() {
        rule.runOnIdle { rule.activity.settingsStack.open() }
        val work = container.environments.environments.value.first { it.type == EnvType.WORK }.id
        rule.onNodeWithTag("space_chip_$normalId").assertIsSelected()
        val normalLeft = rule.onNodeWithTag("space_chip_$normalId").fetchSemanticsNode().boundsInRoot.left
        val workLeft = rule.onNodeWithTag("space_chip_$work").fetchSemanticsNode().boundsInRoot.left
        assertThat(normalLeft).isLessThan(workLeft)
        rule.onNodeWithTag("space_chip_$work").assertIsNotSelected().performClick()
        rule.waitUntil(5_000) { container.settings.settings.value.currentEnvId == work }
        rule.onNodeWithTag("space_chip_$work").assertIsSelected()
        rule.onNodeWithTag("nav_search").assertDoesNotExist()
    }

    @Test
    fun searchHeaderHoldsOnlySearchOptions() {
        openSearch()
        rule.onNodeWithTag("toggle_hidden").assertDoesNotExist()
        val header = rule.onNodeWithTag("search_header").fetchSemanticsNode().boundsInRoot
        val options = rule.onNodeWithTag("search_options").fetchSemanticsNode().boundsInRoot
        val recents = rule.onNodeWithTag("recents").fetchSemanticsNode().boundsInRoot
        assertThat(options.bottom).isAtMost(header.bottom + 1f)
        assertThat(options.bottom).isAtMost(recents.top)
        rule.onNodeWithTag("search_options").performClick()
        rule.onNodeWithTag("search_option_FUZZY").performClick()
        rule.waitUntil(5_000) { !container.settings.settings.value.searchFuzzy }
    }

    @Test
    fun styleChangesTheFontOfTheWholeApp() {
        rule.runOnIdle { rule.activity.settingsStack.open() }
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

    @Test
    fun eachSpaceKeepsItsOwnListPosition() {
        val work = container.environments.environments.value.first { it.type == EnvType.WORK }.id
        val top = { rule.onNodeWithTag("list_handle").fetchSemanticsNode().boundsInRoot.top }
        val start = top()
        runBlocking { container.environments.setListOffset(normalId, 100) }
        rule.waitUntil(5_000) { top() - start > 50f }
        val moved = top()
        runBlocking { container.settings.update { it.copy(currentEnvId = work) } }
        rule.waitUntil(5_000) { abs(top() - start) < 2f }
        runBlocking { container.settings.update { it.copy(currentEnvId = normalId) } }
        rule.waitUntil(5_000) { abs(top() - moved) < 2f }
    }

    @Test
    fun styleOffersElevenThemesIncludingTheNewOnes() {
        rule.runOnIdle { rule.activity.settingsStack.open() }
        rule.onNodeWithTag("nav_style").performClick()
        rule.onNodeWithTag("palette_dropdown").performClick()
        listOf("LAGOON", "PLUM", "EMBER", "MIST", "ROSE").forEach {
            rule.onNodeWithTag("palette_$it").performScrollTo().assertIsDisplayed()
        }
        val original = container.environments.environments.value.first { it.id == normalId }.preset
        rule.onNodeWithTag("palette_ROSE").performClick()
        try {
            rule.waitUntil(5_000) {
                container.environments.environments.value.first { it.id == normalId }.preset == PalettePreset.ROSE
            }
        } finally {
            runBlocking { container.environments.setPalette(normalId, original) }
        }
    }

    @Test
    fun drawingALearnedShapeOnTheHomeScreenFindsTheAppAtAnotherScale() {
        val key = envItems().first()
        runBlocking { container.gestureLearning.update { listOf(LearnedGesture("mapsdfghjkl", key, 1.0)) } }
        rule.waitUntil(5_000) { container.gestureLearning.samples.value.isNotEmpty() }
        drawOnHome("mapsdfghjkl".filterIndexed { i, _ -> i in listOf(0, 1, 2, 3, 10) }, scale = 1.5f)
        rule.waitUntil(5_000) { rule.activity.overlay == Overlay.Search }
        rule.runOnIdle {
            assertThat(rule.activity.search.suggesting).isTrue()
            assertThat(rule.activity.search.rows.map { it.key }).containsExactly(key)
        }
    }

    @Test
    fun fiveUnknownDrawingsAskWhatYouMeantAndOpenLearning() {
        runBlocking { container.gestureLearning.update { emptyList() } }
        repeat(MainActivity.LEARN_HINT_MISSES - 1) {
            drawOnHome("qazwsx", scale = 1f)
            rule.onNodeWithTag("learn_hint").assertDoesNotExist()
        }
        drawOnHome("qazwsx", scale = 1f)
        rule.onNodeWithTag("learn_hint").assertIsDisplayed().performClick()
        rule.runOnIdle { assertThat(rule.activity.settingsStack.top).isEqualTo(SettingsRoute.MemoryPickApp) }
    }

    @Test
    fun learningFromMissesOpensTheSheetWithTheAveragePattern() {
        runBlocking { container.gestureLearning.update { emptyList() } }
        repeat(MainActivity.LEARN_HINT_MISSES) { drawOnHome("qazwsx", scale = 1f + it * 0.05f) }
        rule.onNodeWithTag("learn_hint").performClick()
        val app = runBlocking { container.catalog.apps.first() }.minBy { it.label }
        rule.runOnIdle { assertThat(rule.activity.settingsStack.draft).isNotEmpty() }
        rule.onNodeWithTag("pick_${app.label}").performClick()
        rule.onNodeWithTag("gesture_sheet").assertIsDisplayed()
        rule.onNodeWithTag("sheet_add").performClick()
        rule.waitUntil(5_000) {
            container.gestureLearning.samples.value.any { it.key == app.key && it.shape.size >= 2 }
        }
        rule.runOnIdle { assertThat(rule.activity.settingsStack.draft).isNull() }
    }

    @Test
    fun learnHintCanBeSwipedAway() {
        rule.runOnIdle { rule.activity.learnHint = true }
        rule.onNodeWithTag("learn_hint").performTouchInput { swipeRight(startX = left + 10f, endX = right) }
        rule.onNodeWithTag("learn_hint").assertDoesNotExist()
        rule.runOnIdle { assertThat(rule.activity.settingsStack.isOpen).isFalse() }
    }

    private fun drawOnHome(word: String, scale: Float) {
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

    @Test
    fun memoryAddsAGestureDrawnInTheSheet() {
        runBlocking { container.gestureLearning.update { emptyList() } }
        val app = runBlocking { container.catalog.apps.first() }.minBy { it.label }
        rule.runOnIdle { rule.activity.settingsStack.open(SettingsRoute.Memory) }
        rule.onNodeWithTag("memory_add_gesture").performClick()
        rule.onNodeWithTag("pick_${app.label}").performClick()
        rule.onNodeWithTag("memory_add").performClick()
        rule.onNodeWithTag("gesture_sheet").assertIsDisplayed()
        rule.onNodeWithTag("sheet_add").performClick()
        rule.onNodeWithTag("gesture_sheet").assertIsDisplayed()
        rule.onNodeWithTag("gesture_pad").performTouchInput {
            down(Offset(width * 0.05f, height / 6f))
            repeat(STEPS) { i -> moveTo(Offset(width * (0.05f + 0.9f * (i + 1) / STEPS), height / 6f)) }
            up()
        }
        rule.onNodeWithTag("sheet_add").performClick()
        rule.onNodeWithTag("gesture_sheet").assertDoesNotExist()
        rule.waitUntil(5_000) {
            container.gestureLearning.samples.value.any {
                it.key == app.key && it.path == "qwertyuiop" && it.shape.size >= 2
            }
        }
        rule.onNodeWithTag("memory_drawing_0").assertIsDisplayed()
    }

    @Test
    fun gestureSheetClosesWhenPulledDown() {
        val app = runBlocking { container.catalog.apps.first() }.first()
        rule.runOnIdle { rule.activity.settingsStack.open(SettingsRoute.MemoryApp(app.key)) }
        rule.onNodeWithTag("memory_add").performClick()
        rule.onNodeWithTag("gesture_sheet_handle").performTouchInput {
            swipeDown(startY = centerY, endY = centerY + 900f, durationMillis = 300)
        }
        rule.onNodeWithTag("gesture_sheet").assertDoesNotExist()
        rule.onNodeWithTag("memory_add").performClick()
        Espresso.pressBack()
        rule.onNodeWithTag("gesture_sheet").assertDoesNotExist()
        rule.onNodeWithTag("memory_add").assertIsDisplayed()
    }

    @Test
    fun searchMenuRenamesTheApp() {
        openSearch()
        rule.onNodeWithTag("result_0").performTouchInput { longClick() }
        val key = rule.activity.search.menu!!.key
        rule.onNodeWithTag("menu_rename").performClick()
        rule.onNodeWithTag("text_input").assertIsDisplayed()
        assertThat(rule.activity.renameTarget).isEqualTo(key)
        Espresso.pressBack()
    }

    @Test
    fun popupListsSpacesAsVerticalRadioButtons() {
        backgroundPoint().performTouchInput { longClick(bottomCenter.copy(y = bottom - height * 0.1f)) }
        val work = container.environments.environments.value.first { it.type == EnvType.WORK }.id
        rule.onNodeWithTag("popup_env_$normalId").assertIsSelected()
        rule.onNodeWithTag("popup_env_$work").assertIsNotSelected()
        val first = rule.onNodeWithTag("popup_env_$normalId").fetchSemanticsNode().boundsInRoot
        val second = rule.onNodeWithTag("popup_env_$work").fetchSemanticsNode().boundsInRoot
        assertThat(second.top).isAtLeast(first.bottom - 1f)
    }

    @Test
    fun everySettingsPageShowsTheCurrentSpaceName() {
        val name = container.environments.environments.value.first { it.id == normalId }.name
        rule.runOnIdle { rule.activity.settingsStack.open() }
        rule.onNodeWithTag("settings_space", useUnmergedTree = true).assertTextEquals(name)
        rule.onNodeWithTag("nav_keyboard").performClick()
        rule.onNodeWithTag("settings_space", useUnmergedTree = true).assertTextEquals(name)
    }

    @Test
    fun keyboardWidthAndShiftMoveTheWholeInputPanel() {
        runBlocking { container.settings.update { it.copy(keyboardWidth = 0.6f, keyboardShift = 1f) } }
        openSearch()
        rule.waitForIdle()
        val pane = rule.onNodeWithTag("input_pane").fetchSemanticsNode().boundsInRoot
        val keyboard = rule.onNodeWithTag("keyboard").fetchSemanticsNode().boundsInRoot
        val results = rule.onNodeWithTag("recents").fetchSemanticsNode().boundsInRoot
        val full = results.right - rule.onNodeWithTag("results").fetchSemanticsNode().boundsInRoot.left
        assertThat(pane.width).isWithin(3f).of(full * 0.6f)
        assertThat(pane.right).isWithin(3f).of(results.right)
        assertThat(keyboard.width).isWithin(2f).of(pane.width)
    }

    @Test
    fun resizeKeyShowsAnchorsAndDraggingASideAnchorNarrowsTheKeyboard() {
        openSearch()
        rule.onNodeWithTag("key_RESIZE").performClick()
        rule.onNodeWithTag("resize_overlay").assertIsDisplayed().performTouchInput {
            val inset = 10.dp.toPx()
            down(Offset(left + inset, centerY))
            repeat(STEPS) { i -> moveTo(Offset(left + inset + 200f * (i + 1) / STEPS, centerY)) }
            up()
        }
        rule.waitUntil(5_000) { container.settings.settings.value.keyboardWidth < 0.9f }
        rule.runOnIdle { assertThat(container.settings.settings.value.keyboardShift).isEqualTo(1f) }
        rule.onNodeWithTag("resize_overlay").performTouchInput { click(Offset(centerX, top + 4f)) }
        rule.onNodeWithTag("resize_overlay").assertDoesNotExist()
    }

    @Test
    fun styleChangesStrokeColour() {
        rule.runOnIdle { rule.activity.settingsStack.open(SettingsRoute.Style) }
        rule.onNodeWithTag("stroke_color_dropdown").performScrollTo().performClick()
        rule.onNodeWithTag("stroke_color_PINK").performScrollTo().performClick()
        rule.waitUntil(5_000) { container.settings.settings.value.strokeColor == StrokeColor.PINK }
    }

    @Test
    fun deleteKeyIsACrossAfterADrawingAndBackspaceWhileTyping() {
        openSearch()
        drawOnKeyboard("maps")
        rule.onNodeWithText("×", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("key_BACKSPACE").performClick()
        rule.onNodeWithTag("query", useUnmergedTree = true).assertTextEquals("")
        rule.onNodeWithTag("key_m").performClick()
        rule.onNodeWithText("⌫", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun digitBubbleIsNotCutByTheQueryRow() {
        openSearch()
        rule.onNodeWithTag("key_u").performTouchInput { down(center) }
        rule.waitUntil(2_000) { runCatching { rule.onNodeWithTag("digit_bubble").assertExists() }.isSuccess }
        val bubble = rule.onNodeWithTag("digit_bubble").fetchSemanticsNode().boundsInRoot
        val keyboard = rule.onNodeWithTag("keyboard").fetchSemanticsNode().boundsInRoot
        assertThat(bubble.top).isLessThan(keyboard.top)
        rule.onNodeWithTag("digit_bubble").assertIsDisplayed()
        rule.onNodeWithTag("key_u").performTouchInput { up() }
    }

    @Test
    fun suggestionIconsHaveEqualGapsAboveAndBelow() {
        openSearch()
        rule.runOnIdle { rule.activity.search.suggest(envItems().take(2)) }
        val icon = rule.onNodeWithTag("suggestion_0").fetchSemanticsNode().boundsInRoot
        val pane = rule.onNodeWithTag("input_pane").fetchSemanticsNode().boundsInRoot
        val q = rule.onNodeWithTag("key_q").fetchSemanticsNode().boundsInRoot
        assertThat(icon.top - pane.top).isWithin(2f).of(q.top - icon.bottom)
    }

    @Test
    fun appBoundToASystemGestureIsMarkedInSearchAndOnHome() {
        val key = envItems().first()
        openSearch()
        val label = rule.runOnIdle { rule.activity.search.rows.first { it.key == key }.label }
        rule.runOnIdle { rule.activity.openSearchMenu(key, label, Offset(100f, 100f)) }
        rule.onNodeWithTag("menu_shortcut").performClick()
        rule.waitUntil(5_000) { container.overrides.overrides.value[key]?.systemShortcut == true }
        rule.waitUntil(5_000) { rule.activity.search.rows.first { it.key == key }.shortcut }
        Espresso.pressBack()
        rule.onNodeWithTag("swipe_0", useUnmergedTree = true).assertIsDisplayed()
        runBlocking { container.overrides.setSystemShortcut(key, false) }
    }

    @Test
    fun assistIntentOpensSearchFromAnywhere() {
        rule.runOnUiThread {
            rule.activity.handleHomeIntent(android.content.Intent(android.content.Intent.ACTION_ASSIST))
        }
        rule.runOnIdle { assertThat(rule.activity.overlay).isEqualTo(Overlay.Search) }
        rule.onNodeWithTag("keyboard").assertIsDisplayed()
    }

    @Test
    fun styleTurnsOffDotsOnTheLine() {
        rule.runOnIdle { rule.activity.settingsStack.open(SettingsRoute.Style) }
        rule.onNodeWithTag("stroke_dots").performScrollTo().performClick()
        rule.waitUntil(5_000) { !container.settings.settings.value.strokeDots }
        runBlocking { container.settings.update { it.copy(strokeDots = true) } }
    }

    @Test
    fun backgroundImageShowsOnHomeWithAVisibilitySliderUntilRemoved() {
        rule.onNodeWithTag("home_background").assertDoesNotExist()
        rule.runOnIdle { rule.activity.settingsStack.open(SettingsRoute.Style) }
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
            rule.runOnIdle { rule.activity.settingsStack.close() }
            rule.onNodeWithTag("home_background").assertIsDisplayed()
            rule.runOnIdle { rule.activity.settingsStack.open(SettingsRoute.Style) }
            rule.onNodeWithTag("background_clear").performScrollTo().performClick()
            rule.waitUntil(5_000) { container.background.image.value == null }
            rule.onNodeWithTag("background_opacity").assertDoesNotExist()
        } finally {
            container.background.clear()
            file.delete()
            runBlocking { container.settings.update { it.copy(backgroundOpacity = BackgroundOpacity.DEFAULT) } }
        }
    }

    @Test
    fun searchOptionsTurnOnColouredIcons() {
        openSearch()
        rule.onNodeWithTag("search_options").performClick()
        rule.onNodeWithTag("search_option_COLORED_ICONS").performClick()
        rule.waitUntil(5_000) { container.settings.settings.value.searchColoredIcons }
    }

    private companion object {
        const val STEPS = 6
    }
}
