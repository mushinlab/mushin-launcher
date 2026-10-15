package com.mushinlab.launcher

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchUiTest : LauncherUiTestBase() {
    @Test
    fun swipeDownOnRecentsClosesSearch() {
        openSearch()
        rule.onNodeWithTag("recents").performTouchInput {
            swipeDown(startY = top + height * 0.2f, endY = top + height * 0.7f, durationMillis = 200)
        }
        rule.runOnIdle { assertThat(rule.activity.launcher.navigator.overlay).isEqualTo(Overlay.None) }
    }

    @Test
    fun longPressOnResultTogglesAppInCurrentSpace() {
        openSearch()
        val key = rule.runOnIdle { rule.activity.launcher.navigator.search.rows.first().key }
        val before = key in spaceItems()
        rule.onNodeWithTag("result_0").performTouchInput { longClick() }
        rule.onNodeWithTag("menu_info").assertIsDisplayed()
        rule.onNodeWithTag("menu_env").performClick()
        rule.onNodeWithTag("popup").assertDoesNotExist()
        rule.waitUntil(5_000) { (key in spaceItems()) != before }
    }

    @Test
    fun searchMenuHidesAppAndOptionsShowHiddenApps() {
        openSearch()
        rule.onNodeWithTag("result_0").performTouchInput { longClick() }
        val key = rule.activity.launcher.navigator.search.panels.menu!!.key
        val label = rule.activity.launcher.navigator.search.panels.menu!!.label
        rule.onNodeWithTag("menu_hide").performClick()
        try {
            rule.waitUntil(5_000) { container.overrides.overrides.value[key]?.hidden == true }
            rule.waitUntil(5_000) { rule.activity.launcher.navigator.search.rows.none { it.key == key } }
            rule.onNodeWithTag("search_options").performClick()
            rule.onNodeWithTag("search_option_show_hidden").performClick()
            rule.waitUntil(5_000) {
                rule.activity.launcher.navigator.search.rows.any {
                    it.key == key && it.hidden &&
                        it.label == label
                }
            }
            rule.onNodeWithTag("search_option_show_hidden").performClick()
            rule.waitUntil(5_000) { rule.activity.launcher.navigator.search.rows.none { it.key == key } }
        } finally {
            runBlocking { container.overrides.setHidden(key, false) }
        }
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
    fun searchMenuRenamesTheApp() {
        openSearch()
        rule.onNodeWithTag("result_0").performTouchInput { longClick() }
        val key = rule.activity.launcher.navigator.search.panels.menu!!.key
        rule.onNodeWithTag("menu_rename").performClick()
        rule.onNodeWithTag("text_input").assertIsDisplayed()
        assertThat(rule.activity.launcher.navigator.renameTarget).isEqualTo(key)
        Espresso.pressBack()
    }

    @Test
    fun appBoundToASystemGestureIsMarkedInSearchAndOnHome() {
        val key = spaceItems().first()
        openSearch()
        val label = rule.runOnIdle { rule.activity.launcher.navigator.search.rows.first { it.key == key }.label }
        rule.runOnIdle { rule.activity.launcher.searchInput.openMenu(key, label, Offset(100f, 100f)) }
        rule.onNodeWithTag("menu_shortcut").performClick()
        rule.waitUntil(5_000) { container.overrides.overrides.value[key]?.systemShortcut == true }
        rule.waitUntil(5_000) { rule.activity.launcher.navigator.search.rows.first { it.key == key }.shortcut }
        Espresso.pressBack()
        rule.onNodeWithTag("swipe_0", useUnmergedTree = true).assertIsDisplayed()
        runBlocking { container.overrides.setSystemShortcut(key, false) }
    }

    @Test
    fun searchOptionsTurnOnColouredIcons() {
        openSearch()
        rule.onNodeWithTag("search_options").performClick()
        rule.onNodeWithTag("search_option_COLORED_ICONS").performClick()
        rule.waitUntil(5_000) { container.settings.settings.value.searchColoredIcons }
    }
}
