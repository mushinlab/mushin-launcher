package com.mushinlab.launcher.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.model.PressFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A 1 dp full-width line in the palette divider color. */
@Composable
fun HorizontalDivider(modifier: Modifier = Modifier, color: Color = LocalMushinColors.current.divider) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

/** A 1 dp vertical line in the palette divider color; height comes from [modifier]. */
@Composable
fun VerticalDivider(modifier: Modifier = Modifier, color: Color = LocalMushinColors.current.divider) {
    Box(modifier.width(1.dp).background(color))
}

/** One line of text, ellipsized at the end. */
@Composable
fun SingleLineText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** Press style used by [pressable] and [pressFeedback] in the current subtree. */
val LocalPressFeedback = staticCompositionLocalOf { PressFeedback.FLASH }

/**
 * Click and long-click handling with the [PressFeedback] style from [LocalPressFeedback] instead of ripple.
 *
 * A short tap still flashes the feedback once. A transparent [pressedColor] disables visual feedback.
 */
@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    pressedColor: Color = LocalMushinColors.current.keyPressed,
    role: Role = Role.Button,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    val style = LocalPressFeedback.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val level = rememberPressLevel(pressed, style)
    val scope = rememberCoroutineScope()
    val visible = enabled && pressedColor.alpha > 0f
    return this
        .then(if (visible) Modifier.pressFeedback({ level.value }, pressedColor) else Modifier)
        .combinedClickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onLongClick = onLongClick,
            onClick = {
                if (visible && level.value == 0f) {
                    scope.launch {
                        level.snapTo(1f)
                        level.release(style, quick = true)
                    }
                }
                onClick()
            },
        )
}

/**
 * The visual part of [pressable] for elements whose touches are handled elsewhere: shows the [LocalPressFeedback]
 * style while [pressed] is true and releases it the same way. A transparent [pressedColor] disables it.
 */
@Composable
fun Modifier.pressFeedback(pressed: Boolean, pressedColor: Color = LocalMushinColors.current.keyPressed): Modifier {
    val level = rememberPressLevel(pressed, LocalPressFeedback.current)
    return if (pressedColor.alpha > 0f) pressFeedback({ level.value }, pressedColor) else this
}

/** Draws the [LocalPressFeedback] style at strength [level], from 0 (released) to 1 (pressed). */
@Composable
fun Modifier.pressFeedback(level: () -> Float, pressedColor: Color = LocalMushinColors.current.keyPressed): Modifier {
    val style = LocalPressFeedback.current
    val accent = LocalMushinColors.current.accent
    return when (style) {
        PressFeedback.DIM -> graphicsLayer { alpha = 1f - DIM_AMOUNT * level() }

        PressFeedback.SCALE -> graphicsLayer {
            scaleX = 1f - SCALE_AMOUNT * level()
            scaleY = scaleX
        }

        else -> {
            val color = if (style == PressFeedback.ACCENT) accent.copy(alpha = ACCENT_ALPHA) else pressedColor
            drawBehind {
                val value = level()
                if (value > 0f) drawRect(color, alpha = value)
            }
        }
    }
}

@Composable
private fun rememberPressLevel(pressed: Boolean, style: PressFeedback): Animatable<Float, AnimationVector1D> {
    val level = remember { Animatable(0f) }
    LaunchedEffect(pressed) {
        if (pressed) {
            level.snapTo(1f)
        } else if (level.value > 0f) {
            level.release(style, quick = false)
        }
    }
    return level
}

private suspend fun Animatable<Float, *>.release(style: PressFeedback, quick: Boolean) {
    if (style == PressFeedback.HOLD) {
        if (quick) delay(Motion.PRESS_HOLD_MS)
        snapTo(0f)
    } else {
        animateTo(0f, tween(Motion.PRESS_FADE_MS))
    }
}

private const val DIM_AMOUNT = 0.5f
private const val SCALE_AMOUNT = 0.04f
private const val ACCENT_ALPHA = 0.35f

/**
 * Animation and hold durations in ms. [TRANSITION_MS] is the upper bound for any transition (ADR-0003);
 * [HOLD_MS] is the hold duration for deliberate actions.
 */
object Motion {
    const val TRANSITION_MS = 200
    const val PRESS_FADE_MS = 200
    const val PRESS_HOLD_MS = 100L
    const val HOLD_MS = 1000L
}

/** Full width with a fixed [height]. */
fun Modifier.rowHeight(height: Dp): Modifier = this.fillMaxWidth().height(height)
