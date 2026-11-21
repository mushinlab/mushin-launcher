package com.mushinlab.launcher.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WidgetLayoutTest {
    private val a = HostedWidget(1, "a", WidgetCell(0, 0, 4, 4))
    private val b = HostedWidget(2, "b", WidgetCell(4, 0, 4, 4))
    private val c = HostedWidget(3, "c", WidgetCell(0, 4, 4, 4))
    private val d = HostedWidget(4, "d", WidgetCell(4, 4, 4, 4))

    @Test
    fun cellsStayInsideTheEightByEightGrid() {
        assertThat(WidgetCell(7, 7).fits).isTrue()
        assertThat(WidgetCell(0, 0, 8, 8).fits).isTrue()
        assertThat(WidgetCell(4, 0, 8, 1).fits).isFalse()
        assertThat(WidgetCell(0, 4, 1, 8).fits).isFalse()
        assertThat(WidgetCell(-1, 0).fits).isFalse()
    }

    @Test
    fun overlapComparesRectangles() {
        assertThat(WidgetCell(0, 0, 4, 1).overlaps(WidgetCell(3, 0))).isTrue()
        assertThat(WidgetCell(0, 0, 4, 1).overlaps(WidgetCell(3, 1))).isFalse()
        assertThat(WidgetCell(0, 0, 1, 4).overlaps(WidgetCell(0, 3))).isTrue()
    }

    @Test
    fun addUsesThePreferredFreeCellOrTheFirstFreeOne() {
        val layout = WidgetLayout(listOf(a))
        assertThat(layout.add(HostedWidget(5, "e"), WidgetCell(4, 4))?.widgets?.last()?.cell)
            .isEqualTo(WidgetCell(4, 4))
        assertThat(layout.add(HostedWidget(5, "e"), WidgetCell(0, 0))?.widgets?.last()?.cell)
            .isEqualTo(WidgetCell(4, 0))
        assertThat(WidgetLayout(listOf(a.copy(cell = WidgetCell(0, 0, 8, 8)))).add(HostedWidget(5, "e"))).isNull()
        assertThat(layout.add(a)).isNull()
    }

    @Test
    fun addStopsAtFourWidgets() {
        val layout = WidgetLayout(listOf(a, b, c))
        assertThat(layout.add(d, WidgetCell(4, 4))).isNotNull()
        assertThat(WidgetLayout(listOf(a, b, c, d)).add(HostedWidget(5, "e"))).isNull()
    }

    @Test
    fun addingAWideWidgetTakesAFreeRowBeforeDisplacingAnything() {
        val layout = WidgetLayout(listOf(a)).add(HostedWidget(5, "e"), WidgetCell(4, 0), columnSpan = 8)
        assertThat(layout?.widgets?.map { it.cell }).containsExactly(WidgetCell(0, 0, 4, 4), WidgetCell(0, 4, 8, 1))
            .inOrder()
    }

    @Test
    fun addingATallWidgetTopAlignsItInTheColumnOfTheChosenCell() {
        val layout = WidgetLayout(listOf(a)).add(HostedWidget(5, "e"), WidgetCell(4, 4), rowSpan = 8)
        assertThat(layout?.widgets?.last()?.cell).isEqualTo(WidgetCell(4, 0, 1, 8))
    }

    @Test
    fun addingASpanThatDoesNotFitFallsBackToASmallerOne() {
        val layout = WidgetLayout(
            listOf(a, b, c),
        ).add(HostedWidget(5, "e"), WidgetCell(4, 4), columnSpan = 8, rowSpan = 8)
        assertThat(layout?.widgets?.last()?.cell).isEqualTo(WidgetCell(0, 4, 8, 1))
    }

    @Test
    fun movingOntoAnotherWidgetSwapsThem() {
        val moved = WidgetLayout(listOf(a.copy(cell = WidgetCell(0, 0)), b.copy(cell = WidgetCell(4, 0))))
            .place(1, WidgetCell(4, 0))
        assertThat(moved?.widgets?.map { it.cell }).containsExactly(WidgetCell(4, 0), WidgetCell(0, 0))
    }

    @Test
    fun widenedWidgetPushesItsNeighbourToFreeSpace() {
        val wide = WidgetLayout(listOf(a.copy(cell = WidgetCell(0, 0)), b.copy(cell = WidgetCell(4, 0))))
            .place(1, WidgetCell(0, 0, 8, 1))
        assertThat(wide?.widgets?.map { it.cell })
            .containsExactly(WidgetCell(0, 0, 8, 1), WidgetCell(0, 1))
            .inOrder()
    }

    @Test
    fun displacedWidgetKeepsItsSpanWhenThereIsRoom() {
        val tall = WidgetLayout(listOf(HostedWidget(1, "a", WidgetCell(0, 0, 1, 4)), b.copy(cell = WidgetCell(4, 0))))
        val swapped = tall.place(2, WidgetCell(0, 0))
        assertThat(
            swapped?.widgets?.map {
                it.cell
            },
        ).containsExactly(WidgetCell(4, 0, 1, 4), WidgetCell(0, 0)).inOrder()
    }

    @Test
    fun placeFailsOutsideTheGridOrWithoutRoom() {
        val full = WidgetLayout(listOf(a, b, c, d))
        assertThat(full.place(1, WidgetCell(0, 0, 8, 8))).isNull()
        assertThat(full.place(9, WidgetCell(0, 0))).isNull()
        assertThat(full.freeCells).isEmpty()
    }

    @Test
    fun aWidgetCanCoverAWholeColumnAndPushTheRestAside() {
        val layout = WidgetLayout(listOf(a, c)).place(3, WidgetCell(0, 0, 4, 8))
        assertThat(layout?.widgets?.map { it.cell })
            .containsExactly(WidgetCell(4, 0, 4, 4), WidgetCell(0, 0, 4, 8))
            .inOrder()
        assertThat(layout?.freeCells).hasSize(16)
    }

    @Test
    fun theDefaultClockFillsTheTopLeftQuarter() {
        assertThat(WidgetLayout().widgets).containsExactly(HostedWidget.clock)
        assertThat(HostedWidget.clock.cell).isEqualTo(WidgetCell(0, 0, 4, 4))
        assertThat(WidgetCell.quarters)
            .containsExactly(
                WidgetCell(0, 0, 4, 4),
                WidgetCell(4, 0, 4, 4),
                WidgetCell(0, 4, 4, 4),
                WidgetCell(4, 4, 4, 4),
            )
            .inOrder()
    }
}
