package com.mushinlab.launcher.feature.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EditListTest {
    @Test
    fun outsideEditModeEveryRowIsShown() {
        assertThat((0..20).all { EditList.shown(it, editing = false, dragging = false) }).isTrue()
    }

    @Test
    fun editModeShowsOnlyTheFirstFiveRows() {
        val shown = (0..9).filter { EditList.shown(it, editing = true, dragging = false) }

        assertThat(shown).containsExactly(0, 1, 2, 3, 4).inOrder()
    }

    @Test
    fun draggingTheListHandleShowsTheWholeList() {
        assertThat((0..20).all { EditList.shown(it, editing = true, dragging = true) }).isTrue()
    }
}
