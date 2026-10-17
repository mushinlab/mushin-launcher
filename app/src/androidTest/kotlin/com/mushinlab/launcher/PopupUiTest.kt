package com.mushinlab.launcher

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PopupUiTest : LauncherUiTestBase() {
    @Test
    fun homePopupHasScrimAndTapOnItDismisses() {
        backgroundPoint().performTouchInput { longClick(bottomCenter.copy(y = bottom - height * 0.1f)) }
        rule.onNodeWithTag("popup").assertIsDisplayed()
        tapScrimOutsidePopup()
        rule.onNodeWithTag("popup").assertDoesNotExist()
        rule.onNodeWithTag("popup_scrim").assertDoesNotExist()
        rule.runOnIdle { assertThat(rule.activity.launcher.navigator.overlay).isEqualTo(Overlay.None) }
    }

    @Test
    fun searchMenuAndOptionsHaveScrimAndTapOnItDismisses() {
        openSearch()
        rule.onNodeWithTag("result_0").performTouchInput { longClick() }
        rule.onNodeWithTag("menu_info").assertIsDisplayed()
        tapScrimOutsidePopup()
        rule.onNodeWithTag("popup").assertDoesNotExist()
        rule.runOnIdle { assertThat(rule.activity.launcher.navigator.search.panels.menu).isNull() }
        rule.onNodeWithTag("search_options").performClick()
        rule.onNodeWithTag("search_option_FUZZY").assertIsDisplayed()
        tapScrimOutsidePopup()
        rule.onNodeWithTag("popup").assertDoesNotExist()
        rule.onNodeWithTag("popup_scrim").assertDoesNotExist()
        rule.runOnIdle {
            assertThat(rule.activity.launcher.navigator.search.panels.options).isNull()
            assertThat(rule.activity.launcher.navigator.overlay).isEqualTo(Overlay.Search)
        }
    }

    private fun tapScrimOutsidePopup() {
        val popup = rule.onNodeWithTag("popup").fetchSemanticsNode().boundsInRoot
        val scrim = rule.onNodeWithTag("popup_scrim").assertIsDisplayed()
        val bounds = scrim.fetchSemanticsNode().boundsInRoot
        val y = if (popup.top - bounds.top > bounds.bottom - popup.bottom) {
            (bounds.top + popup.top) / 2
        } else {
            (popup.bottom + bounds.bottom) / 2
        }
        scrim.performTouchInput { click(Offset(width / 2f, y - bounds.top)) }
    }
}
