package com.mushinlab.launcher.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
