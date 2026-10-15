package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable

@Composable
internal fun ReorderableItems(items: List<EditorItem>, onChange: (List<AppKey>) -> Unit) {
    val colors = LocalMushinColors.current
    val density = LocalDensity.current
    val rowHeightPx = with(density) { (ROW_HEIGHT + CARD_GAP).toPx() }
    var order by remember(items) { mutableStateOf(items) }
    var dragKey by remember { mutableStateOf<AppKey?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val currentOrder by rememberUpdatedState(order)
    val moveUp = stringResource(R.string.space_move_up)
    val moveDown = stringResource(R.string.space_move_down)
    fun move(from: Int, to: Int): List<EditorItem> {
        val list = currentOrder.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        order = list
        return list
    }
    fun dragBy(key: AppKey, dy: Float) {
        dragOffset += dy
        val from = currentOrder.indexOfFirst { it.key == key }
        if (dragOffset > rowHeightPx / 2 && from < currentOrder.lastIndex) {
            move(from, from + 1)
            dragOffset -= rowHeightPx
        } else if (dragOffset < -rowHeightPx / 2 && from > 0) {
            move(from, from - 1)
            dragOffset += rowHeightPx
        }
    }
    order.forEachIndexed { index, item ->
        key(item.key.id) {
            val dragging = dragKey == item.key
            Row(
                Modifier
                    .graphicsLayer { translationY = if (dragging) dragOffset else 0f }
                    .settingsCard()
                    .height(ROW_HEIGHT)
                    .background(if (dragging) colors.accent.copy(alpha = DRAG_ALPHA) else Color.Transparent)
                    .moveActions(index, order.lastIndex, moveUp, moveDown) { to ->
                        onChange(move(index, to).map { it.key })
                    }
                    .testTag("editor_item_$index"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RemoveButton(index) {
                    val next = currentOrder.filterNot { it.key == item.key }
                    order = next
                    onChange(next.map { it.key })
                }
                SingleLineText(
                    item.label + itemNote(item),
                    MushinType.style(17.sp, colors.textPrimary),
                    Modifier.weight(1f),
                )
                DragHandle(
                    index,
                    item.key,
                    onStart = {
                        dragKey = item.key
                        dragOffset = 0f
                    },
                    onDrag = { dy -> dragBy(item.key, dy) },
                    onEnd = {
                        dragKey = null
                        dragOffset = 0f
                        onChange(currentOrder.map { it.key })
                    },
                )
            }
        }
    }
}

private fun Modifier.moveActions(
    index: Int,
    lastIndex: Int,
    moveUp: String,
    moveDown: String,
    onMove: (Int) -> Unit,
): Modifier = semantics {
    customActions = listOfNotNull(
        if (index > 0) {
            CustomAccessibilityAction(moveUp) {
                onMove(index - 1)
                true
            }
        } else {
            null
        },
        if (index < lastIndex) {
            CustomAccessibilityAction(moveDown) {
                onMove(index + 1)
                true
            }
        } else {
            null
        },
    )
}

@Composable
private fun itemNote(item: EditorItem): String = when {
    item.missing -> " · ${stringResource(R.string.space_item_missing)}"
    item.hidden -> " · ${stringResource(R.string.space_item_hidden)}"
    else -> ""
}

@Composable
private fun RemoveButton(index: Int, onRemove: () -> Unit) {
    Box(
        Modifier.size(48.dp).pressable(onClick = onRemove).testTag("editor_remove_$index"),
        contentAlignment = Alignment.Center,
    ) { BasicText("−", style = MushinType.style(22.sp, LocalMushinColors.current.textPrimary)) }
}

@Composable
private fun DragHandle(
    index: Int,
    key: AppKey,
    onStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onEnd: () -> Unit,
) {
    Box(
        Modifier
            .size(48.dp)
            .testTag("editor_handle_$index")
            .pointerInput(key) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (!awaitHold(down.id)) return@awaitEachGesture
                    onStart()
                    dragUntilUp(down.id, onDrag)
                    onEnd()
                }
            },
        contentAlignment = Alignment.Center,
    ) { BasicText("≡", style = MushinType.style(22.sp, LocalMushinColors.current.textSecondary)) }
}

private suspend fun AwaitPointerEventScope.awaitHold(id: PointerId): Boolean = withTimeoutOrNull(HANDLE_HOLD_MS) {
    var released = false
    while (!released) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == id }
        released = change == null || !change.pressed
    }
} == null

private suspend fun AwaitPointerEventScope.dragUntilUp(id: PointerId, onDelta: (Float) -> Unit) {
    var change = awaitPointerEvent().changes.firstOrNull { it.id == id }
    while (change != null && change.pressed) {
        onDelta(change.positionChange().y)
        change.consume()
        change = awaitPointerEvent().changes.firstOrNull { it.id == id }
    }
}

private val ROW_HEIGHT = 56.dp
private const val DRAG_ALPHA = 0.12f
private const val HANDLE_HOLD_MS = 200L
