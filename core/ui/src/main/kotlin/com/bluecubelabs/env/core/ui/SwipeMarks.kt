package com.bluecubelabs.env.core.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.bluecubelabs.env.core.model.SwipeDirection

@Composable
fun SwipeMarks(directions: List<SwipeDirection>, style: TextStyle, modifier: Modifier = Modifier) {
    if (directions.isEmpty()) return
    BasicText(directions.joinToString(" ") { it.arrow() }, style = style, modifier = modifier)
}

private fun SwipeDirection.arrow(): String = when (this) {
    SwipeDirection.UP -> "↑"
    SwipeDirection.DOWN -> "↓"
    SwipeDirection.LEFT -> "←"
    SwipeDirection.RIGHT -> "→"
}
