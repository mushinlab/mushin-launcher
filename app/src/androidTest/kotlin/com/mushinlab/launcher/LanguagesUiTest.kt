package com.mushinlab.launcher

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.feature.settings.SettingsRoute
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanguagesUiTest : LauncherUiTestBase() {
    private fun hasText(text: String): Boolean = rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun pickLanguage(tag: String) {
        rule.onNodeWithTag("system_language_dropdown").performClick()
        rule.onNodeWithTag("system_language_$tag").performScrollTo().performClick()
        rule.waitUntil(5_000) { rule.activity.launcher.appLanguage() == tag }
    }

    @Test
    fun languageIsTheSecondSystemItemAndSwitchesTheInterfaceAtOnce() {
        try {
            rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.System) }
            val first = rule.onNodeWithTag("system_default").fetchSemanticsNode().boundsInRoot
            val language = rule.onNodeWithTag("system_language_dropdown").assertIsDisplayed()
                .fetchSemanticsNode().boundsInRoot
            assertThat(language.top).isAtLeast(first.bottom)
            assertThat(language.top - first.bottom).isLessThan(with(rule.density) { 16.dp.toPx() })

            pickLanguage("ru")
            rule.waitUntil(5_000) { hasText("Язык") && !hasText("Language") }
            rule.onNodeWithTag("system_language_dropdown").assertIsDisplayed()

            pickLanguage("en")
            rule.waitUntil(5_000) { hasText("Language") && !hasText("Язык") }
            rule.runOnIdle {
                assertThat(rule.activity.launcher.navigator.settings.top).isEqualTo(SettingsRoute.System)
            }

            rule.onNodeWithTag("system_language_dropdown").performClick()
            rule.onNodeWithTag("system_language_null").performClick()
            rule.waitUntil(5_000) { rule.activity.launcher.appLanguage() == null }
        } finally {
            rule.activity.launcher.setAppLanguage(null)
        }
    }

    @Test
    fun keyboardLayoutsAreAListWithAddMoreAndRemoveAndTheKeyCyclesInListOrder() {
        try {
            rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.Keyboard) }
            rule.onNodeWithTag("layout_en").assertIsDisplayed()
            rule.onNodeWithTag("layout_ru").assertIsDisplayed()
            rule.onNodeWithTag("layout_de").assertDoesNotExist()
            rule.onNodeWithTag("layout_add_dropdown").performScrollTo().performClick()
            rule.onNodeWithTag("layout_add_en").assertDoesNotExist()
            rule.onNodeWithTag("layout_add_de").performScrollTo().performClick()
            rule.waitUntil(5_000) { container.settings.settings.value.keyboardLayouts == listOf("en", "ru", "de") }
            rule.onNodeWithTag("layout_de").performScrollTo().assertIsDisplayed()

            rule.runOnIdle { rule.activity.launcher.navigator.closeAll() }
            openSearch()
            listOf(
                Triple("en", 'q', "RU"),
                Triple("ru", 'й', "DE"),
                Triple("de", 'ü', "EN"),
            ).forEach { (active, letter, nextLabel) ->
                rule.waitUntil(5_000) { container.settings.settings.value.activeKeyboardLayout == active }
                rule.onNodeWithTag("key_$letter").assertIsDisplayed()
                rule.onNodeWithTag("key_SWITCH_LAYOUT", useUnmergedTree = true).onChild().assertTextEquals(nextLabel)
                rule.onNodeWithTag("key_SWITCH_LAYOUT").performClick()
            }
            rule.waitUntil(5_000) { container.settings.settings.value.activeKeyboardLayout == "en" }
            rule.onNodeWithTag("key_q").assertIsDisplayed()

            rule.runOnIdle { rule.activity.launcher.navigator.openSettings(SettingsRoute.Keyboard) }
            rule.onNodeWithTag("layout_remove_ru").performClick()
            rule.waitUntil(5_000) { container.settings.settings.value.keyboardLayouts == listOf("en", "de") }
            rule.onNodeWithTag("layout_ru").assertDoesNotExist()
            rule.onNodeWithTag("layout_remove_de").performClick()
            rule.waitUntil(5_000) { container.settings.settings.value.keyboardLayouts == listOf("en") }
            rule.onNodeWithTag("layout_remove_en").assertIsNotEnabled()
        } finally {
            runBlocking {
                container.settings.update { it.copy(keyboardLayouts = listOf("en", "ru"), activeKeyboardLayout = "en") }
            }
        }
    }
}
