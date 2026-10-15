package com.mushinlab.launcher.feature.search

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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.search.GestureDecoder
import com.mushinlab.launcher.core.search.KeyAction
import com.mushinlab.launcher.core.search.KeySpec
import com.mushinlab.launcher.core.search.KeyboardLayout
import com.mushinlab.launcher.core.search.KeyboardLayouts
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.LocalMushinStroke
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.drawVectors
import kotlin.math.roundToInt

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
    val callbacks = KeyboardCallbacks(
        actions = { currentActions },
        glide = { points, vertices ->
            val letters = rowHeightPx * KeyboardLayouts.LETTER_ROWS
            val shape = vertices.map {
                it.copy(x = it.x / size.width, y = (it.y - paddingPx) / letters)
            }
            currentGlide.onGlide(decoder, points, shape)
        },
        haptic = { type -> if (currentHaptics) view.performHapticFeedback(type) },
    )
    val totalHeight = KeyboardDimens.rowHeight * layout.rowScales.sum() + KeyboardDimens.verticalPadding * 2
    Box(
        modifier
            .fillMaxWidth()
            .height(totalHeight)
            .onSizeChanged { size = it }
            .systemGestureExclusion()
            .testTag("keyboard")
            .pointerInput(keys, decoder) {
                handleKeys(keys, state, callbacks, rowHeightPx * VECTOR_TOLERANCE)
            },
    ) {
        keys.forEachIndexed { index, key ->
            KeyView(index, key, state, nextLayoutLabel, digitHints, drawn)
        }
        GlideTrail(state, Modifier.matchParentSize().clipToBounds().testTag("glide_trail"))
        state.bubbleKey?.let { index ->
            keys.getOrNull(index)?.let { DigitBubble(it) }
        }
    }
}

@Composable
private fun GlideTrail(state: KeyboardState, modifier: Modifier) {
    val colors = LocalMushinColors.current
    val stroke = LocalMushinStroke.current
    val strokeColor = stroke.color(colors)
    val strokePx = with(LocalDensity.current) { stroke.width.toPx() }
    Canvas(modifier) {
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
    val colors = LocalMushinColors.current
    val density = LocalDensity.current
    val pressed by remember(index) { derivedStateOf { state.pressed[index] == true } }
    val label = keyLabel(key.spec, drawn, nextLayoutLabel)
    val size = keyTextSize(key.spec, drawn)
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
        BasicText(label, style = MushinType.style(size, colors.textPrimary))
        if (key.spec.action == KeyAction.RESIZE) ResizeMark(colors.textPrimary)
        val digit = key.spec.digit
        if (digitHints && digit != null) {
            BasicText(
                digit.toString(),
                style = MushinType.style(MushinType.digitHintSize, colors.textSecondary),
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 4.dp),
            )
        }
    }
}

private fun keyLabel(spec: KeySpec, drawn: Boolean, nextLayoutLabel: String): String = when (spec.action) {
    KeyAction.BACKSPACE -> if (drawn) "×" else "⌫"
    KeyAction.SWITCH_LAYOUT -> nextLayoutLabel
    KeyAction.SPACE, KeyAction.RESIZE -> ""
    null -> spec.char?.toString().orEmpty()
}

private fun keyTextSize(spec: KeySpec, drawn: Boolean): TextUnit = when (spec.action) {
    null -> MushinType.keySize
    KeyAction.BACKSPACE -> if (drawn) KeyboardDimens.crossSize else MushinType.keySize
    else -> MushinType.secondarySize
}

@Composable
private fun DigitBubble(key: KeyGeometry) {
    val colors = LocalMushinColors.current
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
        BasicText(digit.toString(), style = MushinType.style(MushinType.querySize, colors.textPrimary))
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
