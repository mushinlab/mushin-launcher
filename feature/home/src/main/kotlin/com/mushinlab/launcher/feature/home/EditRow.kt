package com.mushinlab.launcher.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.feature.home.gesture.RowRegistry

@Composable
internal fun EditRow(
    modifier: Modifier,
    handle: Modifier,
    index: Int,
    row: HomeRow,
    layout: RowLayout,
    rowStyle: RowStyle,
    registry: RowRegistry,
    actions: State<HomeActions>,
) {
    val tint = LocalMushinColors.current.textPrimary
    Row(
        modifier
            .fillMaxWidth()
            .height(rowStyle.height)
            .background(rowStyle.color(index))
            .onGloballyPositioned { registry.update(index, it.boundsInRoot()) }
            .semantics { contentDescription = row.label }
            .testTag("row_$index")
            .padding(horizontal = EDIT_ROW_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            handle.fillMaxHeight().width(CONTROL_SIZE).testTag("row_drag_$index"),
            contentAlignment = Alignment.Center,
        ) {
            GlyphBadge(Glyph.DRAG, HANDLE_SIZE)
        }
        RowLabel(
            row,
            index,
            layout,
            rowStyle.text,
            Modifier.weight(1f).padding(horizontal = LABEL_GAP).clearAndSetSemantics { },
        )
        GlyphButton(
            Glyph.EDIT,
            stringResource(R.string.home_row_edit, row.label),
            tint,
            tag = "row_edit_$index",
        ) { actions.value.onReplaceRow(row) }
        GlyphButton(
            Glyph.CLOSE,
            stringResource(R.string.home_row_remove, row.label),
            tint,
            tag = "row_remove_$index",
        ) { actions.value.onRemoveRow(row) }
    }
}

private val EDIT_ROW_PADDING = 8.dp
private val CONTROL_SIZE = 48.dp
private val HANDLE_SIZE = 40.dp
private val LABEL_GAP = 8.dp
