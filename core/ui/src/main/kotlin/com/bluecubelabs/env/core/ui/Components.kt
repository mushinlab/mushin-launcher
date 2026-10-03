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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

@Composable
fun Modifier.pressable(
    enabled: Boolean = true,
    pressedColor: Color = LocalEnvColors.current.keyPressed,
    role: Role = Role.Button,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val flash = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pressed) {
        if (pressed) {
            flash.snapTo(1f)
        } else if (flash.value > 0f) {
            flash.animateTo(0f, tween(Motion.PRESS_FADE_MS))
        }
    }
    return this
        .drawBehind { if (enabled && flash.value > 0f) drawRect(pressedColor, alpha = flash.value) }
        .combinedClickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onLongClick = onLongClick,
            onClick = {
                scope.launch {
                    flash.snapTo(1f)
                    flash.animateTo(0f, tween(Motion.PRESS_FADE_MS))
                }
                onClick()
            },
        )
}

object Motion {
    const val TRANSITION_MS = 200
    const val PRESS_FADE_MS = 200
}

fun Modifier.rowHeight(height: Dp): Modifier = this.fillMaxWidth().height(height)
