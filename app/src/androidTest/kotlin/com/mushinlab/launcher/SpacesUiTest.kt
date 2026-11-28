package com.mushinlab.launcher

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.data.repo.SpaceOutcome
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.feature.settings.R as SettingsR
import com.mushinlab.launcher.feature.settings.SettingsRoute
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpacesUiTest : LauncherUiTestBase() {
    @Test
    fun pickerAppliesEachTapWithoutDone() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.SpaceEditor(normalId)) }
        rule.onNodeWithTag("env_add").performScrollTo().performClick()
        val app = runBlocking { container.catalog.apps.first() }.first { it.key !in spaceItems() }
        rule.onNodeWithTag("app_picker").assertIsDisplayed()
        val list = rule.onNodeWithTag("picker_list").fetchSemanticsNode().boundsInRoot
        val input = rule.onNodeWithTag("picker_input").fetchSemanticsNode().boundsInRoot
        assertThat(input.top).isAtLeast(list.bottom)
        rule.onNodeWithTag("keyboard").assertIsDisplayed()
        rule.onNodeWithTag("key_SPACE").assertIsDisplayed()
        rule.onNodeWithTag("key_RESIZE").assertDoesNotExist()
        app.label.lowercase().filter { it in 'a'..'z' }.take(4).forEach { rule.onNodeWithTag("key_$it").performClick() }
        rule.onNodeWithTag("pick_${app.label}").performClick()
        rule.waitUntil(5_000) { app.key in spaceItems() }
        rule.onNodeWithTag("pick_${app.label}").assertIsSelected()
        rule.onNodeWithTag("pick_${app.label}").performClick()
        rule.waitUntil(5_000) { app.key !in spaceItems() }
        rule.onNodeWithTag("pick_${app.label}").assertIsNotSelected()
    }

    @Test
    fun holdingASpaceInThePopupForOneSecondOpensItsSettings() {
        rule.onNodeWithTag("home_bar").performClick()
        rule.onNodeWithTag("popup_env_$normalId").performTouchInput { longClick(durationMillis = 1_200) }
        rule.waitUntil(5_000) { rule.activity.launcher.navigator.settings.top == SettingsRoute.SpaceEditor(normalId) }
        assertThat(rule.activity.launcher.navigator.overlay).isEqualTo(Overlay.None)
    }

    @Test
    fun colouredIconsAreToggledInTheSpaceSettings() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.SpaceEditor(normalId)) }
        rule.onNodeWithTag("env_colored_icons").performScrollTo().performClick()
        rule.waitUntil(5_000) { container.spaces.spaces.value.first { it.id == normalId }.coloredIcons }
        rule.onNodeWithTag("env_colored_icons").performClick()
        rule.waitUntil(5_000) { !container.spaces.spaces.value.first { it.id == normalId }.coloredIcons }
    }

    @Test
    fun editButtonTogglesEditModeWithItsThreeTools() {
        val navigator = rule.activity.launcher.navigator
        rule.onNodeWithTag("home_edit_tools").assertDoesNotExist()
        val button = { rule.onNodeWithTag("home_edit").fetchSemanticsNode().boundsInRoot.width }
        val small = button()
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("home_edit_tools").assertIsDisplayed()
        rule.waitUntil(5_000) { button() > small }
        assertThat(navigator.settings.isOpen).isFalse()
        rule.onNodeWithTag("home_edit_text").performClick()
        rule.waitUntil(5_000) { navigator.settings.top == SettingsRoute.TextSheet }
        rule.onNodeWithTag("text_sheet").assertIsDisplayed()
        rule.runOnIdle { navigator.handleBack() }
        rule.onNodeWithTag("home_edit_theme").performClick()
        rule.waitUntil(5_000) { navigator.settings.top == SettingsRoute.ThemeSheet }
        rule.onNodeWithTag("theme_sheet").assertIsDisplayed()
        rule.runOnIdle { navigator.handleBack() }
        rule.waitUntil(5_000) { !navigator.settings.isOpen }
        assertThat(navigator.homeEditing).isTrue()
        rule.onNodeWithTag("home_edit").performClick()
        rule.waitUntil(5_000) {
            runCatching { rule.onNodeWithTag("home_edit_tools").assertDoesNotExist() }.isSuccess
        }
        assertThat(navigator.homeEditing).isFalse()
    }

    @Test
    fun AC_57_1_spacesHaveTheirOwnSettingsScreenAtTheTopOfSettings() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open() }
        val spacesRow = rule.onNodeWithTag("nav_spaces").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val keyboardRow = rule.onNodeWithTag("nav_keyboard").fetchSemanticsNode().boundsInRoot
        assertThat(spacesRow.top).isLessThan(keyboardRow.top)
        rule.onNodeWithTag("space_chips").assertDoesNotExist()
        rule.onNodeWithTag("nav_spaces").performClick()
        rule.waitUntil(5_000) { rule.activity.launcher.navigator.settings.top == SettingsRoute.Spaces }
        val work = container.spaces.spaces.value.first { it.type == SpaceType.WORK }.id
        val normalTop = rule.onNodeWithTag("space_row_$normalId").fetchSemanticsNode().boundsInRoot.top
        val workTop = rule.onNodeWithTag("space_row_$work").fetchSemanticsNode().boundsInRoot.top
        assertThat(normalTop).isLessThan(workTop)
        rule.onNodeWithTag("space_rename_$work").assertIsDisplayed()
        rule.onNodeWithTag("space_delete_$work").assertIsDisplayed()
    }

    @Test
    fun addRowCreatesASpaceThatAppearsInTheList() {
        val name = "New ${System.currentTimeMillis() % 100_000}"
        openSpaces()
        rule.onNodeWithTag("settings_add_space").performScrollTo().performClick()
        rule.onNodeWithTag("space_type_WORK").assertDoesNotExist()
        rule.onNodeWithTag("text_input").performTextInput(name)
        rule.onNodeWithTag("dialog_button_1").performClick()
        rule.waitUntil(5_000) { container.spaces.spaces.value.any { it.name == name } }
        val created = container.spaces.spaces.value.first { it.name == name }
        try {
            assertThat(created.type).isEqualTo(SpaceType.CUSTOM)
            rule.onNodeWithTag("dialog").assertDoesNotExist()
            rule.onNodeWithTag("space_row_${created.id}").performScrollTo().assert(hasAnyDescendant(hasText(name)))
            rule.onNodeWithTag("settings_add_space").performScrollTo().assertIsDisplayed()
        } finally {
            runBlocking { container.spaces.delete(created.id) }
        }
    }

    @Test
    fun deleteIconRemovesTheSpaceAfterTheSystemDialog() {
        val id = createCustomSpace("Doomed ${System.currentTimeMillis() % 100_000}")
        try {
            openSpaces()
            rule.onNodeWithTag("space_delete_$id").performScrollTo().performClick()
            assertThat(container.spaces.spaces.value.any { it.id == id }).isTrue()
            onView(withText(rule.activity.getString(SettingsR.string.settings_delete)))
                .inRoot(isDialog())
                .perform(click())
            rule.waitUntil(5_000) { container.spaces.spaces.value.none { it.id == id } }
            rule.onNodeWithTag("space_row_$id").assertDoesNotExist()
        } finally {
            runBlocking { container.spaces.delete(id) }
        }
    }

    @Test
    fun cancelInTheSystemDialogKeepsTheSpace() {
        val id = createCustomSpace("Kept ${System.currentTimeMillis() % 100_000}")
        try {
            openSpaces()
            rule.onNodeWithTag("space_delete_$id").performScrollTo().performClick()
            onView(withText(rule.activity.getString(SettingsR.string.settings_cancel)))
                .inRoot(isDialog())
                .perform(click())
            rule.waitForIdle()
            assertThat(container.spaces.spaces.value.any { it.id == id }).isTrue()
            rule.onNodeWithTag("space_row_$id").assertExists()
        } finally {
            runBlocking { container.spaces.delete(id) }
        }
    }

    @Test
    fun AC_49_2_normalSpaceCanBeRenamedButNotDeleted() {
        val original = container.spaces.spaces.value.first { it.id == normalId }.name
        openSpaces()
        rule.onNodeWithTag("space_delete_$normalId").assertDoesNotExist()
        try {
            rule.onNodeWithTag("space_rename_$normalId").performClick()
            rule.onNodeWithTag("text_input").assertTextEquals(original).performTextReplacement("Renamed default")
            rule.onNodeWithTag("dialog_button_1").performClick()
            rule.waitUntil(5_000) {
                container.spaces.spaces.value.any {
                    it.id == normalId &&
                        it.name == "Renamed default"
                }
            }
            rule.onNodeWithTag("space_row_$normalId").assert(hasAnyDescendant(hasText("Renamed default")))
            rule.onNodeWithTag("dialog").assertDoesNotExist()
        } finally {
            runBlocking { container.spaces.rename(normalId, original) }
        }
    }

    @Test
    fun AC_49_2_spaceRowHasTwoIconsAndRenameRejectsDuplicates() {
        val id = createCustomSpace("Rename me")
        val duplicate = container.spaces.spaces.value.first { it.id == normalId }.name
        try {
            openSpaces()
            rule.onNodeWithTag("space_row_$id").performScrollTo()
            val delete = rule.onNodeWithTag("space_delete_$id").fetchSemanticsNode().boundsInRoot
            val rename = rule.onNodeWithTag("space_rename_$id").fetchSemanticsNode().boundsInRoot
            assertThat(rename.center.y).isWithin(1f).of(delete.center.y)
            assertThat(delete.left).isAtLeast(rename.right)
            rule.onNodeWithTag("space_rename_$id").performClick()
            rule.onNodeWithTag("text_input").performTextReplacement(duplicate)
            rule.onNodeWithTag("dialog_button_1").performClick()
            rule.onNodeWithText(rule.activity.getString(SettingsR.string.error_name_taken)).assertIsDisplayed()
            rule.onNodeWithTag("text_input").performTextReplacement("Renamed space")
            rule.onNodeWithTag("dialog_button_1").performClick()
            rule.waitUntil(5_000) { container.spaces.spaces.value.any { it.id == id && it.name == "Renamed space" } }
            rule.onNodeWithTag("dialog").assertDoesNotExist()
            rule.onNodeWithTag("space_row_$id").performScrollTo().assert(hasAnyDescendant(hasText("Renamed space")))
        } finally {
            runBlocking { container.spaces.delete(id) }
        }
    }

    private fun openSpaces() {
        rule.runOnIdle {
            rule.activity.launcher.navigator.settings.open()
            rule.activity.launcher.navigator.settings.push(SettingsRoute.Spaces)
        }
        rule.onNodeWithTag("settings_add_space").assertExists()
    }

    private fun createCustomSpace(name: String): Long {
        val outcome = runBlocking { container.spaces.create(name, SpaceType.CUSTOM, PalettePreset.MOSS) }
        val id = (outcome as SpaceOutcome.Ok).value
        rule.waitUntil(5_000) { container.spaces.spaces.value.any { it.id == id } }
        return id
    }

    @Test
    fun eachSpaceKeepsItsOwnListPosition() {
        val work = container.spaces.spaces.value.first { it.type == SpaceType.WORK }.id
        val top = { rule.onNodeWithTag("list_handle").fetchSemanticsNode().boundsInRoot.top }
        val start = top()
        runBlocking { container.spaces.setListOffset(normalId, 100) }
        rule.waitUntil(5_000) { top() - start > 50f }
        val moved = top()
        runBlocking { container.settings.update { it.copy(currentSpaceId = work) } }
        rule.waitUntil(5_000) { abs(top() - start) < 2f }
        runBlocking { container.settings.update { it.copy(currentSpaceId = normalId) } }
        rule.waitUntil(5_000) { abs(top() - moved) < 2f }
    }

    @Test
    fun popupListsSpacesAsVerticalRadioButtons() {
        rule.onNodeWithTag("home_bar").performClick()
        val work = container.spaces.spaces.value.first { it.type == SpaceType.WORK }.id
        rule.onNodeWithTag("popup_env_$normalId").assertIsSelected()
        rule.onNodeWithTag("popup_env_$work").assertIsNotSelected()
        val first = rule.onNodeWithTag("popup_env_$normalId").fetchSemanticsNode().boundsInRoot
        val second = rule.onNodeWithTag("popup_env_$work").fetchSemanticsNode().boundsInRoot
        assertThat(second.top).isAtLeast(first.bottom - 1f)
    }

    @Test
    fun everySettingsPageShowsTheCurrentSpaceName() {
        val name = container.spaces.spaces.value.first { it.id == normalId }.name
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open() }
        rule.onNodeWithTag("settings_space", useUnmergedTree = true).assertTextEquals(name)
        rule.onNodeWithTag("nav_keyboard").performClick()
        rule.onNodeWithTag("settings_space", useUnmergedTree = true).assertTextEquals(name)
    }
}
