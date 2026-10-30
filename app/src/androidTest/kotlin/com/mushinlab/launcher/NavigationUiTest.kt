package com.mushinlab.launcher

import android.view.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationUiTest : LauncherUiTestBase() {
    @Test
    fun AC_07_1_doubleTapOpensKeyboardWithoutIme() {
        backgroundPoint().performTouchInput { doubleClick(bottomCenter.copy(y = bottom - height * 0.15f)) }
        rule.onNodeWithTag("keyboard").assertIsDisplayed()
        val insets = ViewCompat.getRootWindowInsets(rule.activity.window.decorView)
        assertThat(insets?.isVisible(WindowInsetsCompat.Type.ime())).isFalse()
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
        rule.runOnIdle { assertThat(rule.activity.launcher.navigator.overlay).isEqualTo(Overlay.None) }
        rule.onNodeWithTag("search").assertDoesNotExist()
    }

    @Test
    fun AC_13_1_homeButtonClosesSettings() {
        rule.onNodeWithTag("home_settings").performClick()
        rule.onNodeWithTag("settings").assertIsDisplayed()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_MAIN)
                .addCategory(android.content.Intent.CATEGORY_HOME)
                .setClass(context, MainActivity::class.java)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        rule.waitUntil(5_000) { !rule.activity.launcher.navigator.settings.isOpen }
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
    fun assistIntentOpensSearchFromAnywhere() {
        rule.runOnUiThread {
            rule.activity.handleHomeIntent(android.content.Intent(android.content.Intent.ACTION_ASSIST))
        }
        rule.runOnIdle { assertThat(rule.activity.launcher.navigator.overlay).isEqualTo(Overlay.Search) }
        rule.onNodeWithTag("keyboard").assertIsDisplayed()
    }
}
