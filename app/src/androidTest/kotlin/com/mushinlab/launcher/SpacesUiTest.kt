package com.mushinlab.launcher

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso
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
        rule.onNodeWithTag("picker_filter").performTextInput(app.label)
        rule.onNodeWithTag("pick_${app.label}").performClick()
        rule.waitUntil(5_000) { app.key in spaceItems() }
        rule.onNodeWithTag("pick_${app.label}").assertIsSelected()
        rule.onNodeWithTag("pick_${app.label}").performClick()
        rule.waitUntil(5_000) { app.key !in spaceItems() }
        rule.onNodeWithTag("pick_${app.label}").assertIsNotSelected()
        Espresso.closeSoftKeyboard()
    }

    @Test
    fun holdingASpaceInThePopupForOneSecondOpensItsSettings() {
        backgroundPoint().performTouchInput { longClick(bottomCenter.copy(y = bottom - height * 0.1f)) }
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
        rule.waitUntil(5_000) { abs(button() - small * DONE_SCALE_NUMERATOR / DONE_SCALE_DENOMINATOR) < 2f }
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
    fun settingsRootListsSpacesAsRadioRowWithCurrentFirst() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open() }
        val work = container.spaces.spaces.value.first { it.type == SpaceType.WORK }.id
        rule.onNodeWithTag("space_chip_$normalId").assertIsSelected()
        val normalLeft = rule.onNodeWithTag("space_chip_$normalId").fetchSemanticsNode().boundsInRoot.left
        val workLeft = rule.onNodeWithTag("space_chip_$work").fetchSemanticsNode().boundsInRoot.left
        assertThat(normalLeft).isLessThan(workLeft)
        rule.onNodeWithTag("space_chip_$work").assertIsNotSelected().performClick()
        rule.waitUntil(5_000) { container.settings.settings.value.currentSpaceId == work }
        rule.onNodeWithTag("space_chip_$work").assertIsSelected()
        rule.onNodeWithTag("nav_search").assertDoesNotExist()
    }

    @Test
    fun plusButtonCreatesASpaceThatAppearsAsAChip() {
        val name = "New ${System.currentTimeMillis() % 100_000}"
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open() }
        rule.onNodeWithTag("settings_add_space").assertIsDisplayed().performClick()
        rule.onNodeWithTag("space_type_WORK").assertDoesNotExist()
        rule.onNodeWithTag("text_input").performTextInput(name)
        rule.onNodeWithTag("dialog_button_1").performClick()
        rule.waitUntil(5_000) { container.spaces.spaces.value.any { it.name == name } }
        val created = container.spaces.spaces.value.first { it.name == name }
        try {
            assertThat(created.type).isEqualTo(SpaceType.CUSTOM)
            rule.onNodeWithTag("dialog").assertDoesNotExist()
            rule.onNodeWithTag("space_chip_${created.id}").performScrollTo().assertIsDisplayed().assertIsNotSelected()
            rule.onNodeWithTag("settings_add_space").assertIsDisplayed()
        } finally {
            runBlocking { container.spaces.delete(created.id) }
        }
    }

    @Test
    fun longPressOnASpaceChipDeletesItAfterTheSystemDialog() {
        val id = createCustomSpace("Doomed ${System.currentTimeMillis() % 100_000}")
        try {
            rule.runOnIdle { rule.activity.launcher.navigator.settings.open() }
            rule.onNodeWithTag("space_chip_$id").performScrollTo().performTouchInput { longClick() }
            rule.onNodeWithTag("popup").assertIsDisplayed()
            rule.onNodeWithTag("space_delete").assertIsDisplayed().performClick()
            rule.onNodeWithTag("popup").assertDoesNotExist()
            assertThat(container.spaces.spaces.value.any { it.id == id }).isTrue()
            onView(withText(rule.activity.getString(SettingsR.string.settings_delete)))
                .inRoot(isDialog())
                .perform(click())
            rule.waitUntil(5_000) { container.spaces.spaces.value.none { it.id == id } }
            rule.onNodeWithTag("space_chip_$id").assertDoesNotExist()
        } finally {
            runBlocking { container.spaces.delete(id) }
        }
    }

    @Test
    fun cancelInTheSystemDialogKeepsTheSpace() {
        val id = createCustomSpace("Kept ${System.currentTimeMillis() % 100_000}")
        try {
            rule.runOnIdle { rule.activity.launcher.navigator.settings.open() }
            rule.onNodeWithTag("space_chip_$id").performScrollTo().performTouchInput { longClick() }
            rule.onNodeWithTag("space_delete").performClick()
            onView(withText(rule.activity.getString(SettingsR.string.settings_cancel)))
                .inRoot(isDialog())
                .perform(click())
            rule.waitForIdle()
            assertThat(container.spaces.spaces.value.any { it.id == id }).isTrue()
            rule.onNodeWithTag("space_chip_$id").assertExists()
        } finally {
            runBlocking { container.spaces.delete(id) }
        }
    }

    @Test
    fun longPressOnTheNormalSpaceOpensNoPanel() {
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open() }
        rule.onNodeWithTag("space_chip_$normalId").performTouchInput { longClick() }
        rule.waitForIdle()
        rule.onNodeWithTag("popup").assertDoesNotExist()
        rule.onNodeWithTag("space_delete").assertDoesNotExist()
        rule.onNodeWithTag("space_chip_$normalId").assertIsSelected()
        assertThat(container.spaces.spaces.value.any { it.id == normalId }).isTrue()
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
        backgroundPoint().performTouchInput { longClick(bottomCenter.copy(y = bottom - height * 0.1f)) }
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

private const val DONE_SCALE_NUMERATOR = 88f
private const val DONE_SCALE_DENOMINATOR = 48f
