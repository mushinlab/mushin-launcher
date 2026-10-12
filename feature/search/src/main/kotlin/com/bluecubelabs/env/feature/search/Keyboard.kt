package com.bluecubelabs.env.feature.search

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluecubelabs.env.core.model.ShapeVertex
import com.bluecubelabs.env.core.search.GestureDecoder
import com.bluecubelabs.env.core.search.GesturePoint
import com.bluecubelabs.env.core.search.KeyAction
import com.bluecubelabs.env.core.search.KeySpec
import com.bluecubelabs.env.core.search.KeyboardLayout
import com.bluecubelabs.env.core.search.KeyboardLayouts
import com.bluecubelabs.env.core.search.Shapes
import com.bluecubelabs.env.core.search.TimedPoint
import com.bluecubelabs.env.core.ui.EnvType
import com.bluecubelabs.env.core.ui.LocalEnvColors
import com.bluecubelabs.env.core.ui.LocalEnvStroke
import com.bluecubelabs.env.core.ui.drawVectors
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

    fun onSwitchLayout()

    fun onClose()

    fun onResize()
}

object KeyboardDimens {
    val rowHeight = 66.dp
    val verticalPadding = 4.dp
    val bottomLift = rowHeight
    val gap = 4.dp
    val radius = 6.dp
    val digitSwipe = 24.dp
    val clearSwipe = 48.dp
    val bubbleWidth = 48.dp
    val bubbleHeight = 56.dp
    val glideStart = 20.dp
    val crossSize = 32.sp
    val resizeMark = 22.dp
    const val DWELL_MS = 1000L
    const val VERTICAL_RATIO = 0.5f
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
        var top = topPadding
        layout.rows.forEachIndexed { rowIndex, row ->
            val inset = width * layout.rowInsets.getOrElse(rowIndex) { 0f }
            val usable = width - 2 * inset
            val totalWeight = row.sumOf { it.weight.toDouble() }.toFloat()
            val height = rowHeight * layout.rowScales.getOrElse(rowIndex) { 1f }
            var x = inset
            row.forEach { spec ->
                val w = usable * spec.weight / totalWeight
                keys += KeyGeometry(spec, Rect(x + gap / 2, top + gap / 2, x + w - gap / 2, top + height - gap / 2))
                x += w
            }
            top += height
        }
        return keys
    }

    fun height(layout: KeyboardLayout, rowHeight: Float, padding: Float): Float =
        rowHeight * layout.rowScales.sum() + padding * 2

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
    val trail = mutableStateListOf<Offset>()
    val dwells = mutableStateListOf<Offset>()
    var vectors by mutableStateOf<List<ShapeVertex>>(emptyList())

    fun clearDrawing() {
        trail.clear()
        dwells.clear()
        vectors = emptyList()
    }
}

private enum class PressMode { NORMAL, DIGIT, REPEATING, CLEAR, GLIDE, COMMITTED }

private class ActivePress(val keyIndex: Int, val start: Offset, startMs: Long) {
    var mode = PressMode.NORMAL
    var timer: Job? = null
    val points = mutableListOf(GesturePoint(start.x, start.y))
    val timed = mutableListOf(TimedPoint(start.x, start.y, startMs))
    var anchor = start
}

class Glide(
    val shiftPenalty: Float,
    val suggestMargin: Float,
    val onGlide: (GestureDecoder, List<GesturePoint>, List<ShapeVertex>) -> Boolean,
)

fun glideCenters(width: Float, rowHeight: Float, padding: Float, gap: Float): Map<Char, GesturePoint> =
    KeyboardGeometry.compute(KeyboardLayouts.EN, width, rowHeight, padding, gap)
        .mapNotNull { key -> key.spec.char?.takeIf { it in 'a'..'z' }?.let { it to key.rect.center } }
        .associate { (char, center) -> char to GesturePoint(center.x, center.y) }

@Composable
fun Keyboard(
    layout: KeyboardLayout,
    nextLayoutLabel: String,
    digitHints: Boolean,
    haptics: Boolean,
    actions: KeyboardActions,
    glide: Glide,
    clearTrail: Boolean,
    drawn: Boolean,
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
    val decoder = remember(size, glide.shiftPenalty, glide.suggestMargin) {
        GestureDecoder(
            glideCenters(size.width.toFloat(), rowHeightPx, paddingPx, gapPx),
            glide.shiftPenalty,
            glide.suggestMargin,
        )
    }
    val currentGlide by rememberUpdatedState(glide)
    LaunchedEffect(clearTrail) {
        if (clearTrail) state.clearDrawing()
    }
    val stroke = LocalEnvStroke.current
    val strokeColor = stroke.color(colors)
    val strokePx = with(density) { stroke.width.toPx() }
    val totalHeight = KeyboardDimens.rowHeight * layout.rowScales.sum() + KeyboardDimens.verticalPadding * 2
    Box(
        modifier
            .fillMaxWidth()
            .height(totalHeight)
            .onSizeChanged { size = it }
            .systemGestureExclusion()
            .testTag("keyboard")
            .pointerInput(keys, decoder) {
                coroutineScope {
                    val handler = KeyboardGestureHandler(
                        keys = keys,
                        state = state,
                        scope = this,
                        actions = { currentActions },
                        glide = { points, vertices ->
                            val letters = rowHeightPx * KeyboardLayouts.LETTER_ROWS
                            val shape = vertices.map {
                                it.copy(x = it.x / size.width, y = (it.y - paddingPx) / letters)
                            }
                            currentGlide.onGlide(decoder, points, shape)
                        },
                        haptic = { type -> if (currentHaptics) view.performHapticFeedback(type) },
                        thresholds = Thresholds(
                            digitSwipe = KeyboardDimens.digitSwipe.toPx(),
                            clearSwipe = KeyboardDimens.clearSwipe.toPx(),
                            glideStart = KeyboardDimens.glideStart.toPx(),
                            dwellRadius = viewConfiguration.touchSlop,
                            vectorTolerance = rowHeightPx * VECTOR_TOLERANCE,
                        ),
                    )
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent().changes.forEach { change ->
                                when {
                                    change.pressed && !change.previousPressed -> handler.onDown(
                                        change.id,
                                        change.position,
                                        change.uptimeMillis,
                                    )

                                    change.pressed -> handler.onMove(change.id, change.position, change.uptimeMillis)

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
            KeyView(index, key, state, nextLayoutLabel, digitHints, drawn)
        }
        Canvas(Modifier.matchParentSize().clipToBounds().testTag("glide_trail")) {
            if (state.trail.size > 1) {
                val path = Path().apply {
                    moveTo(state.trail.first().x, state.trail.first().y)
                    state.trail.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, strokeColor, style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                if (stroke.dots) state.dwells.forEach { drawCircle(strokeColor, radius = strokePx * 2, center = it) }
            }
            drawVectors(state.vectors, strokeColor, strokePx, stroke.dots)
        }
        state.bubbleKey?.let { index ->
            keys.getOrNull(index)?.let { DigitBubble(it) }
        }
    }
}

private class Thresholds(
    val digitSwipe: Float,
    val clearSwipe: Float,
    val glideStart: Float,
    val dwellRadius: Float,
    val vectorTolerance: Float,
)

private class KeyboardGestureHandler(
    private val keys: List<KeyGeometry>,
    private val state: KeyboardState,
    private val scope: CoroutineScope,
    private val actions: () -> KeyboardActions,
    private val glide: (List<GesturePoint>, List<ShapeVertex>) -> Boolean,
    private val haptic: (Int) -> Unit,
    private val thresholds: Thresholds,
) {
    private val active = LinkedHashMap<PointerId, ActivePress>()

    fun onDown(id: PointerId, position: Offset, timeMs: Long) {
        active.values.toList().forEach(::commit)
        active.clear()
        state.clearDrawing()
        val index = KeyboardGeometry.hit(keys, position) ?: return
        val press = ActivePress(index, position, timeMs)
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

    fun onMove(id: PointerId, position: Offset, timeMs: Long) {
        val press = active[id] ?: return
        val spec = keys[press.keyIndex].spec
        val delta = position - press.start
        press.points += GesturePoint(position.x, position.y)
        press.timed += TimedPoint(position.x, position.y, timeMs)
        if (press.mode == PressMode.GLIDE) {
            state.trail += position
            if ((position - press.anchor).getDistance() > thresholds.dwellRadius) {
                press.anchor = position
                press.timer?.cancel()
                press.timer = scope.launch { dwell(press) }
            }
            return
        }
        if (press.mode == PressMode.NORMAL && startsGlide(press, spec, position, delta)) {
            press.timer?.cancel()
            press.mode = PressMode.GLIDE
            state.pressed.remove(press.keyIndex)
            press.points.forEach { state.trail += Offset(it.x, it.y) }
            press.anchor = position
            press.timer = scope.launch { dwell(press) }
            return
        }
        val digitSwipePx = thresholds.digitSwipe
        val clearSwipePx = thresholds.clearSwipe
        if (spec.digit != null && delta.y <= -digitSwipePx && press.mode == PressMode.NORMAL) {
            press.timer?.cancel()
            press.mode = PressMode.DIGIT
            state.bubbleKey = press.keyIndex
        }
        if (spec.action == KeyAction.BACKSPACE && delta.x <= -clearSwipePx && press.mode != PressMode.CLEAR) {
            press.timer?.cancel()
            press.mode = PressMode.CLEAR
        }
    }

    fun onUp(id: PointerId) {
        active.remove(id)?.let(::commit)
    }

    private fun startsGlide(press: ActivePress, spec: KeySpec, position: Offset, delta: Offset): Boolean {
        val letter = spec.action == null && spec.char != null
        val far = delta.getDistance() >= thresholds.glideStart
        val toDigit = spec.digit != null && delta.y < 0f && abs(delta.x) < -delta.y * KeyboardDimens.VERTICAL_RATIO
        return letter && far && !toDigit && KeyboardGeometry.hit(keys, position) != press.keyIndex
    }

    private suspend fun dwell(press: ActivePress) {
        while (press.mode == PressMode.GLIDE) {
            delay(KeyboardDimens.DWELL_MS)
            if (press.mode != PressMode.GLIDE) return
            press.points += GesturePoint(press.anchor.x, press.anchor.y, repeat = true)
            state.dwells += press.anchor
            haptic(HapticFeedbackConstants.KEYBOARD_TAP)
        }
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

            PressMode.GLIDE -> {
                val vertices = Shapes.vectorize(press.timed, thresholds.vectorTolerance, thresholds.dwellRadius)
                state.trail.clear()
                state.vectors = vertices
                if (!glide(press.points, vertices)) haptic(HapticFeedbackConstants.REJECT)
            }

            PressMode.CLEAR -> target.onClear()

            PressMode.DIGIT -> spec.digit?.let(::type)

            PressMode.NORMAL -> when (spec.action) {
                KeyAction.BACKSPACE -> target.onBackspace()
                KeyAction.SWITCH_LAYOUT -> target.onSwitchLayout()
                KeyAction.SPACE -> type(' ')
                KeyAction.RESIZE -> target.onResize()
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
}

@Composable
private fun KeyView(
    index: Int,
    key: KeyGeometry,
    state: KeyboardState,
    nextLayoutLabel: String,
    digitHints: Boolean,
    drawn: Boolean,
) {
    val colors = LocalEnvColors.current
    val density = LocalDensity.current
    val pressed by remember(index) { derivedStateOf { state.pressed[index] == true } }
    val label = when (key.spec.action) {
        KeyAction.BACKSPACE -> if (drawn) "×" else "⌫"
        KeyAction.SWITCH_LAYOUT -> nextLayoutLabel
        KeyAction.SPACE, KeyAction.RESIZE -> ""
        null -> key.spec.char?.toString().orEmpty()
    }
    val size = when (key.spec.action) {
        null -> EnvType.keySize
        KeyAction.BACKSPACE -> if (drawn) KeyboardDimens.crossSize else EnvType.keySize
        else -> EnvType.secondarySize
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
        BasicText(label, style = EnvType.style(size, colors.textPrimary))
        if (key.spec.action == KeyAction.RESIZE) ResizeMark(colors.textPrimary)
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

@Composable
private fun ResizeMark(color: Color) {
    Canvas(Modifier.size(KeyboardDimens.resizeMark).testTag("resize_mark")) {
        val w = size.width
        val h = size.height
        val stroke = w * RESIZE_STROKE
        val mid = h / 2
        drawLine(color, Offset(stroke, h * 0.2f), Offset(stroke, h * 0.8f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w - stroke, h * 0.2f), Offset(w - stroke, h * 0.8f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.22f, mid), Offset(w * 0.78f, mid), stroke, StrokeCap.Round)
        val head = w * 0.14f
        listOf(w * 0.22f to 1f, w * 0.78f to -1f).forEach { (x, dir) ->
            drawLine(color, Offset(x, mid), Offset(x + head * dir, mid - head), stroke, StrokeCap.Round)
            drawLine(color, Offset(x, mid), Offset(x + head * dir, mid + head), stroke, StrokeCap.Round)
        }
    }
}

private const val RESIZE_STROKE = 0.08f
private const val VECTOR_TOLERANCE = 0.2f
