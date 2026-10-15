package com.mushinlab.launcher

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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.SpaceType
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
        rule.onNodeWithTag("home_edit").performClick()
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
    fun editButtonOpensTheCurrentSpace() {
        rule.onNodeWithTag("home_edit").performClick()
        rule.waitUntil(5_000) { rule.activity.launcher.navigator.settings.top == SettingsRoute.SpaceEditor(normalId) }
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
