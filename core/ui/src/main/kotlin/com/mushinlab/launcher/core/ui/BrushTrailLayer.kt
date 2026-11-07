package com.mushinlab.launcher.core.ui

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.collectLatest

/** Draws raw touch samples as a brush whose tail thins and disappears within one transition. */
@Composable
fun BrushTrailLayer(points: List<BrushPoint>, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(0L) }
    val stroke = LocalMushinStroke.current
    val color = stroke.color(LocalMushinColors.current)
    LaunchedEffect(points) {
        snapshotFlow { points.lastOrNull()?.timeMs }.collectLatest { last ->
            if (last != null) {
                do {
                    withFrameMillis { }
                    now = SystemClock.uptimeMillis()
                } while (now - last < Motion.TRANSITION_MS)
            }
        }
    }
    Canvas(modifier) {
        val visible = points.takeLastWhile { now - it.timeMs < Motion.TRANSITION_MS }
        drawBrushTrail(visible.map { it.position }, color, stroke.width.toPx() * BRUSH_WIDTH) { index ->
            val age = (now - visible[index].timeMs).coerceAtLeast(0L)
            val remaining = (1f - age.toFloat() / Motion.TRANSITION_MS).coerceIn(0f, 1f)
            remaining * remaining
        }
    }
}

private const val BRUSH_WIDTH = 2.5f
