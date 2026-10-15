package com.mushinlab.launcher.feature.home

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.LocalMushinStroke
import com.mushinlab.launcher.core.ui.Motion
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
        for (i in 1 until points.size) {
            val age = (trace.now - points[i].timeMs).coerceAtLeast(0L)
            val alpha = (1f - age.toFloat() / Motion.TRANSITION_MS).coerceIn(0f, 1f) * TRACE_ALPHA
            drawLine(
                color.copy(alpha = alpha),
                points[i - 1].position,
                points[i].position,
                widthPx,
                StrokeCap.Round,
            )
        }
    }
}

@Composable
fun ListHandle(active: Boolean, onBounds: (Rect) -> Unit) {
    val colors = LocalMushinColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(HANDLE_HEIGHT)
            .onGloballyPositioned { onBounds(it.boundsInRoot()) }
            .testTag("list_handle"),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(HANDLE_WIDTH, HANDLE_THICKNESS)
                .background(
                    if (active) colors.accent else colors.textSecondary.copy(alpha = HANDLE_ALPHA),
                    RoundedCornerShape(HANDLE_THICKNESS),
                ),
        )
    }
}

private const val TRACE_ALPHA = 0.6f
val HANDLE_HEIGHT = 24.dp
private val HANDLE_WIDTH = 36.dp
private val HANDLE_THICKNESS = 4.dp
private const val HANDLE_ALPHA = 0.35f
