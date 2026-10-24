package com.mushinlab.launcher

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeRight
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.LearnedGesture
import com.mushinlab.launcher.feature.settings.SettingsRoute
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemoryUiTest : LauncherUiTestBase() {
    @Test
    fun memoryShowsLearnedDrawingsPerAppAndForgetsThem() {
        val app = runBlocking { container.catalog.apps.first().sortedBy { it.label }.first() }
        runBlocking {
            container.gestureLearning.update { listOf(LearnedGesture("qwerty", app.key, 2.0)) }
        }
        try {
            rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.Gestures) }
            rule.onNodeWithTag("memory_app_${app.label}").performScrollTo().performClick()
            rule.onNodeWithTag("memory_drawing_0").assertIsDisplayed()
            rule.onNodeWithTag("memory_forget_0").performClick()
            rule.waitUntil(5_000) { container.gestureLearning.samples.value.isEmpty() }
        } finally {
            runBlocking { container.gestureLearning.update { emptyList() } }
        }
    }

    @Test
    fun drawingALearnedShapeOnTheHomeScreenFindsTheAppAtAnotherScale() {
        val key = spaceItems().first()
        runBlocking { container.gestureLearning.update { listOf(LearnedGesture("mapsdfghjkl", key, 1.0)) } }
        rule.waitUntil(5_000) { container.gestureLearning.samples.value.isNotEmpty() }
        drawOnHome("mapsdfghjkl".filterIndexed { i, _ -> i in listOf(0, 1, 2, 3, 10) }, scale = 1.5f)
        rule.waitUntil(5_000) { rule.activity.launcher.navigator.overlay == Overlay.Search }
        rule.runOnIdle {
            assertThat(rule.activity.launcher.navigator.search.suggesting).isTrue()
            assertThat(rule.activity.launcher.navigator.search.rows.map { it.key }).containsExactly(key)
        }
    }

    @Test
    fun fiveUnknownDrawingsAskWhatYouMeantAndOpenLearning() {
        runBlocking { container.gestureLearning.update { emptyList() } }
        repeat(HomeInteractions.LEARN_HINT_MISSES - 1) {
            drawOnHome("qazwsx", scale = 1f)
            rule.onNodeWithTag("learn_hint").assertDoesNotExist()
        }
        drawOnHome("qazwsx", scale = 1f)
        rule.onNodeWithTag("learn_hint").assertIsDisplayed().performClick()
        rule.runOnIdle {
            assertThat(rule.activity.launcher.navigator.settings.top).isEqualTo(SettingsRoute.MemoryPickApp)
        }
    }

    @Test
    fun learningFromMissesOpensTheSheetWithTheAveragePattern() {
        runBlocking { container.gestureLearning.update { emptyList() } }
        repeat(HomeInteractions.LEARN_HINT_MISSES) { drawOnHome("qazwsx", scale = 1f + it * 0.05f) }
        rule.onNodeWithTag("learn_hint").performClick()
        val app = runBlocking { container.catalog.apps.first() }.minBy { it.label }
        rule.runOnIdle { assertThat(rule.activity.launcher.navigator.settings.draft).isNotEmpty() }
        rule.onNodeWithTag("pick_${app.label}").performClick()
        rule.onNodeWithTag("gesture_sheet").assertIsDisplayed()
        rule.onNodeWithTag("sheet_add").performClick()
        rule.waitUntil(5_000) {
            container.gestureLearning.samples.value.any { it.key == app.key && it.shape.size >= 2 }
        }
        rule.runOnIdle { assertThat(rule.activity.launcher.navigator.settings.draft).isNull() }
    }

    @Test
    fun learnHintCloseOffersHideAndNeverSuggest() {
        runBlocking { container.gestureLearning.update { emptyList() } }
        repeat(HomeInteractions.LEARN_HINT_MISSES) { drawOnHome("qazwsx", scale = 1f) }
        rule.onNodeWithTag("learn_hint_close").performClick()
        rule.onNodeWithTag("learn_hint_hide").assertIsDisplayed()
        rule.onNodeWithTag("learn_hint_never").performClick()
        rule.waitUntil(5_000) { !container.settings.settings.value.learnHints }
        rule.onNodeWithTag("learn_hint").assertDoesNotExist()
        repeat(HomeInteractions.LEARN_HINT_MISSES) { drawOnHome("qazwsx", scale = 1f) }
        rule.onNodeWithTag("learn_hint").assertDoesNotExist()
    }

    @Test
    fun withGestureInputOffDrawingOnTheHomeScreenDoesNothing() {
        runBlocking { container.settings.update { it.copy(gestureInput = false) } }
        rule.waitUntil(5_000) { !rule.activity.launcher.home.value.drawing }
        repeat(HomeInteractions.LEARN_HINT_MISSES) { drawOnHome("qazwsx", scale = 1f) }
        rule.onNodeWithTag("learn_hint").assertDoesNotExist()
        assertThat(rule.activity.launcher.navigator.overlay).isEqualTo(Overlay.None)
    }

    @Test
    fun learnHintCanBeSwipedAway() {
        rule.runOnIdle { rule.activity.launcher.homeInput.learnHint = true }
        rule.onNodeWithTag("learn_hint").performTouchInput { swipeRight(startX = left + 10f, endX = right) }
        rule.onNodeWithTag("learn_hint").assertDoesNotExist()
        rule.runOnIdle { assertThat(rule.activity.launcher.navigator.settings.isOpen).isFalse() }
    }

    @Test
    fun memoryAddsAGestureDrawnInTheSheet() {
        runBlocking { container.gestureLearning.update { emptyList() } }
        val app = runBlocking { container.catalog.apps.first() }.minBy { it.label }
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.Gestures) }
        rule.onNodeWithTag("memory_add_gesture").performScrollTo().performClick()
        rule.onNodeWithTag("pick_${app.label}").performClick()
        rule.onNodeWithTag("memory_add").performClick()
        rule.onNodeWithTag("gesture_sheet").assertIsDisplayed()
        rule.onNodeWithTag("sheet_add").performClick()
        rule.onNodeWithTag("gesture_sheet").assertIsDisplayed()
        rule.onNodeWithTag("gesture_pad").performTouchInput {
            down(Offset(width * 0.05f, height / 6f))
            repeat(STEPS) { i -> moveTo(Offset(width * (0.05f + 0.9f * (i + 1) / STEPS), height / 6f)) }
            up()
        }
        rule.onNodeWithTag("sheet_add").performClick()
        rule.onNodeWithTag("gesture_sheet").assertDoesNotExist()
        rule.waitUntil(5_000) {
            container.gestureLearning.samples.value.any {
                it.key == app.key && it.path == "qwertyuiop" && it.shape.size >= 2
            }
        }
        rule.onNodeWithTag("memory_drawing_0").assertIsDisplayed()
    }

    @Test
    fun gestureSheetClosesWhenPulledDown() {
        val app = runBlocking { container.catalog.apps.first() }.first()
        rule.runOnIdle { rule.activity.launcher.navigator.settings.open(SettingsRoute.MemoryApp(app.key)) }
        rule.onNodeWithTag("memory_add").performClick()
        rule.onNodeWithTag("gesture_sheet_handle").performTouchInput {
            swipeDown(startY = centerY, endY = centerY + 900f, durationMillis = 300)
        }
        rule.onNodeWithTag("gesture_sheet").assertDoesNotExist()
        rule.onNodeWithTag("memory_add").performClick()
        Espresso.pressBack()
        rule.onNodeWithTag("gesture_sheet").assertDoesNotExist()
        rule.onNodeWithTag("memory_add").assertIsDisplayed()
    }
}
