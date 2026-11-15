package com.mushinlab.launcher.feature.home

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppKey
import org.junit.Test

class RowReorderTest {
    private val a = AppKey("a", "A", 0)
    private val b = AppKey("b", "B", 0)
    private val c = AppKey("c", "C", 0)
    private val keys = listOf(a, b, c)

    @Test
    fun AC_37_10_draggingPastHalfARowSwapsAndTheDropTakesTheTargetPlace() {
        val reorder = RowReorder()
        reorder.begin(a, keys)
        reorder.drag(40f, rowHeight = 100f)
        assertThat(reorder.order).isEqualTo(keys)
        reorder.drag(120f, rowHeight = 100f)
        assertThat(reorder.order).containsExactly(b, c, a).inOrder()
        assertThat(reorder.offset).isWithin(0.01f).of(-40f)
        assertThat(reorder.drop()).isEqualTo(a to c)
        assertThat(reorder.arrange(keys) { it }).containsExactly(b, c, a).inOrder()
    }

    @Test
    fun theDraggedRowStopsAtTheEndsAndADropInPlaceMovesNothing() {
        val reorder = RowReorder()
        reorder.begin(b, keys)
        reorder.drag(-500f, rowHeight = 100f)
        assertThat(reorder.order).containsExactly(b, a, c).inOrder()
        assertThat(reorder.offset).isEqualTo(0f)
        reorder.drag(60f, rowHeight = 100f)
        assertThat(reorder.order).isEqualTo(keys)
        assertThat(reorder.drop()).isNull()
    }

    @Test
    fun theLocalOrderLastsUntilStoredRowsCatchUpOrChange() {
        val reorder = RowReorder()
        reorder.sync(keys)
        reorder.begin(a, keys)
        reorder.drag(100f, rowHeight = 100f)
        reorder.sync(keys)
        assertThat(reorder.order).isNotNull()
        reorder.drop()
        reorder.settled()
        assertThat(reorder.order).containsExactly(b, a, c).inOrder()
        reorder.sync(listOf(b, a, c))
        assertThat(reorder.order).isNull()
        reorder.begin(a, keys)
        reorder.cancel()
        assertThat(reorder.order).isNull()
        assertThat(reorder.dragged).isNull()
    }

    @Test
    fun storedRowsThatCatchUpWhileTheRowSettlesEndTheLocalOrder() {
        val reorder = RowReorder()
        reorder.sync(keys)
        reorder.begin(a, keys)
        reorder.drag(100f, rowHeight = 100f)
        reorder.drop()
        reorder.sync(listOf(b, a, c))
        assertThat(reorder.order).containsExactly(b, a, c).inOrder()
        reorder.settled()
        assertThat(reorder.order).isNull()
    }

    @Test
    fun aHoldWithoutAMoveLeavesNoLocalOrder() {
        val reorder = RowReorder()
        reorder.sync(keys)
        reorder.begin(a, keys)
        assertThat(reorder.drop()).isNull()
        reorder.settled()
        assertThat(reorder.order).isNull()
    }

    @Test
    fun aRowLeavingTheListDuringADragEndsTheDrag() {
        val reorder = RowReorder()
        reorder.sync(keys)
        reorder.begin(a, keys)
        reorder.drag(100f, rowHeight = 100f)
        reorder.sync(listOf(a, b))
        assertThat(reorder.order).isNull()
        assertThat(reorder.dragged).isNull()
        assertThat(reorder.drop()).isNull()
    }
}
