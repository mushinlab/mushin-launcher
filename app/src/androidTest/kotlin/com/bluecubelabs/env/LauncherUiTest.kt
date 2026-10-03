package com.bluecubelabs.env

import android.view.KeyEvent
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bluecubelabs.env.core.apps.NotificationSource
import com.bluecubelabs.env.core.model.EnvType
import com.bluecubelabs.env.core.model.PressFeedback
import com.bluecubelabs.env.core.search.KeyboardLayouts
import com.bluecubelabs.env.feature.search.GestureDimens
import com.bluecubelabs.env.feature.search.KeyboardDimens
import com.bluecubelabs.env.feature.search.KeyboardGeometry
import com.google.common.truth.Truth.assertThat
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
        container.settings.update { it.copy(currentEnvId = normal.id, keyboardGestureTyping = false) }
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
            it.copy(keyboardGestureTyping = false, notificationCounts = false, pressFeedback = PressFeedback.FLASH)
        }
        container.notificationCounts.value = emptyMap()
    }

    private fun backgroundPoint() = rule.onNodeWithTag("home")

    private fun openSearch() {
        backgroundPoint().performTouchInput { doubleClick(bottomCenter.copy(y = bottom - height * 0.15f)) }
        rule.onNodeWithTag("search").assertIsDisplayed()
    }

    private fun envItems(): List<com.bluecubelabs.env.core.model.AppKey> =
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
    fun enterButtonSitsUnderRecentsAboveKeyboard() {
        openSearch()
        val enter = rule.onNodeWithTag("enter").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val recents = rule.onNodeWithTag("recents").fetchSemanticsNode().boundsInRoot
        val keyboard = rule.onNodeWithTag("keyboard").fetchSemanticsNode().boundsInRoot
        assertThat(enter.left).isAtLeast(recents.left - 2f)
        assertThat(enter.top).isAtLeast(recents.bottom - 2f)
        assertThat(enter.bottom).isAtMost(keyboard.top + 2f)
    }

    @Test
    fun swipeDownOnKeyboardClosesSearch() {
        openSearch()
        rule.onNodeWithTag("keyboard").performTouchInput {
            swipeDown(startY = top + 10f, endY = bottom - 10f, durationMillis = 200)
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
        backgroundPoint().performTouchInput { longClick(bottomCenter.copy(y = bottom - height * 0.1f)) }
        rule.onNodeWithTag("popup_settings").performClick()
        rule.onNodeWithTag("nav_env_apps").performClick()
        rule.onNodeWithTag("env_add").performClick()
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
    fun keyboardHasSpaceRowAndSitsHalfAKeyAboveTheBottom() {
        openSearch()
        val q = rule.onNodeWithTag("key_q").fetchSemanticsNode().boundsInRoot
        val space = rule.onNodeWithTag("key_SPACE").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val keyboard = rule.onNodeWithTag("keyboard").fetchSemanticsNode().boundsInRoot
        val search = rule.onNodeWithTag("search").fetchSemanticsNode().boundsInRoot
        val navBar = ViewCompat.getRootWindowInsets(rule.activity.window.decorView)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        with(rule.density) {
            assertThat(space.height).isWithin(1f).of(q.height)
            assertThat(search.bottom - navBar - keyboard.bottom).isWithin(2f).of((KeyboardDimens.rowHeight / 2).toPx())
        }
        rule.onNodeWithTag("key_s").performClick()
        rule.onNodeWithTag("key_SPACE").performClick()
        rule.onNodeWithTag("query", useUnmergedTree = true).assertTextEquals("s ")
    }

    @Test
    fun gestureInputDecodesWordDrawnOnBlankPanel() {
        openSearch()
        rule.onNodeWithTag("input_toggle").performClick()
        rule.waitUntil(5_000) { container.settings.settings.value.keyboardGestureTyping }
        rule.onNodeWithTag("keyboard").assertDoesNotExist()
        val panel = rule.onNodeWithTag("gesture_panel").fetchSemanticsNode().boundsInRoot
        with(rule.density) { assertThat(panel.height).isWithin(2f).of(GestureDimens.height.toPx()) }
        rule.onNodeWithTag("gesture_panel").performTouchInput {
            val padding = KeyboardDimens.verticalPadding.toPx()
            val keys = KeyboardGeometry.compute(
                KeyboardLayouts.EN.copy(rows = KeyboardLayouts.EN.rows.take(KeyboardLayouts.LETTER_ROWS)),
                width.toFloat(),
                (height - padding * 2) / KeyboardLayouts.LETTER_ROWS,
                padding,
                KeyboardDimens.gap.toPx(),
            )
            val points = "maps".map { c -> keys.first { it.spec.char == c }.rect.center }
            down(points.first())
            points.drop(1).forEach { moveTo(it) }
            up()
        }
        rule.onNodeWithTag("query", useUnmergedTree = true).assertTextEquals("maps")
        rule.onNodeWithTag("input_toggle").performClick()
        rule.onNodeWithTag("keyboard").assertIsDisplayed()
    }

    @Test
    fun notificationCountShowsNextToRowLabelWhenEnabled() {
        val key = envItems().first()
        container.notificationCounts.value = mapOf(NotificationSource(key.packageName, key.userSerial) to 3)
        rule.onNodeWithTag("badge_0", useUnmergedTree = true).assertDoesNotExist()
        runBlocking { container.settings.update { it.copy(notificationCounts = true) } }
        rule.waitUntil(5_000) {
            runCatching { rule.onNodeWithTag("badge_0", useUnmergedTree = true).assertTextEquals("3") }.isSuccess
        }
    }

    @Test
    fun selectedOptionIsMarkedByBackgroundAndPressStyleApplies() {
        rule.runOnIdle { rule.activity.settingsStack.open() }
        rule.onNodeWithTag("nav_press").performClick()
        rule.onNodeWithTag("press_FLASH").assertIsSelected()
        rule.onNodeWithTag("press_DIM").assertIsNotSelected().performClick()
        rule.waitUntil(5_000) { container.settings.settings.value.pressFeedback == PressFeedback.DIM }
        rule.onNodeWithTag("press_DIM").assertIsSelected()
        rule.onNodeWithTag("press_FLASH").assertIsNotSelected()
    }
}
