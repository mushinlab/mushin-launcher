package com.mushinlab.launcher.feature.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.Motion
import com.mushinlab.launcher.core.ui.PopupStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun SettingsSheet(
    tag: String,
    onClose: () -> Unit,
    heightFraction: Float? = null,
    scrimAlpha: Float = SHEET_SCRIM,
    color: Color = LocalMushinColors.current.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalMushinColors.current
    val scope = rememberCoroutineScope()
    val drag = remember { Animatable(0f) }
    var sheetPx by remember { mutableFloatStateOf(0f) }
    val currentClose by rememberUpdatedState(onClose)
    val shape = RoundedCornerShape(topStart = SHEET_RADIUS, topEnd = SHEET_RADIUS)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha))
            .pointerInput(Unit) { detectTapGestures { currentClose() } }
            .testTag("${tag}_scrim"),
    ) {
        val height = heightFraction?.let { Modifier.height(maxHeight * it) }
            ?: Modifier.heightIn(max = maxHeight * MAX_FRACTION)
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .then(height)
                .onSizeChanged { sheetPx = it.height.toFloat() }
                .graphicsLayer { translationY = drag.value }
                .clip(shape)
                .background(color)
                .border(PopupStyle.outlineWidth, PopupStyle.outline(colors), shape)
                .pointerInput(Unit) { detectTapGestures { } }
                .testTag(tag),
        ) {
            SheetHandle(drag, { sheetPx }, scope, "${tag}_handle") { currentClose() }
            content()
        }
    }
}

@Composable
private fun SheetHandle(
    drag: Animatable<Float, AnimationVector1D>,
    sheetPx: () -> Float,
    scope: CoroutineScope,
    tag: String,
    onClose: () -> Unit,
) {
    val colors = LocalMushinColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(HANDLE_AREA)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (drag.value > sheetPx() * CLOSE_FRACTION) {
                                onClose()
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
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(36.dp, 4.dp)
                .background(colors.textSecondary.copy(alpha = HANDLE_ALPHA), RoundedCornerShape(2.dp)),
        )
    }
}

private const val MAX_FRACTION = 0.85f
private const val CLOSE_FRACTION = 0.25f
private const val SHEET_SCRIM = 0.5f
private const val HANDLE_ALPHA = 0.5f
private val SHEET_RADIUS = 16.dp
private val HANDLE_AREA = 32.dp
