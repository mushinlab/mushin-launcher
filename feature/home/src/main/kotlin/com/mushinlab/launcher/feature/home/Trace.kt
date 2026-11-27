package com.mushinlab.launcher.feature.home

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.LocalMushinStroke
import com.mushinlab.launcher.core.ui.Motion
import com.mushinlab.launcher.core.ui.drawBrushTrail
import kotlinx.coroutines.flow.first

@Immutable
data class TracePoint(val position: Offset, val timeMs: Long)

@Stable
class Trace {
    val points = mutableStateListOf<TracePoint>()
    var now by mutableLongStateOf(0L)

    fun add(position: Offset, timeMs: Long) {
        points += TracePoint(position, timeMs)
    }
}

@Composable
fun TraceLayer(trace: Trace, modifier: Modifier = Modifier) {
    val stroke = LocalMushinStroke.current
    val color = stroke.color(LocalMushinColors.current)
    val widthPx = with(LocalDensity.current) { stroke.width.toPx() }
    LaunchedEffect(trace) {
        while (true) {
            snapshotFlow { trace.points.isNotEmpty() }.first { it }
            withFrameMillis { }
            trace.now = SystemClock.uptimeMillis()
            trace.points.removeAll { trace.now - it.timeMs > Motion.TRANSITION_MS }
        }
    }
    Canvas(modifier.fillMaxSize().testTag("home_trace")) {
        val points = trace.points
        drawBrushTrail(points.map { it.position }, color.copy(alpha = TRACE_ALPHA), widthPx * BRUSH_WIDTH) { index ->
            val age = (trace.now - points[index].timeMs).coerceAtLeast(0L)
            val remaining = (1f - age.toFloat() / Motion.TRANSITION_MS).coerceIn(0f, 1f)
            remaining * remaining
        }
    }
}

private const val TRACE_ALPHA = 0.85f
private const val BRUSH_WIDTH = 2.5f
