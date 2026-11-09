package com.mushinlab.launcher.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WidgetLayoutTest {
    private val a = HostedWidget(1, "a", WidgetCell(0, 0))
    private val b = HostedWidget(2, "b", WidgetCell(1, 0))
    private val c = HostedWidget(3, "c", WidgetCell(0, 1))

    @Test
    fun cellsStayInsideTheTwoByTwoGrid() {
        assertThat(WidgetCell(1, 1).fits).isTrue()
        assertThat(WidgetCell(0, 0, 2, 2).fits).isTrue()
        assertThat(WidgetCell(1, 0, 2, 1).fits).isFalse()
        assertThat(WidgetCell(0, 1, 1, 2).fits).isFalse()
        assertThat(WidgetCell(-1, 0).fits).isFalse()
    }

    @Test
    fun overlapComparesRectangles() {
        assertThat(WidgetCell(0, 0, 2, 1).overlaps(WidgetCell(1, 0))).isTrue()
        assertThat(WidgetCell(0, 0, 2, 1).overlaps(WidgetCell(1, 1))).isFalse()
        assertThat(WidgetCell(0, 0, 1, 2).overlaps(WidgetCell(0, 1))).isTrue()
    }

    @Test
    fun addUsesThePreferredFreeCellOrTheFirstFreeOne() {
        val layout = WidgetLayout(listOf(a))
        assertThat(layout.add(b, WidgetCell(1, 1))?.widgets?.last()?.cell).isEqualTo(WidgetCell(1, 1))
        assertThat(layout.add(b, WidgetCell(0, 0))?.widgets?.last()?.cell).isEqualTo(WidgetCell(1, 0))
        assertThat(WidgetLayout(listOf(a.copy(cell = WidgetCell(0, 0, 2, 2)))).add(b)).isNull()
        assertThat(layout.add(a)).isNull()
    }

    @Test
    fun movingOntoAnotherWidgetSwapsThem() {
        val moved = WidgetLayout(listOf(a, b)).place(1, WidgetCell(1, 0))
        assertThat(moved?.widgets).containsExactly(a.copy(cell = WidgetCell(1, 0)), b.copy(cell = WidgetCell(0, 0)))
    }

    @Test
    fun movingOntoAnotherWidgetSwapsThemEvenWhenAnEarlierCellIsFree() {
        val layout = WidgetLayout(listOf(a.copy(cell = WidgetCell(1, 1)), b))
        val moved = layout.place(1, WidgetCell(1, 0))
        assertThat(moved?.widgets?.map { it.cell }).containsExactly(WidgetCell(1, 0), WidgetCell(1, 1))
    }

    @Test
    fun widenedWidgetPushesItsNeighbourToFreeSpace() {
        val wide = WidgetLayout(listOf(a, b)).place(1, WidgetCell(0, 0, 2, 1))
        assertThat(wide?.widgets?.map { it.cell }).containsExactly(WidgetCell(0, 0, 2, 1), WidgetCell(0, 1))
    }

    @Test
    fun displacedWidgetKeepsItsSpanWhenThereIsRoom() {
        val tall = WidgetLayout(listOf(a.copy(cell = WidgetCell(0, 0, 1, 2)), b))
        val swapped = tall.place(2, WidgetCell(0, 0))
        assertThat(swapped?.widgets?.map { it.cell }).containsExactly(WidgetCell(1, 0, 1, 2), WidgetCell(0, 0))
    }

    @Test
    fun displacedWidgetShrinksWhenItsSpanDoesNotFit() {
        val d = HostedWidget(4, "d", WidgetCell(1, 1))
        val layout = WidgetLayout(listOf(a.copy(cell = WidgetCell(0, 0, 1, 2)), b, d))
        val moved = layout.place(2, WidgetCell(0, 0))
        assertThat(
            moved?.widgets?.map {
                it.cell
            },
        ).containsExactly(WidgetCell(1, 0), WidgetCell(0, 0), WidgetCell(1, 1))
        assertThat(moved?.freeCells).containsExactly(WidgetCell(0, 1))
    }

    @Test
    fun movingASingleOntoAWideRowSwapsTheRows() {
        val layout = WidgetLayout(listOf(a.copy(cell = WidgetCell(0, 0, 2, 1)), c))
        val moved = layout.place(3, WidgetCell(0, 0))
        assertThat(moved?.widgets?.map { it.cell }).containsExactly(WidgetCell(0, 1, 2, 1), WidgetCell(0, 0))
    }

    @Test
    fun placeFailsOutsideTheGridOrWithoutRoom() {
        val full = WidgetLayout(listOf(a, b, c, HostedWidget(4, "d", WidgetCell(1, 1))))
        assertThat(full.place(1, WidgetCell(0, 0, 2, 2))).isNull()
        assertThat(full.place(1, WidgetCell(1, 0, 2, 1))).isNull()
        assertThat(full.place(9, WidgetCell(0, 0))).isNull()
        assertThat(full.freeCells).isEmpty()
        assertThat(WidgetLayout(listOf(a, b)).place(1, WidgetCell(0, 0, 2, 2))).isNull()
    }

    @Test
    fun threeWidgetsCanShareAColumnAndTwoRows() {
        val layout = WidgetLayout(listOf(a, c)).place(1, WidgetCell(0, 0, 1, 2))
        assertThat(layout?.widgets?.map { it.cell }).containsExactly(WidgetCell(0, 0, 1, 2), WidgetCell(1, 0))
        assertThat(layout?.freeCells).containsExactly(WidgetCell(1, 1))
    }
}
