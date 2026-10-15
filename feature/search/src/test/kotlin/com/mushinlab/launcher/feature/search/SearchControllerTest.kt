package com.mushinlab.launcher.feature.search

import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.search.GestureDecoder
import com.mushinlab.launcher.core.search.GesturePoint
import com.mushinlab.launcher.core.search.KeyAction
import com.mushinlab.launcher.core.search.KeyboardLayouts
import com.mushinlab.launcher.core.search.SearchIndex
import com.mushinlab.launcher.core.search.SearchItem
import org.junit.Test

class SearchControllerTest {
    private fun item(
        label: String,
        hidden: Boolean = false,
    ) = SearchItem(AppKey("p.$label", "A", 0), label, hidden = hidden)

    private val index = SearchIndex.build(
        listOf(item("Slack"), item("Telegram"), item("Камера"), item("Secret", hidden = true)),
    )

    @Test
    fun AC_09_3_emptyQueryShowsVisibleAppsAlphabetically() {
        val controller = SearchController(clock = { 0L })
        controller.sources.update(index, emptyMap())
        assertThat(controller.rows.map { it.label }).containsExactly("Камера", "Slack", "Telegram").inOrder()
    }

    @Test
    fun typingAndBackspaceRecomputeSynchronously() {
        val controller = SearchController(clock = { 0L })
        controller.sources.update(index, emptyMap())
        controller.type('s')
        controller.type('l')
        assertThat(controller.query).isEqualTo("sl")
        assertThat(controller.rows.first().label).isEqualTo("Slack")
        controller.backspace()
        assertThat(controller.query).isEqualTo("s")
        controller.clear()
        assertThat(controller.query).isEmpty()
        assertThat(controller.rows).hasSize(3)
    }

    @Test
    fun AC_08_edge_queryIsLimitedTo32Chars() {
        val controller = SearchController(clock = { 0L })
        repeat(32) { assertThat(controller.type('a')).isTrue() }
        assertThat(controller.type('a')).isFalse()
        assertThat(controller.query).hasLength(32)
    }

    @Test
    fun AC_09_6_noResultsIsEmptyList() {
        val controller = SearchController(clock = { 0L })
        controller.sources.update(index, emptyMap())
        "xyzq".forEach { controller.type(it) }
        assertThat(controller.rows).isEmpty()
        assertThat(controller.target()).isNull()
    }

    @Test
    fun AC_14_2_arrowsMoveSelectionAndEnterUsesIt() {
        val controller = SearchController(clock = { 0L })
        controller.sources.update(index, emptyMap())
        assertThat(controller.target()?.label).isEqualTo("Камера")
        controller.moveSelection(1)
        controller.moveSelection(1)
        assertThat(controller.target()?.label).isEqualTo("Slack")
        controller.moveSelection(5)
        assertThat(controller.target()?.label).isEqualTo("Telegram")
        controller.reset()
        assertThat(controller.selected).isEqualTo(SearchController.NO_SELECTION)
    }

    @Test
    fun replaceQueryAndResetClosesMenu() {
        val controller = SearchController(clock = { 0L })
        controller.sources.update(index, emptyMap())
        controller.type('x')
        controller.replaceQuery("slack")
        assertThat(controller.query).isEqualTo("slack")
        assertThat(controller.target()?.label).isEqualTo("Slack")
        controller.panels.openMenu(
            SearchMenu(controller.rows.first().key, "Slack", Offset.Zero, inSpace = false, canUninstall = true),
        )
        assertThat(controller.panels.menu).isNotNull()
        controller.reset()
        assertThat(controller.panels.menu).isNull()
    }

    @Test
    fun hardwareKeysMapToActions() {
        assertThat(KeyboardRouter.map(KeyEvent.KEYCODE_DEL, 0, false)).isEqualTo(HardwareKey.Backspace)
        assertThat(KeyboardRouter.map(KeyEvent.KEYCODE_ENTER, 0, false)).isEqualTo(HardwareKey.Enter)
        assertThat(KeyboardRouter.map(KeyEvent.KEYCODE_ESCAPE, 0, false)).isEqualTo(HardwareKey.Escape)
        assertThat(KeyboardRouter.map(KeyEvent.KEYCODE_DPAD_UP, 0, false)).isEqualTo(HardwareKey.Up)
        assertThat(KeyboardRouter.map(KeyEvent.KEYCODE_DPAD_DOWN, 0, false)).isEqualTo(HardwareKey.Down)
        assertThat(KeyboardRouter.map(KeyEvent.KEYCODE_SPACE, ' '.code, true)).isEqualTo(HardwareKey.SwitchLayout)
        assertThat(KeyboardRouter.map(KeyEvent.KEYCODE_A, 'S'.code, false)).isEqualTo(HardwareKey.Text('s'))
        assertThat(KeyboardRouter.map(KeyEvent.KEYCODE_A, 'ж'.code, false)).isEqualTo(HardwareKey.Text('ж'))
        assertThat(KeyboardRouter.map(KeyEvent.KEYCODE_A, 'a'.code, true)).isNull()
        assertThat(KeyboardRouter.map(KeyEvent.KEYCODE_SPACE, ' '.code, false)).isNull()
    }

    @Test
    fun keyboardGeometryFillsRowsAndHitsNearestKey() {
        val keys = KeyboardGeometry.compute(
            KeyboardLayouts.EN,
            width = 1080f,
            rowHeight = 136f,
            topPadding = 10f,
            gap = 10f,
        )
        assertThat(keys.size).isEqualTo(10 + 9 + 9 + 2)
        val space = keys.single { it.spec.action == KeyAction.SPACE }
        val resize = keys.single { it.spec.action == KeyAction.RESIZE }
        assertThat(resize.rect.right).isLessThan(space.rect.left)
        assertThat(space.rect.right).isGreaterThan(1060f)
        assertThat(space.rect.height + 10f).isEqualTo((keys[0].rect.height + 10f) / 2)
        assertThat(space.rect.bottom + 5f + 10f)
            .isEqualTo(KeyboardGeometry.height(KeyboardLayouts.EN, rowHeight = 136f, padding = 10f))
        val q = keys.indexOfFirst { it.spec.char == 'q' }
        assertThat(KeyboardGeometry.hit(keys, Offset(20f, 60f))).isEqualTo(q)
        val gapBetweenQandW = Offset(keys[q].rect.right + 2f, keys[q].rect.center.y)
        assertThat(KeyboardGeometry.hit(keys, gapBetweenQandW)).isNotNull()
        val backspace = keys.first { it.spec.action == KeyAction.BACKSPACE }
        assertThat(backspace.rect.right).isGreaterThan(1060f)
        val a = keys.first { it.spec.char == 'a' }
        assertThat(a.rect.left).isGreaterThan(54f)
    }

    @Test
    fun spaceIsTypedOnlyBetweenWords() {
        val controller = SearchController(clock = { 0L })
        controller.sources.update(index, emptyMap())
        assertThat(controller.type(' ')).isFalse()
        controller.type('s')
        assertThat(controller.type(' ')).isTrue()
        assertThat(controller.type(' ')).isFalse()
        assertThat(controller.query).isEqualTo("s ")
        assertThat(controller.rows.first().label).isEqualTo("Slack")
    }

    @Test
    fun gestureReplacesQueryWithAppWordAndLearnsOnlyAfterCorrection() {
        val keys = KeyboardGeometry.compute(KeyboardLayouts.EN, 1080f, 136f, 10f, 10f)
            .mapNotNull { k -> k.spec.char?.let { it to GesturePoint(k.rect.center.x, k.rect.center.y) } }
            .toMap()
        val decoder = GestureDecoder(keys)
        val path = { word: String -> word.map { keys.getValue(it) } }
        val controller = SearchController(clock = { 0L })
        controller.sources.update(index, emptyMap())
        val session = controller.session

        val slack = AppKey("p.Slack", "A", 0)
        assertThat(controller.gestures.decode(decoder, path("kamera"))).isTrue()
        assertThat(controller.query).isEqualTo("камера")
        assertThat(controller.fromGesture).isTrue()
        assertThat(controller.gestures.toLearn(slack)).isEmpty()

        controller.gestures.decode(decoder, path("slack"))
        assertThat(controller.query).isEqualTo("slack")
        assertThat(controller.gestures.toLearn(slack).map { it.path }).containsExactly(decoder.keysAlong(path("slack")))

        controller.reset()
        assertThat(controller.session).isGreaterThan(session)
        assertThat(controller.gestures.toLearn(slack)).isEmpty()
        controller.gestures.decode(decoder, path("slack"))
        controller.backspace()
        assertThat(controller.query).isEmpty()
        assertThat(controller.fromGesture).isFalse()
        controller.type('k')
        assertThat(controller.gestures.toLearn(slack).map { it.path }).containsExactly(decoder.keysAlong(path("slack")))
        controller.type('a')
        controller.backspace()
        assertThat(controller.query).isEqualTo("k")
    }

    @Test
    fun gestureLikeSeveralAppsShowsThemAllAsSuggestions() {
        val keys = KeyboardGeometry.compute(KeyboardLayouts.EN, 1080f, 136f, 10f, 10f)
            .mapNotNull { k -> k.spec.char?.let { it to GesturePoint(k.rect.center.x, k.rect.center.y) } }
            .toMap()
        val decoder = GestureDecoder(keys)
        val controller = SearchController(clock = { 0L })
        controller.sources.update(
            SearchIndex.build(listOf(item("Google Maps"), item("Files by Google"), item("Slack"))),
            emptyMap(),
        )

        assertThat(controller.gestures.decode(decoder, "google".map { keys.getValue(it) })).isTrue()
        assertThat(controller.suggesting).isTrue()
        assertThat(controller.query).isEmpty()
        assertThat(controller.rows.map { it.label }).containsExactly("Google Maps", "Files by Google")
        assertThat(controller.target()?.label).isEqualTo(controller.rows.first().label)
        val maps = AppKey("p.Google Maps", "A", 0)
        assertThat(controller.gestures.toLearn(maps).map { it.path })
            .containsExactly(decoder.keysAlong("google".map { keys.getValue(it) }))

        assertThat(controller.backspace()).isTrue()
        assertThat(controller.suggesting).isFalse()
        assertThat(controller.rows).hasSize(3)

        controller.gestures.decode(decoder, "google".map { keys.getValue(it) })
        controller.type('s')
        assertThat(controller.suggesting).isFalse()
        assertThat(controller.query).isEqualTo("s")
    }

    @Test
    fun eyeToggleShowsHiddenAppsUntilSearchCloses() {
        val controller = SearchController(clock = { 0L })
        controller.sources.update(index, emptyMap())
        assertThat(controller.rows.map { it.label }).doesNotContain("Secret")
        controller.toggleShowHidden()
        assertThat(controller.rows.single { it.label == "Secret" }.hidden).isTrue()
        controller.type('c')
        controller.type('r')
        assertThat(controller.rows.map { it.label }).contains("Secret")
        controller.reset()
        assertThat(controller.showHidden).isFalse()
        assertThat(controller.rows.map { it.label }).doesNotContain("Secret")
    }

    @Test
    fun rowsCarryTheSwipesTheirAppIsBoundTo() {
        val controller = SearchController(clock = { 0L })
        controller.sources.update(index, emptyMap())
        val slack = controller.rows.first { it.label == "Slack" }.key
        controller.sources.setSwipes(mapOf(slack to listOf(SwipeDirection.LEFT, SwipeDirection.UP)))
        assertThat(controller.rows.first { it.label == "Slack" }.swipes)
            .containsExactly(SwipeDirection.LEFT, SwipeDirection.UP).inOrder()
        assertThat(controller.rows.first { it.label == "Telegram" }.swipes).isEmpty()
        controller.type('s')
        assertThat(controller.rows.first { it.label == "Slack" }.swipes).hasSize(2)
    }

    @Test
    fun ruTopRowKeysAreNarrowerThanEn() {
        val en = KeyboardGeometry.compute(KeyboardLayouts.EN, 1080f, 136f, 10f, 10f).first()
        val ru = KeyboardGeometry.compute(KeyboardLayouts.RU, 1080f, 136f, 10f, 10f).first()
        assertThat(ru.rect.width).isLessThan(en.rect.width)
    }

    @Test
    fun unrelatedDrawingIsNotLearnedForTheAppOpenedAfterIt() {
        val keys = KeyboardGeometry.compute(KeyboardLayouts.EN, 1080f, 136f, 10f, 10f)
            .mapNotNull { k -> k.spec.char?.let { it to GesturePoint(k.rect.center.x, k.rect.center.y) } }
            .toMap()
        val decoder = GestureDecoder(keys)
        val controller = SearchController(clock = { 0L })
        controller.sources.update(index, emptyMap())
        controller.gestures.decode(decoder, "ijn".map { keys.getValue(it) })
        controller.type('s')
        assertThat(controller.gestures.toLearn(AppKey("p.Telegram", "A", 0))).isEmpty()
    }
}
