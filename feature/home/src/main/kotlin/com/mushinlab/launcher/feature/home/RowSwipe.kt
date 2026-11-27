package com.mushinlab.launcher.feature.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.ui.Motion
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal object SwipeResistance {
    fun offset(pull: Float, limit: Float): Float = if (pull <= 0f) 0f else limit * (1f - exp(-pull / limit))

    fun pull(offset: Float, limit: Float): Float =
        if (offset <= 0f) 0f else -limit * ln(1f - (offset / limit).coerceAtMost(MAX_FRACTION))

    private const val MAX_FRACTION = 0.99f
}

internal object SwipeDimens {
    val reveal = 72.dp
    val limit = 120.dp
    val hint = 56.dp
}

@Stable
internal class RowSwipe {
    var key by mutableStateOf<AppKey?>(null)
        private set
    var offset by mutableFloatStateOf(0f)
    var hint by mutableFloatStateOf(0f)
    var hintKey by mutableStateOf<AppKey?>(null)
    var settling: Job? = null
    private var open by mutableStateOf(false)
    private var wasOpen = false
    private var pull = 0f

    fun offsetOf(key: AppKey): Float {
        val hinted = if (key == hintKey) hint else 0f
        return if (this.key == key) max(offset, hinted) else hinted
    }

    fun isOpen(key: AppKey): Boolean = open && this.key == key

    fun begin(key: AppKey, limit: Float) {
        settling?.cancel()
        if (this.key != key) {
            this.key = key
            offset = 0f
            open = false
        }
        wasOpen = open
        open = false
        pull = SwipeResistance.pull(offset, limit)
    }

    fun drag(delta: Float, reveal: Float, limit: Float): Boolean {
        val before = offset
        pull = (pull - delta).coerceAtLeast(0f)
        offset = SwipeResistance.offset(pull, limit)
        return before < reveal && offset >= reveal
    }

    fun release(reveal: Float): Float =
        if (offset >= reveal * (if (wasOpen) KEEP_OPEN_FRACTION else 1f)) reveal else 0f

    fun settled(reveal: Float) {
        open = offset >= reveal
        if (!open) close()
    }

    fun close() {
        settling?.cancel()
        key = null
        offset = 0f
        open = false
    }

    private companion object {
        const val KEEP_OPEN_FRACTION = 0.5f
    }
}

@Composable
internal fun Modifier.swipeToReveal(key: AppKey, swipe: RowSwipe): Modifier {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val reveal = with(LocalDensity.current) { SwipeDimens.reveal.toPx() }
    val limit = with(LocalDensity.current) { SwipeDimens.limit.toPx() }
    val settle = { target: Float ->
        swipe.settling?.cancel()
        swipe.settling = scope.launch {
            animate(swipe.offset, target, animationSpec = tween(Motion.TRANSITION_MS)) { value, _ ->
                swipe.offset = value
            }
            swipe.settled(reveal)
        }
    }
    return pointerInput(key, reveal, limit) {
        detectHorizontalDragGestures(
            onDragStart = { swipe.begin(key, limit) },
            onDragEnd = { settle(swipe.release(reveal)) },
            onDragCancel = { settle(swipe.release(reveal)) },
        ) { _, amount ->
            if (swipe.drag(amount, reveal, limit)) haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
        }
    }.pointerInput(key) {
        detectTapGestures { if (swipe.isOpen(key)) settle(0f) }
    }
}

internal class RowEdits(val reorder: RowReorder, val swipe: RowSwipe, val editing: Boolean)

@Composable
internal fun SwipeEffects(swipe: RowSwipe, reorder: RowReorder, rows: List<HomeRow>, editing: Boolean) {
    val hint = with(LocalDensity.current) { SwipeDimens.hint.toPx() }
    LaunchedEffect(rows, reorder.dragged) {
        if (reorder.dragged != null || rows.none { it.key == swipe.key }) swipe.close()
    }
    LaunchedEffect(editing) {
        swipe.close()
        swipe.hint = 0f
        swipe.hintKey = rows.firstOrNull()?.key
        if (editing) {
            animate(0f, 0f, animationSpec = hintKeyframes(hint)) { value, _ -> swipe.hint = value }
        }
    }
}

private fun hintKeyframes(hint: Float) = keyframes {
    durationMillis = Motion.TRANSITION_MS + HINT_PULL_MS * 2 + HINT_HOLD_MS
    0f at Motion.TRANSITION_MS using FastOutSlowInEasing
    hint at Motion.TRANSITION_MS + HINT_PULL_MS
    hint at Motion.TRANSITION_MS + HINT_PULL_MS + HINT_HOLD_MS using FastOutSlowInEasing
}

private const val HINT_PULL_MS = 600
private const val HINT_HOLD_MS = 300
