package com.bluecubelabs.env.feature.search

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.bluecubelabs.env.core.search.KeyAction
import com.bluecubelabs.env.core.search.KeySpec
import com.bluecubelabs.env.core.search.KeyboardLayout
import com.bluecubelabs.env.core.ui.EnvType
import com.bluecubelabs.env.core.ui.LocalEnvColors
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface KeyboardActions {
    fun onChar(c: Char): Boolean

    fun onBackspace(): Boolean

    fun onClear()

    fun onEnter()

    fun onSwitchLayout()

    fun onClose()
}

object KeyboardDimens {
    val rowHeight = 52.dp
    val verticalPadding = 4.dp
    val gap = 4.dp
    val radius = 6.dp
    val digitSwipe = 24.dp
    val clearSwipe = 48.dp
    val closeSwipe = 72.dp
    val bubbleWidth = 48.dp
    val bubbleHeight = 56.dp
    const val DIGIT_HOLD_MS = 300L
    const val REPEAT_START_MS = 400L
    const val REPEAT_STEP_MS = 50L
}

data class KeyGeometry(val spec: KeySpec, val rect: Rect)

object KeyboardGeometry {
    fun compute(
        layout: KeyboardLayout,
        width: Float,
        rowHeight: Float,
        topPadding: Float,
        gap: Float,
    ): List<KeyGeometry> {
        val keys = ArrayList<KeyGeometry>()
        layout.rows.forEachIndexed { rowIndex, row ->
            val inset = width * layout.rowInsets.getOrElse(rowIndex) { 0f }
            val usable = width - 2 * inset
            val totalWeight = row.sumOf { it.weight.toDouble() }.toFloat()
            var x = inset
            val top = topPadding + rowIndex * rowHeight
            row.forEach { spec ->
                val w = usable * spec.weight / totalWeight
                keys += KeyGeometry(spec, Rect(x + gap / 2, top + gap / 2, x + w - gap / 2, top + rowHeight - gap / 2))
                x += w
            }
        }
        return keys
    }

    fun hit(keys: List<KeyGeometry>, position: Offset): Int? {
        keys.forEachIndexed { i, k -> if (k.rect.contains(position)) return i }
        var best: Int? = null
        var bestDistance = Float.MAX_VALUE
        keys.forEachIndexed { i, k ->
            val dx = position.x - k.rect.center.x
            val dy = position.y - k.rect.center.y
            val inRowBand = position.y >= k.rect.top - k.rect.height && position.y <= k.rect.bottom + k.rect.height
            val d = dx * dx + dy * dy
            if (inRowBand && d < bestDistance) {
                bestDistance = d
                best = i
            }
        }
        return best
    }
}

@Stable
class KeyboardState {
    val pressed = mutableStateMapOf<Int, Boolean>()
    var bubbleKey by mutableStateOf<Int?>(null)
}

private enum class PressMode { NORMAL, DIGIT, REPEATING, CLEAR, CLOSE, COMMITTED }

private class ActivePress(val keyIndex: Int, val start: Offset) {
    var mode = PressMode.NORMAL
    var timer: Job? = null
}

@Composable
fun Keyboard(
    layout: KeyboardLayout,
    nextLayoutLabel: String,
    digitHints: Boolean,
    haptics: Boolean,
    actions: KeyboardActions,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEnvColors.current
    val density = LocalDensity.current
    val view = LocalView.current
    val state = remember { KeyboardState() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val currentActions by rememberUpdatedState(actions)
    val currentHaptics by rememberUpdatedState(haptics)
    val rowHeightPx = with(density) { KeyboardDimens.rowHeight.toPx() }
    val paddingPx = with(density) { KeyboardDimens.verticalPadding.toPx() }
    val gapPx = with(density) { KeyboardDimens.gap.toPx() }
    val keys = remember(layout, size) {
        KeyboardGeometry.compute(layout, size.width.toFloat(), rowHeightPx, paddingPx, gapPx)
    }
    val totalHeight = KeyboardDimens.rowHeight * layout.rows.size + KeyboardDimens.verticalPadding * 2
    Box(
        modifier
            .fillMaxWidth()
            .height(totalHeight)
            .background(colors.background)
            .onSizeChanged { size = it }
            .testTag("keyboard")
            .pointerInput(keys) {
                val digitSwipePx = KeyboardDimens.digitSwipe.toPx()
                val clearSwipePx = KeyboardDimens.clearSwipe.toPx()
                val closeSwipePx = KeyboardDimens.closeSwipe.toPx()
                coroutineScope {
                    val handler = KeyboardGestureHandler(
                        keys = keys,
                        state = state,
                        scope = this,
                        actions = { currentActions },
                        haptic = { type -> if (currentHaptics) view.performHapticFeedback(type) },
                        digitSwipePx = digitSwipePx,
                        clearSwipePx = clearSwipePx,
                        closeSwipePx = closeSwipePx,
                    )
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent().changes.forEach { change ->
                                when {
                                    change.pressed && !change.previousPressed -> handler.onDown(
                                        change.id,
                                        change.position,
                                    )

                                    change.pressed -> handler.onMove(change.id, change.position)

                                    else -> handler.onUp(change.id)
                                }
                                change.consume()
                            }
                        }
                    }
                }
            },
    ) {
        keys.forEachIndexed { index, key ->
            KeyView(index, key, state, nextLayoutLabel, digitHints)
        }
        state.bubbleKey?.let { index ->
            keys.getOrNull(index)?.let { DigitBubble(it) }
        }
    }
}

private class KeyboardGestureHandler(
    private val keys: List<KeyGeometry>,
    private val state: KeyboardState,
    private val scope: CoroutineScope,
    private val actions: () -> KeyboardActions,
    private val haptic: (Int) -> Unit,
    private val digitSwipePx: Float,
    private val clearSwipePx: Float,
    private val closeSwipePx: Float,
) {
    private val active = LinkedHashMap<PointerId, ActivePress>()

    fun onDown(id: PointerId, position: Offset) {
        active.values.toList().forEach(::commit)
        active.clear()
        val index = KeyboardGeometry.hit(keys, position) ?: return
        val press = ActivePress(index, position)
        active[id] = press
        state.pressed[index] = true
        haptic(HapticFeedbackConstants.KEYBOARD_TAP)
        val spec = keys[index].spec
        press.timer = when {
            spec.digit != null -> scope.launch { holdForDigit(press) }
            spec.action == KeyAction.BACKSPACE -> scope.launch { repeatBackspace(press) }
            else -> null
        }
    }

    fun onMove(id: PointerId, position: Offset) {
        val press = active[id] ?: return
        val spec = keys[press.keyIndex].spec
        val delta = position - press.start
        if (spec.digit != null && delta.y <= -digitSwipePx && press.mode == PressMode.NORMAL) {
            press.timer?.cancel()
            press.mode = PressMode.DIGIT
            state.bubbleKey = press.keyIndex
        }
        if (spec.action == KeyAction.BACKSPACE && delta.x <= -clearSwipePx && press.mode != PressMode.CLEAR) {
            press.timer?.cancel()
            press.mode = PressMode.CLEAR
        }
        if (delta.y >= closeSwipePx && abs(delta.x) < delta.y && press.mode in CLOSABLE) {
            press.timer?.cancel()
            press.mode = PressMode.CLOSE
            if (state.bubbleKey == press.keyIndex) state.bubbleKey = null
        }
    }

    fun onUp(id: PointerId) {
        active.remove(id)?.let(::commit)
    }

    private suspend fun holdForDigit(press: ActivePress) {
        delay(KeyboardDimens.DIGIT_HOLD_MS)
        if (press.mode == PressMode.NORMAL) {
            press.mode = PressMode.DIGIT
            state.bubbleKey = press.keyIndex
        }
    }

    private suspend fun repeatBackspace(press: ActivePress) {
        delay(KeyboardDimens.REPEAT_START_MS)
        if (press.mode != PressMode.NORMAL) return
        press.mode = PressMode.REPEATING
        while (press.mode == PressMode.REPEATING) {
            if (actions().onBackspace()) haptic(HapticFeedbackConstants.KEYBOARD_TAP)
            delay(KeyboardDimens.REPEAT_STEP_MS)
        }
    }

    private fun commit(press: ActivePress) {
        press.timer?.cancel()
        val spec = keys[press.keyIndex].spec
        val target = actions()
        when (press.mode) {
            PressMode.COMMITTED, PressMode.REPEATING -> Unit

            PressMode.CLEAR -> target.onClear()

            PressMode.CLOSE -> target.onClose()

            PressMode.DIGIT -> spec.digit?.let(::type)

            PressMode.NORMAL -> when (spec.action) {
                KeyAction.BACKSPACE -> target.onBackspace()
                KeyAction.SWITCH_LAYOUT -> target.onSwitchLayout()
                null -> spec.char?.let(::type)
            }
        }
        press.mode = PressMode.COMMITTED
        state.pressed.remove(press.keyIndex)
        if (state.bubbleKey == press.keyIndex) state.bubbleKey = null
    }

    private fun type(c: Char) {
        if (!actions().onChar(c)) haptic(HapticFeedbackConstants.REJECT)
    }

    private companion object {
        val CLOSABLE = setOf(PressMode.NORMAL, PressMode.DIGIT)
    }
}

@Composable
private fun KeyView(index: Int, key: KeyGeometry, state: KeyboardState, nextLayoutLabel: String, digitHints: Boolean) {
    val colors = LocalEnvColors.current
    val density = LocalDensity.current
    val pressed by remember(index) { derivedStateOf { state.pressed[index] == true } }
    val label = when (key.spec.action) {
        KeyAction.BACKSPACE -> "⌫"
        KeyAction.SWITCH_LAYOUT -> nextLayoutLabel
        null -> key.spec.char?.toString().orEmpty()
    }
    val widthDp = with(density) { key.rect.width.toDp() }
    val heightDp = with(density) { key.rect.height.toDp() }
    Box(
        Modifier
            .offset { IntOffset(key.rect.left.roundToInt(), key.rect.top.roundToInt()) }
            .size(widthDp, heightDp)
            .clip(RoundedCornerShape(KeyboardDimens.radius))
            .background(if (pressed) colors.keyPressed else colors.keyBackground)
            .testTag("key_${key.spec.char ?: key.spec.action?.name}"),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            label,
            style = EnvType.style(
                if (key.spec.action ==
                    null
                ) {
                    EnvType.keySize
                } else {
                    EnvType.secondarySize
                },
                colors.textPrimary,
            ),
        )
        val digit = key.spec.digit
        if (digitHints && digit != null) {
            BasicText(
                digit.toString(),
                style = EnvType.style(EnvType.digitHintSize, colors.textSecondary),
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 4.dp),
            )
        }
    }
}

@Composable
private fun DigitBubble(key: KeyGeometry) {
    val colors = LocalEnvColors.current
    val density = LocalDensity.current
    val digit = key.spec.digit ?: return
    val bubbleW = with(density) { KeyboardDimens.bubbleWidth.toPx() }
    val bubbleH = with(density) { KeyboardDimens.bubbleHeight.toPx() }
    val x = (key.rect.center.x - bubbleW / 2).roundToInt()
    val y = (key.rect.top - bubbleH).roundToInt()
    Box(
        Modifier
            .offset { IntOffset(x, y) }
            .size(KeyboardDimens.bubbleWidth, KeyboardDimens.bubbleHeight)
            .clip(RoundedCornerShape(KeyboardDimens.radius))
            .background(colors.keyPressed)
            .testTag("digit_bubble"),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(digit.toString(), style = EnvType.style(EnvType.querySize, colors.textPrimary))
    }
}
