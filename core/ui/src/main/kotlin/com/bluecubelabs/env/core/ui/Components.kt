package com.bluecubelabs.env.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    return this
        .background(if (pressed && enabled) pressedColor else Color.Transparent)
        .clickable(interactionSource = source, indication = null, enabled = enabled, role = role, onClick = onClick)
}

fun Modifier.rowHeight(height: Dp): Modifier = this.fillMaxWidth().height(height)
