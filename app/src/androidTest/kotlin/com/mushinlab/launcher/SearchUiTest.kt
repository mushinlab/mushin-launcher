package com.mushinlab.launcher

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.feature.settings.SettingsRoute
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchUiTest : LauncherUiTestBase() {
    @Test
    fun swipeDownOnResultsAtTheEndOfTheListClosesSearch() {
        openSearch()
        val label = rule.runOnIdle { rule.activity.launcher.navigator.search.rows.first().label }
        rule.runOnIdle { rule.activity.launcher.navigator.search.replaceQuery(label.lowercase()) }
        rule.onNodeWithTag("results").performTouchInput {
            swipeDown(startY = top + height * 0.2f, endY = top + height * 0.7f, durationMillis = 200)
        }
        rule.runOnIdle { assertThat(rule.activity.launcher.navigator.overlay).isEqualTo(Overlay.None) }
    }

    @Test
    fun emptyQueryShowsARecentAppBeforeAlphabeticalOnesUntilHistoryIsCleared() {
        runBlocking { container.history.clearRecents(normalId) }
        openSearch()
        rule.waitUntil(5_000) { rule.activity.launcher.navigator.search.rows.size >= 2 }
        val last = rule.runOnIdle { rule.activity.launcher.navigator.search.rows.last().key }
        try {
            runBlocking { container.history.recordLaunch(last, normalId, fromSearch = true) }
            rule.waitUntil(5_000) { rule.activity.launcher.navigator.search.rows.first().key == last }
            val rows = rule.runOnIdle { rule.activity.launcher.navigator.search.rows }
            assertThat(rows.first().recent).isTrue()
            assertThat(rows.drop(1).none { it.recent || it.key == last }).isTrue()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag("recents_divider").fetchSemanticsNodes().isNotEmpty() }
            val recent = rule.onNodeWithTag("result_0").fetchSemanticsNode().boundsInRoot
            val divider = rule.onNodeWithTag("recents_divider").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val next = rule.onNodeWithTag("result_1").fetchSemanticsNode().boundsInRoot
            val results = rule.onNodeWithTag("results").fetchSemanticsNode().boundsInRoot
            assertThat(recent.width).isWithin(2f).of(results.width)
            assertThat(divider.bottom).isAtMost(recent.top + 1f)
            assertThat(next.bottom).isAtMost(divider.top + 1f)
            runBlocking { container.history.clearRecents(normalId) }
            rule.waitUntil(5_000) { rule.activity.launcher.navigator.search.rows.none { it.recent } }
            rule.onNodeWithTag("recents_divider").assertDoesNotExist()
        } finally {
            runBlocking { container.history.clearRecents(normalId) }
        }
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
        val results = rule.onNodeWithTag("results").fetchSemanticsNode().boundsInRoot
        assertThat(options.bottom).isAtMost(header.bottom + 1f)
        assertThat(options.bottom).isAtMost(results.top)
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
    fun searchMenuHasNoSystemGestureItem() {
        val key = spaceItems().first()
        openSearch()
        val label = rule.runOnIdle { rule.activity.launcher.navigator.search.rows.first { it.key == key }.label }
        rule.runOnIdle { rule.activity.launcher.searchInput.openMenu(key, label, Offset(100f, 100f)) }
        rule.onNodeWithTag("menu_hide").assertIsDisplayed()
        rule.onNodeWithTag("menu_shortcut").assertDoesNotExist()
        Espresso.pressBack()
    }

    @Test
    fun searchOptionsTurnOnColouredIcons() {
        openSearch()
        rule.onNodeWithTag("search_options").performClick()
        rule.onNodeWithTag("search_option_COLORED_ICONS").performClick()
        rule.waitUntil(5_000) { container.settings.settings.value.searchColoredIcons }
    }

    @Test
    fun allSettingsInSearchOptionsOpensSettingsAndClosesSearch() {
        openSearch()
        rule.onNodeWithTag("search_options").performClick()
        rule.onNodeWithTag("search_all_settings").performClick()
        rule.waitUntil(5_000) { rule.activity.launcher.navigator.settings.top == SettingsRoute.Root }
        assertThat(rule.activity.launcher.navigator.overlay).isEqualTo(Overlay.None)
        rule.onNodeWithTag("nav_style").assertIsDisplayed()
    }
}
