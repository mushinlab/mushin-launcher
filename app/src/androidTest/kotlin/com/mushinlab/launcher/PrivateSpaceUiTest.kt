package com.mushinlab.launcher

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.SpaceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrivateSpaceUiTest : LauncherUiTestBase() {
    @Before
    fun resetPrivate() = runBlocking {
        container.settings.update {
            it.copy(currentSpaceId = normalId, privatePinSalt = null, privatePinHash = null, privateHintSeen = true)
        }
        rule.waitUntil(5_000) { container.spaces.spaces.value.any { it.type == SpaceType.PRIVATE } }
    }

    @Test
    fun AC_53_1_swipingUpOnThePopupSetsThePinAndOpensThePrivateSpace() {
        openPrivateWithSwipe()
        rule.onNodeWithTag("private_pin").assertIsDisplayed()
        enterPin(1, 2, 3, 4)
        rule.onNodeWithTag("private_pin").assertIsDisplayed()
        enterPin(1, 2, 3, 4)
        rule.waitUntil(10_000) { container.settings.settings.value.currentSpaceId == privateSpace.id }
        rule.onNodeWithTag("private_pin").assertDoesNotExist()
        rule.onNodeWithTag("home_space", useUnmergedTree = true).assertTextEquals(privateSpace.name)
        assertThat(container.settings.settings.value.privatePinHash).isNotNull()
    }

    @Test
    fun AC_53_2_privateAppsStayOutOfListsAndSearchUntilTheSpaceIsOpen() {
        val key = spaceItems().first()
        runBlocking { container.spaces.setItems(privateSpace.id, listOf(key)) }
        rule.waitUntil(5_000) { rule.activity.launcher.home.value.rows.none { it.key == key } }
        rule.waitUntil(5_000) { rule.activity.launcher.searchIndex.value.alphabeticalWithHidden.none { it.key == key } }

        runBlocking { setPin("1234") }
        openPrivateWithSwipe()
        enterPin(1, 2, 3, 4)
        rule.waitUntil(10_000) { container.settings.settings.value.currentSpaceId == privateSpace.id }
        rule.waitUntil(5_000) { rule.activity.launcher.home.value.rows.any { it.key == key } }
        rule.waitUntil(5_000) { rule.activity.launcher.searchIndex.value.alphabeticalWithHidden.any { it.key == key } }
    }

    @Test
    fun AC_53_1_thePrivateSpaceIsNotListedInThePopup() {
        rule.onNodeWithTag("home_bar").performClick()
        rule.onNodeWithTag("popup_env_${privateSpace.id}").assertDoesNotExist()
    }

    @Test
    fun AC_53_4_thePrivateHintNeedsTheCheckbox() {
        runBlocking { container.settings.update { it.copy(privateHintSeen = false) } }
        rule.waitUntil(5_000) {
            runCatching { rule.onNodeWithTag("private_hint").assertIsDisplayed() }.isSuccess
        }
        rule.onNodeWithTag("private_hint_done").performClick()
        rule.onNodeWithTag("private_hint").assertIsDisplayed()
        rule.onNodeWithTag("private_hint_check").performClick()
        rule.onNodeWithTag("private_hint_done").performClick()
        rule.waitUntil(5_000) { container.settings.settings.value.privateHintSeen }
        rule.onNodeWithTag("private_hint").assertDoesNotExist()
    }

    private val privateSpace get() = container.spaces.spaces.value.first { it.type == SpaceType.PRIVATE }

    private fun openPrivateWithSwipe() {
        rule.onNodeWithTag("home_bar").performClick()
        rule.onNodeWithTag("popup").performTouchInput {
            down(center)
            moveBy(Offset(0f, -POPUP_SWIPE), 300)
            up()
        }
    }

    private fun enterPin(vararg digits: Int) {
        digits.forEach { rule.onNodeWithTag("private_pin_$it").performClick() }
    }

    private suspend fun setPin(pin: String) {
        val (salt, hash) = PrivatePin.create(pin)
        container.settings.update { it.copy(privatePinSalt = salt, privatePinHash = hash) }
        container.settings.settings.first { it.privatePinHash == hash }
    }

    private companion object {
        const val POPUP_SWIPE = 600f
    }
}
