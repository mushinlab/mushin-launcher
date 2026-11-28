package com.mushinlab.launcher.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.feature.home.gesture.RowRegistry
import kotlin.math.roundToInt

@Composable
internal fun EditRow(
    modifier: Modifier,
    handle: Modifier,
    index: Int,
    row: HomeRow,
    style: EditRowStyle,
    registry: RowRegistry,
    swipe: RowSwipe,
    actions: State<HomeActions>,
) {
    val rowStyle = style.row
    Box(
        modifier
            .fillMaxWidth()
            .height(rowStyle.height)
            .onGloballyPositioned { registry.update(index, it.boundsInRoot()) }
            .semantics { contentDescription = row.label }
            .testTag("row_$index"),
    ) {
        RemoveStrip(index, row, swipe) { actions.value.onRemoveRow(row) }
        Row(
            Modifier
                .fillMaxSize()
                .offset { IntOffset(-swipe.offsetOf(row.key).roundToInt(), 0) }
                .background(rowStyle.color(index))
                .swipeToReveal(row.key, swipe)
                .padding(horizontal = EDIT_ROW_PADDING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                handle.fillMaxHeight().width(CONTROL_SIZE).testTag("row_drag_$index"),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painterResource(Glyph.DRAG.res),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(LocalMushinColors.current.textSecondary),
                    modifier = Modifier.size(HANDLE_SIZE),
                )
            }
            RowLabel(
                row,
                index,
                style.layout,
                rowStyle.text,
                Modifier.weight(1f).padding(horizontal = LABEL_GAP).clearAndSetSemantics { },
            )
            row.folderId?.let { folder ->
                GlyphButton(
                    Glyph.EDIT,
                    stringResource(R.string.home_row_edit, row.label),
                    LocalMushinColors.current.textPrimary,
                    tag = "row_edit_$index",
                ) { actions.value.onEditFolder(folder) }
            }
        }
    }
}

internal class EditRowStyle(val layout: RowLayout, val row: RowStyle)

@Composable
private fun BoxScope.RemoveStrip(index: Int, row: HomeRow, swipe: RowSwipe, onRemove: () -> Unit) {
    val offset = swipe.offsetOf(row.key)
    if (offset <= 0f) return
    val colors = LocalMushinColors.current
    val density = LocalDensity.current
    val reveal = with(density) { SwipeDimens.reveal.toPx() }
    Box(
        Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .width(with(density) { offset.toDp() })
            .clipToBounds()
            .background(colors.danger.copy(alpha = STRIP_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        GlyphButton(
            Glyph.DELETE,
            stringResource(R.string.home_row_remove, row.label),
            colors.danger,
            tag = "row_remove_$index",
            modifier = Modifier.requiredSize(CONTROL_SIZE).alpha((offset / reveal).coerceAtMost(1f)),
        ) { if (swipe.isOpen(row.key)) onRemove() }
    }
}

private val EDIT_ROW_PADDING = 8.dp
private val CONTROL_SIZE = 48.dp
private val HANDLE_SIZE = 20.dp
private val LABEL_GAP = 8.dp
private const val STRIP_ALPHA = 0.28f
