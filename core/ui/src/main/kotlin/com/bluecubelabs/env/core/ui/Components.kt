package com.bluecubelabs.env.core.ui

import androidx.compose.animation.core.Animatable
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
import com.bluecubelabs.env.core.model.PressFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HorizontalDivider(modifier: Modifier = Modifier, color: Color = LocalEnvColors.current.divider) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun VerticalDivider(modifier: Modifier = Modifier, color: Color = LocalEnvColors.current.divider) {
    Box(modifier.width(1.dp).background(color))
}

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

val LocalPressFeedback = staticCompositionLocalOf { PressFeedback.FLASH }

@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    pressedColor: Color = LocalEnvColors.current.keyPressed,
    role: Role = Role.Button,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    val style = LocalPressFeedback.current
    val accent = LocalEnvColors.current.accent
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val level = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val visible = enabled && pressedColor.alpha > 0f
    LaunchedEffect(pressed) {
        if (pressed) {
            level.snapTo(1f)
        } else if (level.value > 0f) {
            level.release(style, quick = false)
        }
    }
    val feedback = when {
        !visible -> Modifier

        style == PressFeedback.DIM -> Modifier.graphicsLayer { alpha = 1f - DIM_AMOUNT * level.value }

        style == PressFeedback.SCALE -> Modifier.graphicsLayer {
            scaleX = 1f - SCALE_AMOUNT * level.value
            scaleY = scaleX
        }

        else -> {
            val color = if (style == PressFeedback.ACCENT) accent.copy(alpha = ACCENT_ALPHA) else pressedColor
            Modifier.drawBehind { if (level.value > 0f) drawRect(color, alpha = level.value) }
        }
    }
    return this
        .then(feedback)
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

object Motion {
    const val TRANSITION_MS = 200
    const val PRESS_FADE_MS = 200
    const val PRESS_HOLD_MS = 100L
}

fun Modifier.rowHeight(height: Dp): Modifier = this.fillMaxWidth().height(height)
