package com.mushinlab.launcher.feature.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.mushinlab.launcher.core.model.ShapeVertex
import com.mushinlab.launcher.core.search.GestureDecoder
import com.mushinlab.launcher.core.search.GesturePoint
import com.mushinlab.launcher.core.search.GestureSample
import com.mushinlab.launcher.core.search.Shapes
import com.mushinlab.launcher.core.search.TimedPoint
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.LocalMushinStroke
import com.mushinlab.launcher.core.ui.Motion
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.drawVectors
import com.mushinlab.launcher.core.ui.pressable
import kotlinx.coroutines.launch

@Composable
internal fun GestureSheet(
    label: String,
    stack: SettingsStack,
    draft: List<ShapeVertex>?,
    onAdd: (GestureSample) -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalMushinColors.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val drag = remember { Animatable(0f) }
    val currentClose by rememberUpdatedState(onClose)
    DisposableEffect(stack) {
        stack.sheet = { currentClose() }
        onDispose { stack.sheet = null }
    }
    val pad = remember { PadState(draft) }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SHEET_SCRIM))
            .pointerInput(Unit) { detectTapGestures { currentClose() } }
            .testTag("gesture_sheet_scrim"),
    ) {
        val sheetHeight = maxHeight * SHEET_FRACTION
        val sheetPx = with(density) { sheetHeight.toPx() }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(sheetHeight)
                .graphicsLayer { translationY = drag.value }
                .clip(RoundedCornerShape(topStart = SHEET_RADIUS, topEnd = SHEET_RADIUS))
                .background(colors.surface)
                .pointerInput(Unit) { detectTapGestures { } }
                .testTag("gesture_sheet"),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(HANDLE_AREA)
                    .pointerInput(sheetPx) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    if (drag.value > sheetPx * CLOSE_FRACTION) {
                                        currentClose()
                                    } else {
                                        drag.animateTo(0f, tween(Motion.TRANSITION_MS))
                                    }
                                }
                            },
                        ) { change, amount ->
                            change.consume()
                            scope.launch { drag.snapTo((drag.value + amount).coerceAtLeast(0f)) }
                        }
                    }
                    .testTag("gesture_sheet_handle"),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(36.dp, 4.dp)
                        .background(colors.textSecondary.copy(alpha = HANDLE_ALPHA), RoundedCornerShape(2.dp)),
                )
            }
            BasicText(
                label,
                style = MushinType.style(20.sp, colors.textPrimary, FontWeight.Medium),
                modifier = Modifier.padding(horizontal = CARD_PADDING),
            )
            Spacer(Modifier.height(4.dp))
            val hint = when {
                pad.fromDraft -> R.string.memory_sheet_draft
                pad.ready -> R.string.memory_sheet_result
                pad.points.isNotEmpty() -> R.string.memory_sheet_short
                else -> R.string.memory_sheet_hint
            }
            BasicText(
                stringResource(hint),
                style = MushinType.style(15.sp, colors.textSecondary),
                modifier = Modifier.padding(horizontal = CARD_PADDING).testTag("gesture_sheet_hint"),
            )
            Spacer(Modifier.weight(1f))
            DrawPad(pad)
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = CARD_PADDING),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SheetButton(stringResource(R.string.settings_cancel), "sheet_cancel", Modifier.weight(1f)) {
                    currentClose()
                }
                SheetButton(stringResource(R.string.memory_repeat), "sheet_repeat", Modifier.weight(1f)) {
                    pad.reset()
                }
                SheetButton(
                    stringResource(R.string.memory_add),
                    "sheet_add",
                    Modifier.weight(1f),
                    accent = true,
                    enabled = pad.ready,
                ) { pad.sample()?.let(onAdd) }
            }
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Stable
private class PadState(private var draft: List<ShapeVertex>?) {
    val points = mutableStateListOf<Offset>()
    var vectors by mutableStateOf<List<ShapeVertex>>(emptyList())
    var fromDraft by mutableStateOf(draft != null)
    var size = Size.Zero
    val ready: Boolean get() = vectors.size >= 2

    fun place(size: Size) {
        this.size = size
        val shape = draft ?: return
        draft = null
        val scale = minOf(size.width, size.height) * DRAFT_SCALE
        vectors = shape.map { ShapeVertex(size.width / 2 + it.x * scale, size.height / 2 + it.y * scale) }
    }

    fun reset() {
        points.clear()
        vectors = emptyList()
        fromDraft = false
    }

    fun sample(): GestureSample? {
        if (!ready || size == Size.Zero) return null
        val centers = qwertyCenters(size).mapValues { (_, c) -> GesturePoint(c.x, c.y) }
        val path = GestureDecoder(centers).keysAlong(vectors.map { GesturePoint(it.x, it.y) })
        return GestureSample(path, vectors.map { it.copy(x = it.x / size.width, y = it.y / size.height) })
    }
}

@Composable
private fun DrawPad(pad: PadState) {
    val colors = LocalMushinColors.current
    val stroke = LocalMushinStroke.current
    val strokeColor = stroke.color(colors)
    val measurer = rememberTextMeasurer()
    val style = MushinType.style(18.sp, colors.textPrimary)
    val letters = remember(measurer, style) {
        QWERTY.flatMap { it.first.toList() }.associateWith { measurer.measure(it.toString(), style) }
    }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(PAD_ROW * QWERTY.size)
            .clipToBounds()
            .onSizeChanged { pad.place(it.toSize()) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    pad.reset()
                    pad.points += down.position
                    val timed = mutableListOf(TimedPoint(down.position.x, down.position.y, down.uptimeMillis))
                    var change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    while (change != null && change.pressed) {
                        change.consume()
                        pad.points += change.position
                        timed += TimedPoint(change.position.x, change.position.y, change.uptimeMillis)
                        change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    }
                    change?.consume()
                    val rowHeight = size.height.toFloat() / QWERTY.size
                    pad.vectors = Shapes.vectorize(timed, rowHeight * VECTOR_TOLERANCE, viewConfiguration.touchSlop)
                }
            }
            .testTag("gesture_pad"),
    ) {
        val centers = qwertyCenters(size)
        val keyWidth = size.width / QWERTY.first().first.length
        val rowHeight = size.height / QWERTY.size
        val gap = PAD_GAP.toPx()
        letters.forEach { (char, layout) ->
            val center = centers.getValue(char)
            drawRoundRect(
                colors.keyBackground,
                topLeft = Offset(center.x - keyWidth / 2 + gap / 2, center.y - rowHeight / 2 + gap / 2),
                size = Size(keyWidth - gap, rowHeight - gap),
                cornerRadius = CornerRadius(CARD_RADIUS.toPx()),
            )
            drawText(layout, topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f))
        }
        val width = stroke.width.toPx()
        val raw = polyline(pad.points)
        val rawColor = if (pad.ready) strokeColor.copy(alpha = RAW_ALPHA) else strokeColor
        raw?.let { drawPath(it, rawColor, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
        drawVectors(pad.vectors, strokeColor, width, stroke.dots)
    }
}

private fun polyline(points: List<Offset>): Path? {
    if (points.size < 2) return null
    return Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
}

@Composable
private fun SheetButton(
    label: String,
    tag: String,
    modifier: Modifier,
    accent: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = LocalMushinColors.current
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(CARD_RADIUS))
            .background(if (accent) colors.accent else colors.keyBackground)
            .pressable(enabled = enabled, onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        val text = if (accent) colors.background else colors.textPrimary
        BasicText(
            label,
            style = MushinType.style(16.sp, text.copy(alpha = if (enabled) 1f else DISABLED_ALPHA), FontWeight.Medium),
        )
    }
}

private const val SHEET_FRACTION = 0.7f
private const val CLOSE_FRACTION = 0.25f
private const val SHEET_SCRIM = 0.5f
private const val HANDLE_ALPHA = 0.5f
private const val RAW_ALPHA = 0.3f
private val SHEET_RADIUS = 16.dp
private val HANDLE_AREA = 32.dp
private val PAD_ROW = 66.dp
private val PAD_GAP = 4.dp
private const val VECTOR_TOLERANCE = 0.2f
private const val DRAFT_SCALE = 0.3f
