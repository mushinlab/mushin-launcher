package com.bluecubelabs.env.feature.search

import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.search.KeyAction
import com.bluecubelabs.env.core.search.KeyboardLayouts
import com.bluecubelabs.env.core.search.SearchIndex
import com.bluecubelabs.env.core.search.SearchItem
import com.google.common.truth.Truth.assertThat
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
        controller.update(index, emptyMap())
        assertThat(controller.rows.map { it.label }).containsExactly("Камера", "Slack", "Telegram").inOrder()
    }

    @Test
    fun typingAndBackspaceRecomputeSynchronously() {
        val controller = SearchController(clock = { 0L })
        controller.update(index, emptyMap())
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
        controller.update(index, emptyMap())
        "xyzq".forEach { controller.type(it) }
        assertThat(controller.rows).isEmpty()
        assertThat(controller.target()).isNull()
    }

    @Test
    fun AC_14_2_arrowsMoveSelectionAndEnterUsesIt() {
        val controller = SearchController(clock = { 0L })
        controller.update(index, emptyMap())
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
        controller.update(index, emptyMap())
        controller.type('x')
        controller.replaceQuery("slack")
        assertThat(controller.query).isEqualTo("slack")
        assertThat(controller.target()?.label).isEqualTo("Slack")
        controller.openMenu(
            SearchMenu(controller.rows.first().key, "Slack", Offset.Zero, inEnv = false, canUninstall = true),
        )
        assertThat(controller.menu).isNotNull()
        controller.reset()
        assertThat(controller.menu).isNull()
        assertThat(controller.vocabulary).contains("slack")
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
        assertThat(keys.size).isEqualTo(10 + 9 + 9)
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
    fun ruTopRowKeysAreNarrowerThanEn() {
        val en = KeyboardGeometry.compute(KeyboardLayouts.EN, 1080f, 136f, 10f, 10f).first()
        val ru = KeyboardGeometry.compute(KeyboardLayouts.RU, 1080f, 136f, 10f, 10f).first()
        assertThat(ru.rect.width).isLessThan(en.rect.width)
    }
}
