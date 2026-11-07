package com.mushinlab.launcher.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import kotlin.math.hypot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Handles clicks with a clipped gradient wave expanding from the touch position. */
@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    pressedColor: Color = LocalMushinColors.current.keyPressed,
    role: Role = Role.Button,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val wave = rememberWave()
    LaunchedEffect(source, enabled) {
        if (!enabled) wave.release()
        source.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> wave.press(interaction.pressPosition)
                is PressInteraction.Release, is PressInteraction.Cancel -> wave.release()
            }
        }
    }
    return this.wave(wave, pressedColor).combinedClickable(
        interactionSource = source,
        indication = null,
        enabled = enabled,
        role = role,
        onLongClick = onLongClick,
        onClick = onClick,
    )
}

/** Adds the same wave to controls whose gesture detector supplies [pressed]. */
@Composable
fun Modifier.pressFeedback(pressed: Boolean, pressedColor: Color = LocalMushinColors.current.keyPressed): Modifier {
    val wave = rememberWave()
    LaunchedEffect(pressed) {
        if (pressed) wave.press(wave.origin) else wave.release()
    }
    return this
        .pointerInput(wave) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    event.changes.firstOrNull { it.pressed && !it.previousPressed }?.let { wave.origin = it.position }
                }
            }
        }
        .wave(wave, pressedColor)
}

@Composable
private fun rememberWave(): PressWave {
    val scope = rememberCoroutineScope()
    return remember(scope) { PressWave(scope) }
}

private class PressWave(private val scope: CoroutineScope) {
    val radius = Animatable(0f)
    val opacity = Animatable(0f)
    var origin = Offset.Unspecified
    private var expansion: Job? = null
    private var fading: Job? = null

    fun press(position: Offset) {
        origin = position
        expansion?.cancel()
        fading?.cancel()
        fading = scope.launch(start = CoroutineStart.UNDISPATCHED) { opacity.snapTo(1f) }
        expansion = scope.launch {
            radius.snapTo(0f)
            radius.animateTo(1f, tween(Motion.TRANSITION_MS))
        }
    }

    fun release() {
        fading?.cancel()
        fading = scope.launch { opacity.animateTo(0f, tween(Motion.PRESS_FADE_MS)) }
    }
}

private fun Modifier.wave(wave: PressWave, color: Color): Modifier = drawWithContent {
    drawContent()
    val opacity = wave.opacity.value
    if (opacity <= 0f || color.alpha <= 0f) return@drawWithContent
    val origin = if (wave.origin == Offset.Unspecified) center else wave.origin
    val radius = hypot(maxOf(origin.x, size.width - origin.x), maxOf(origin.y, size.height - origin.y))
    val brush = Brush.radialGradient(
        listOf(color.copy(alpha = color.alpha * WAVE_ALPHA * opacity), Color.Transparent),
        center = origin,
        radius = (radius * wave.radius.value).coerceAtLeast(1f),
    )
    clipRect { drawRect(brush) }
}

private const val WAVE_ALPHA = 0.55f
