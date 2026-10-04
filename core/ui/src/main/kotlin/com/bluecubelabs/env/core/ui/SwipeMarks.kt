package com.bluecubelabs.env.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.bluecubelabs.env.core.model.SwipeDirection

@Composable
fun SwipeMarks(directions: List<SwipeDirection>, fontSize: TextUnit, color: Color, modifier: Modifier = Modifier) {
    if (directions.isEmpty()) return
    val size = with(LocalDensity.current) { fontSize.toDp() * GLYPH_TO_FONT }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        directions.forEach { direction ->
            Image(
                painterResource(direction.icon()),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color),
                modifier = Modifier.size(size),
            )
        }
    }
}

private fun SwipeDirection.icon(): Int = when (this) {
    SwipeDirection.UP -> R.drawable.ic_swipe_up
    SwipeDirection.DOWN -> R.drawable.ic_swipe_down
    SwipeDirection.LEFT -> R.drawable.ic_swipe_left
    SwipeDirection.RIGHT -> R.drawable.ic_swipe_right
}

private const val GLYPH_TO_FONT = 0.82f
