package com.mushinlab.launcher.feature.home

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.WidgetCell
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class WidgetPanelsTest {
    @Test
    fun AC_39_2_emptyRowsCollapseOutsideEditing() {
        val lower = HomeWidgetPanel(persistentListOf(HomeWidgetTile(5, "w", WidgetCell(1, 1))))
        assertThat(lower.visibleRows(editing = false)).containsExactly(1)
        assertThat(lower.visibleRows(editing = true)).containsExactly(0, 1).inOrder()
        assertThat(HomeWidgetPanel(persistentListOf()).visibleRows(editing = false)).isEmpty()
        assertThat(lower.freeCells).containsExactly(WidgetCell(0, 0), WidgetCell(1, 0), WidgetCell(0, 1)).inOrder()
    }

    @Test
    fun AC_39_2_dragSnapsToTheGridAndKeepsTheSpanInside() {
        assertThat(WidgetCell(0, 0).moved(1, 1)).isEqualTo(WidgetCell(1, 1))
        assertThat(WidgetCell(0, 0, 2, 1).moved(1, 1)).isEqualTo(WidgetCell(0, 1, 2, 1))
        assertThat(WidgetCell(1, 1).moved(-5, -5)).isEqualTo(WidgetCell(0, 0))
    }

    @Test
    fun AC_39_2_edgesStretchAndShrinkWithinTheGrid() {
        assertThat(WidgetCell(0, 0).resized(WidgetEdge.END, 1)).isEqualTo(WidgetCell(0, 0, 2, 1))
        assertThat(WidgetCell(0, 0).resized(WidgetEdge.END, 5)).isEqualTo(WidgetCell(0, 0, 2, 1))
        assertThat(WidgetCell(1, 0).resized(WidgetEdge.START, -1)).isEqualTo(WidgetCell(0, 0, 2, 1))
        assertThat(WidgetCell(0, 0, 2, 1).resized(WidgetEdge.START, 1)).isEqualTo(WidgetCell(1, 0))
        assertThat(WidgetCell(0, 1).resized(WidgetEdge.TOP, -1)).isEqualTo(WidgetCell(0, 0, 1, 2))
        assertThat(WidgetCell(0, 0, 1, 2).resized(WidgetEdge.BOTTOM, -1)).isEqualTo(WidgetCell(0, 0))
    }

    @Test
    fun AC_39_2_onlyMovableEdgesHaveHandles() {
        assertThat(WidgetEdge.entries.filter { WidgetCell(0, 0).canResize(it) })
            .containsExactly(WidgetEdge.END, WidgetEdge.BOTTOM)
        assertThat(WidgetEdge.entries.filter { WidgetCell(0, 0, 2, 2).canResize(it) })
            .containsExactlyElementsIn(WidgetEdge.entries)
    }

    @Test
    fun accessibilityStepsFollowReadingOrder() {
        assertThat(WidgetCell(1, 0).stepped(1)).isEqualTo(WidgetCell(0, 1))
        assertThat(WidgetCell(0, 0).stepped(-1)).isNull()
        assertThat(WidgetCell(0, 0, 2, 1).stepped(1)).isEqualTo(WidgetCell(0, 1, 2, 1))
    }

    @Test
    fun accessibilityResizeGrowsAndShrinksOneStep() {
        assertThat(WidgetCell(0, 0).grown()).isEqualTo(WidgetCell(0, 0, 2, 1))
        assertThat(WidgetCell(1, 1).grown()).isEqualTo(WidgetCell(0, 1, 2, 1))
        assertThat(WidgetCell(0, 0, 2, 1).grown()).isEqualTo(WidgetCell(0, 0, 2, 2))
        assertThat(WidgetCell(0, 0, 2, 2).grown()).isNull()
        assertThat(WidgetCell(0, 0, 2, 2).shrunk()).isEqualTo(WidgetCell(0, 0, 1, 2))
        assertThat(WidgetCell(0, 0, 1, 2).shrunk()).isEqualTo(WidgetCell(0, 0))
        assertThat(WidgetCell(1, 1).shrunk()).isNull()
    }
}
