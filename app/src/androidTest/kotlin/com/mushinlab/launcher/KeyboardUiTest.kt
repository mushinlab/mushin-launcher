package com.mushinlab.launcher

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.feature.search.KeyboardDimens
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyboardUiTest : LauncherUiTestBase() {
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
    fun bottomRowHasResizeAndSpaceOnly() {
        openSearch()
        val resize = rule.onNodeWithTag("key_RESIZE").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val space = rule.onNodeWithTag("key_SPACE").fetchSemanticsNode().boundsInRoot
        val keyboard = rule.onNodeWithTag("keyboard").fetchSemanticsNode().boundsInRoot
        assertThat(resize.right).isLessThan(space.left)
        assertThat(keyboard.right - space.right).isLessThan(20f)
        rule.onNodeWithTag("key_ENTER").assertDoesNotExist()
        rule.onNodeWithTag("enter").assertDoesNotExist()
        rule.onNodeWithTag("query_clear").assertDoesNotExist()
        rule.onNodeWithTag("result_count").assertDoesNotExist()
    }

    @Test
    fun swipeDownAboveTheKeyboardClosesSearchButDrawingDownFromAKeyDoesNot() {
        openSearch()
        rule.onNodeWithTag("key_y").performTouchInput {
            down(center)
            repeat(STEPS) { i -> moveTo(center + Offset(0f, 400f * (i + 1) / STEPS)) }
            up()
        }
        rule.runOnIdle { assertThat(rule.activity.launcher.navigator.overlay).isEqualTo(Overlay.Search) }
        rule.onNodeWithTag("query_row").performTouchInput {
            swipeDown(startY = centerY, endY = centerY + 600f, durationMillis = 200)
        }
        rule.runOnIdle { assertThat(rule.activity.launcher.navigator.overlay).isEqualTo(Overlay.None) }
    }

    @Test
    fun spaceRowIsHalfAsTallAndKeyboardSitsAKeyAboveTheBottom() {
        openSearch()
        val q = rule.onNodeWithTag("key_q").fetchSemanticsNode().boundsInRoot
        val space = rule.onNodeWithTag("key_SPACE").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val keyboard = rule.onNodeWithTag("keyboard").fetchSemanticsNode().boundsInRoot
        val search = rule.onNodeWithTag("search").fetchSemanticsNode().boundsInRoot
        val navBar = ViewCompat.getRootWindowInsets(rule.activity.window.decorView)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        with(rule.density) {
            assertThat(space.height + KeyboardDimens.gap.toPx())
                .isWithin(2f).of((q.height + KeyboardDimens.gap.toPx()) / 2)
            assertThat(search.bottom - navBar - keyboard.bottom).isWithin(2f).of(KeyboardDimens.rowHeight.toPx())
        }
        rule.onNodeWithTag("key_s").performClick()
        rule.onNodeWithTag("key_SPACE").performClick()
        rule.onNodeWithTag("query", useUnmergedTree = true).assertTextEquals("s ")
    }

    @Test
    fun drawingAcrossTheKeyboardIsDecodedAndTappingStillTypes() {
        openSearch()
        rule.onNodeWithTag("input_toggle").assertDoesNotExist()
        rule.onNodeWithTag("key_c").performClick()
        rule.onNodeWithTag("query", useUnmergedTree = true).assertTextEquals("c")
        clearQuery()
        rule.onNodeWithTag("query", useUnmergedTree = true).assertTextEquals("")
        drawOnKeyboard("maps")
        rule.onNodeWithTag("query", useUnmergedTree = true).assertTextEquals("maps")
        val trail = rule.onNodeWithTag("glide_trail").fetchSemanticsNode().boundsInRoot
        val keyboard = rule.onNodeWithTag("keyboard").fetchSemanticsNode().boundsInRoot
        assertThat(trail).isEqualTo(keyboard)
        clearQuery()
        rule.onNodeWithTag("query", useUnmergedTree = true).assertTextEquals("")
    }

    @Test
    fun gestureLikeSeveralAppsShowsThemAllAsSuggestions() {
        val twins = runBlocking { container.catalog.apps.first().sortedBy { it.label }.take(2).map { it.key } }
        runBlocking {
            container.overrides.rename(twins[0], "Zebra One")
            container.overrides.rename(twins[1], "Zebra Two")
        }
        try {
            openSearch()
            rule.waitUntil(5_000) {
                rule.activity.launcher.navigator.search.rows.count { it.label.startsWith("Zebra") } ==
                    2
            }
            drawOnKeyboard("zebra")
            rule.onNodeWithTag("suggestion_0").assertIsDisplayed()
            rule.onNodeWithTag("suggestion_1").assertIsDisplayed()
            rule.onNodeWithTag("query", useUnmergedTree = true).assertDoesNotExist()
            assertThat(
                rule.activity.launcher.navigator.search.rows.map {
                    it.label
                },
            ).containsExactly("Zebra One", "Zebra Two")
            clearQuery()
            rule.onNodeWithTag("query", useUnmergedTree = true).assertTextEquals("")
            assertThat(rule.activity.launcher.navigator.search.rows.size).isGreaterThan(2)
        } finally {
            runBlocking { twins.forEach { container.overrides.rename(it, null) } }
        }
    }

    @Test
    fun keyboardWidthAndShiftMoveTheWholeInputPanel() {
        runBlocking { container.settings.update { it.copy(keyboardWidth = 0.6f, keyboardShift = 1f) } }
        openSearch()
        rule.waitForIdle()
        val pane = rule.onNodeWithTag("input_pane").fetchSemanticsNode().boundsInRoot
        val keyboard = rule.onNodeWithTag("keyboard").fetchSemanticsNode().boundsInRoot
        val results = rule.onNodeWithTag("recents").fetchSemanticsNode().boundsInRoot
        val full = results.right - rule.onNodeWithTag("results").fetchSemanticsNode().boundsInRoot.left
        assertThat(pane.width).isWithin(3f).of(full * 0.6f)
        assertThat(pane.right).isWithin(3f).of(results.right)
        assertThat(keyboard.width).isWithin(2f).of(pane.width)
    }

    @Test
    fun resizeKeyShowsAnchorsAndDraggingASideAnchorNarrowsTheKeyboard() {
        openSearch()
        rule.onNodeWithTag("key_RESIZE").performClick()
        rule.onNodeWithTag("resize_overlay").assertIsDisplayed().performTouchInput {
            val inset = 10.dp.toPx()
            down(Offset(left + inset, centerY))
            repeat(STEPS) { i -> moveTo(Offset(left + inset + 200f * (i + 1) / STEPS, centerY)) }
            up()
        }
        rule.waitUntil(5_000) { container.settings.settings.value.keyboardWidth < 0.9f }
        rule.runOnIdle { assertThat(container.settings.settings.value.keyboardShift).isEqualTo(1f) }
        rule.onNodeWithTag("resize_overlay").performTouchInput { click(Offset(centerX, top + 4f)) }
        rule.onNodeWithTag("resize_overlay").assertDoesNotExist()
    }

    @Test
    fun deleteKeyIsACrossAfterADrawingAndBackspaceWhileTyping() {
        openSearch()
        drawOnKeyboard("maps")
        rule.onNodeWithText("×", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("key_BACKSPACE").performClick()
        rule.onNodeWithTag("query", useUnmergedTree = true).assertTextEquals("")
        rule.onNodeWithTag("key_m").performClick()
        rule.onNodeWithText("⌫", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun digitBubbleIsNotCutByTheQueryRow() {
        openSearch()
        rule.onNodeWithTag("key_u").performTouchInput { down(center) }
        rule.waitUntil(2_000) { runCatching { rule.onNodeWithTag("digit_bubble").assertExists() }.isSuccess }
        val bubble = rule.onNodeWithTag("digit_bubble").fetchSemanticsNode().boundsInRoot
        val keyboard = rule.onNodeWithTag("keyboard").fetchSemanticsNode().boundsInRoot
        assertThat(bubble.top).isLessThan(keyboard.top)
        rule.onNodeWithTag("digit_bubble").assertIsDisplayed()
        rule.onNodeWithTag("key_u").performTouchInput { up() }
    }

    @Test
    fun suggestionIconsHaveEqualGapsAboveAndBelow() {
        openSearch()
        rule.runOnIdle { rule.activity.launcher.navigator.search.suggest(spaceItems().take(2)) }
        val icon = rule.onNodeWithTag("suggestion_0").fetchSemanticsNode().boundsInRoot
        val pane = rule.onNodeWithTag("input_pane").fetchSemanticsNode().boundsInRoot
        val q = rule.onNodeWithTag("key_q").fetchSemanticsNode().boundsInRoot
        assertThat(icon.top - pane.top).isWithin(2f).of(q.top - icon.bottom)
    }
}
