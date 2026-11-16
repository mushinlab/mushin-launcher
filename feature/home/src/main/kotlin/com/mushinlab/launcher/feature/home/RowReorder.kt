package com.mushinlab.launcher.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.Motion
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Stable
internal class RowReorder {
    var order by mutableStateOf<List<AppKey>?>(null)
        private set
    var dragged by mutableStateOf<AppKey?>(null)
        private set
    var offset by mutableFloatStateOf(0f)
    var settling: Job? = null
    private var start: List<AppKey> = emptyList()
    private var stored: List<AppKey> = emptyList()

    fun <T> arrange(rows: List<T>, key: (T) -> AppKey): List<T> {
        val local = order ?: return rows
        val byKey = rows.associateBy(key)
        if (byKey.keys != local.toSet()) return rows
        return local.map(byKey::getValue)
    }

    fun begin(key: AppKey, keys: List<AppKey>) {
        settling?.cancel()
        start = keys
        order = keys
        dragged = key
        offset = 0f
    }

    fun drag(delta: Float, rowHeight: Float) {
        val key = dragged ?: return
        val list = order?.toMutableList() ?: return
        var index = list.indexOf(key)
        offset += delta
        while (offset > rowHeight / 2 && index < list.lastIndex) {
            list[index] = list[index + 1]
            list[++index] = key
            offset -= rowHeight
        }
        while (offset < -rowHeight / 2 && index > 0) {
            list[index] = list[index - 1]
            list[--index] = key
            offset += rowHeight
        }
        if (index == 0) offset = offset.coerceAtLeast(0f)
        if (index == list.lastIndex) offset = offset.coerceAtMost(0f)
        order = list
    }

    fun drop(): Pair<AppKey, AppKey>? {
        val key = dragged ?: return null
        val target = order?.indexOf(key)?.let(start::getOrNull) ?: return null
        return (key to target).takeIf { target != key }
    }

    fun settled() {
        dragged = null
        offset = 0f
        reconcile()
    }

    fun sync(keys: List<AppKey>) {
        stored = keys
        reconcile()
    }

    fun cancel() {
        settling?.cancel()
        dragged = null
        offset = 0f
        order = null
    }

    private fun reconcile() {
        val local = order ?: return
        if (stored.toSet() != local.toSet()) {
            cancel()
        } else if (dragged == null && stored == local) {
            order = null
        }
    }
}

@Immutable
internal class RowMove(val key: AppKey, val keys: ImmutableList<AppKey>, val rowHeight: Float)

@Composable
internal fun Modifier.reorderableRow(
    move: RowMove,
    reorder: RowReorder,
    remove: CustomAccessibilityAction,
    onMove: (AppKey, AppKey) -> Unit,
): Modifier {
    val accent = LocalMushinColors.current.accent
    val steps = rememberSteps(move, onMove) + remove
    val dragged = reorder.dragged == move.key
    val lift by animateFloatAsState(if (dragged) LIFT_SCALE else 1f, tween(Motion.TRANSITION_MS), label = "lift")
    return slideOnMove(!dragged)
        .semantics { if (steps.isNotEmpty()) customActions = steps }
        .zIndex(if (dragged) 1f else 0f)
        .graphicsLayer {
            translationY = if (dragged) reorder.offset else 0f
            scaleX = lift
            scaleY = lift
        }
        .drawWithContent {
            drawContent()
            if (dragged) drawRect(accent.copy(alpha = HIGHLIGHT_ALPHA))
        }
}

@Composable
internal fun Modifier.reorderHandle(move: RowMove, reorder: RowReorder, onMove: (AppKey, AppKey) -> Unit): Modifier {
    val key = move.key
    val rowHeight = move.rowHeight
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentKeys by rememberUpdatedState(move.keys)
    val currentMove by rememberUpdatedState(onMove)
    return pointerInput(key, rowHeight) {
        detectDragGestures(
            onDragStart = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                reorder.begin(key, currentKeys)
            },
            onDragEnd = {
                reorder.drop()?.let { (moved, target) -> currentMove(moved, target) }
                reorder.settling = scope.launch {
                    animate(reorder.offset, 0f, animationSpec = tween(Motion.TRANSITION_MS)) { value, _ ->
                        reorder.offset = value
                    }
                    reorder.settled()
                }
            },
            onDragCancel = reorder::cancel,
        ) { _, amount -> reorder.drag(amount.y, rowHeight) }
    }
}

@Composable
private fun rememberSteps(move: RowMove, onMove: (AppKey, AppKey) -> Unit): List<CustomAccessibilityAction> {
    val earlier = stringResource(R.string.widget_move_before)
    val later = stringResource(R.string.widget_move_after)
    val index = move.keys.indexOf(move.key)
    return listOfNotNull(
        move.keys.getOrNull(index - 1)?.let {
            CustomAccessibilityAction(earlier) { onMove(move.key, it).let { true } }
        },
        move.keys.getOrNull(index + 1)?.let { CustomAccessibilityAction(later) { onMove(move.key, it).let { true } } },
    )
}

@Composable
private fun Modifier.slideOnMove(enabled: Boolean): Modifier {
    val scope = rememberCoroutineScope()
    val animate by rememberUpdatedState(enabled)
    var target by remember { mutableFloatStateOf(Float.NaN) }
    var slide by remember { mutableStateOf<Animatable<Float, AnimationVector1D>?>(null) }
    return onPlaced { target = it.positionInParent().y }.offset {
        val goal = target
        if (goal.isNaN()) return@offset IntOffset.Zero
        val current = slide ?: Animatable(goal).also { slide = it }
        when {
            !animate -> {
                if (current.value != goal) scope.launch { current.snapTo(goal) }
                IntOffset.Zero
            }

            else -> {
                if (current.targetValue != goal) {
                    scope.launch { current.animateTo(goal, tween(Motion.TRANSITION_MS)) }
                }
                IntOffset(0, (current.value - goal).roundToInt())
            }
        }
    }
}

private const val LIFT_SCALE = 1.03f
private const val HIGHLIGHT_ALPHA = 0.12f
