package com.mushinlab.launcher.feature.home

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.HostedWidget
import com.mushinlab.launcher.core.model.WidgetCell
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class WidgetPanelsTest {
    @Test
    fun AC_51_2_emptyFineRowsCollapseOutsideEditing() {
        val lower = HomeWidgetPanel(persistentListOf(HomeWidgetTile(5, "w", WidgetCell(4, 4, 4, 4))))
        assertThat(lower.visibleRows(editing = false)).containsExactly(4, 5, 6, 7).inOrder()
        assertThat(lower.visibleRows(editing = true)).containsExactly(0, 1, 2, 3, 4, 5, 6, 7).inOrder()
        assertThat(HomeWidgetPanel(persistentListOf()).visibleRows(editing = false)).isEmpty()
        assertThat(lower.freeCells).hasSize(48)
        assertThat(lower.freeCells.first()).isEqualTo(WidgetCell(0, 0))
    }

    @Test
    fun AC_55_3_onlyTheFirstFreeQuarterIsOfferedForAdding() {
        val clock = HomeWidgetTile(HostedWidget.CLOCK_ID, "", WidgetCell(0, 0, 4, 4))
        assertThat(HomeWidgetPanel(persistentListOf(clock)).addCells).containsExactly(WidgetCell(4, 0, 4, 4))
        assertThat(HomeWidgetPanel(persistentListOf()).addCells).containsExactly(WidgetCell(0, 0, 4, 4))
    }

    @Test
    fun AC_51_2_addCellsFallBackToTheFirstFreeFineCell() {
        val panel = HomeWidgetPanel(
            persistentListOf(
                HomeWidgetTile(HostedWidget.CLOCK_ID, "", WidgetCell(0, 0, 4, 4)),
                HomeWidgetTile(5, "w", WidgetCell(3, 3, 2, 2)),
            ),
        )
        assertThat(panel.addCells).containsExactly(WidgetCell(4, 0))
    }

    @Test
    fun AC_51_1_fourWidgetsLeaveNoAddCells() {
        val panel = HomeWidgetPanel(
            persistentListOf(
                HomeWidgetTile(HostedWidget.CLOCK_ID, "", WidgetCell(0, 0, 4, 4)),
                HomeWidgetTile(5, "a", WidgetCell(4, 0, 4, 4)),
                HomeWidgetTile(6, "b", WidgetCell(0, 4, 4, 4)),
                HomeWidgetTile(7, "c", WidgetCell(4, 4, 4, 4)),
            ),
        )
        assertThat(panel.addCells).isEmpty()
    }

    @Test
    fun AC_51_2_dragSnapsToTheFineGridAndKeepsTheSpanInside() {
        assertThat(WidgetCell(0, 0).moved(1, 1)).isEqualTo(WidgetCell(1, 1))
        assertThat(WidgetCell(0, 0, 4, 1).moved(1, 1)).isEqualTo(WidgetCell(1, 1, 4, 1))
        assertThat(WidgetCell(4, 4).moved(-5, -5)).isEqualTo(WidgetCell(0, 0))
    }

    @Test
    fun AC_55_4_aTallWidgetMovedTooLowBecomesShorter() {
        assertThat(WidgetCell(0, 0, 4, 8).moved(4, 3)).isEqualTo(WidgetCell(4, 3, 4, 5))
        assertThat(WidgetCell(0, 0, 4, 4).moved(0, 9)).isEqualTo(WidgetCell(0, 7, 4, 1))
        assertThat(WidgetCell(0, 4, 4, 4).moved(0, -4)).isEqualTo(WidgetCell(0, 0, 4, 4))
        assertThat(WidgetCell(6, 0, 2, 2).moved(3, 0)).isEqualTo(WidgetCell(6, 0, 2, 2))
    }

    @Test
    fun AC_51_2_edgesStretchAndShrinkWithinTheFineGrid() {
        assertThat(WidgetCell(0, 0).resized(WidgetEdge.END, 1)).isEqualTo(WidgetCell(0, 0, 2, 1))
        assertThat(WidgetCell(0, 0).resized(WidgetEdge.END, 9)).isEqualTo(WidgetCell(0, 0, 8, 1))
        assertThat(WidgetCell(4, 0).resized(WidgetEdge.START, -4)).isEqualTo(WidgetCell(0, 0, 5, 1))
        assertThat(WidgetCell(0, 0, 4, 1).resized(WidgetEdge.START, 1)).isEqualTo(WidgetCell(1, 0, 3, 1))
        assertThat(WidgetCell(0, 4).resized(WidgetEdge.TOP, -4)).isEqualTo(WidgetCell(0, 0, 1, 5))
        assertThat(WidgetCell(0, 0, 1, 4).resized(WidgetEdge.BOTTOM, -1)).isEqualTo(WidgetCell(0, 0, 1, 3))
    }

    @Test
    fun AC_51_2_onlyMovableEdgesHaveHandles() {
        assertThat(WidgetEdge.entries.filter { WidgetCell(0, 0).canResize(it) })
            .containsExactly(WidgetEdge.END, WidgetEdge.BOTTOM)
        assertThat(WidgetEdge.entries.filter { WidgetCell(0, 0, 8, 8).canResize(it) })
            .containsExactlyElementsIn(WidgetEdge.entries)
    }

    @Test
    fun accessibilityStepsFollowReadingOrder() {
        assertThat(WidgetCell(1, 0).stepped(1)).isEqualTo(WidgetCell(2, 0))
        assertThat(WidgetCell(0, 0).stepped(-1)).isNull()
        assertThat(WidgetCell(0, 0, 2, 1).stepped(1)).isEqualTo(WidgetCell(1, 0, 2, 1))
    }

    @Test
    fun accessibilityResizeGrowsAndShrinksOneStep() {
        assertThat(WidgetCell(0, 0).grown()).isEqualTo(WidgetCell(0, 0, 2, 1))
        assertThat(WidgetCell(7, 7).grown()).isEqualTo(WidgetCell(6, 7, 2, 1))
        assertThat(WidgetCell(0, 0, 2, 1).grown()).isEqualTo(WidgetCell(0, 0, 3, 1))
        assertThat(WidgetCell(0, 0, 8, 8).grown()).isNull()
        assertThat(WidgetCell(0, 0, 8, 8).shrunk()).isEqualTo(WidgetCell(0, 0, 7, 8))
        assertThat(WidgetCell(0, 0, 1, 8).shrunk()).isEqualTo(WidgetCell(0, 0, 1, 7))
        assertThat(WidgetCell(7, 7).shrunk()).isNull()
    }
}
