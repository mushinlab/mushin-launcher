package com.mushinlab.launcher

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.model.SpaceBackground
import com.mushinlab.launcher.core.model.SpaceTransition
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.ui.BackgroundLook
import com.mushinlab.launcher.core.ui.Motion
import com.mushinlab.launcher.core.ui.MushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.SpaceBackdrop
import com.mushinlab.launcher.feature.home.HomeDimens
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class SpacePagerMotion(private val pager: SpacePager, private val scope: CoroutineScope) {
    var offset by mutableFloatStateOf(0f)
        private set
    var coverAlpha by mutableFloatStateOf(0f)
        private set
    var side by mutableStateOf<SpacePreview?>(null)
        private set
    var cover by mutableStateOf<SpacePreview?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var width = 0f

    fun drag(dx: Float) {
        if (busy || width <= 0f) return
        val neighbors = pager.neighbors.value
        val target = if (dx < 0f) neighbors.next else neighbors.previous
        side = target
        offset = if (target == null) 0f else (dx / width).coerceIn(-1f, 1f)
    }

    fun release(direction: SwipeDirection?) {
        if (busy) return
        val forward = direction == SwipeDirection.LEFT && offset < 0f
        val backward = direction == SwipeDirection.RIGHT && offset > 0f
        val target = side?.takeIf { forward || backward }
        busy = true
        scope.launch {
            try {
                if (target == null) {
                    slide(0f, Motion.TRANSITION_MS)
                    side = null
                } else {
                    slide(if (forward) -1f else 1f, (Motion.TRANSITION_MS * (1f - abs(offset))).toInt())
                    arrive(target)
                }
            } finally {
                busy = false
            }
        }
    }

    suspend fun play(request: SpaceSwitch) {
        busy = true
        try {
            when (request.transition) {
                SpaceTransition.SLIDE -> {
                    side = request.target
                    slide(if (request.direction == SwipeDirection.LEFT) -1f else 1f, Motion.TRANSITION_MS)
                }

                SpaceTransition.FADE -> {
                    cover = request.target
                    fade(1f, Motion.TRANSITION_MS)
                }

                SpaceTransition.NONE -> Unit
            }
            arrive(request.target)
        } finally {
            busy = false
            pager.done()
        }
    }

    private suspend fun arrive(target: SpacePreview) {
        cover = target
        coverAlpha = 1f
        side = null
        offset = 0f
        pager.commit(target.id)
        repeat(SETTLE_FRAMES) { withFrameNanos { } }
        fade(0f, REVEAL_MS)
        cover = null
    }

    private suspend fun slide(to: Float, durationMs: Int) {
        animate(offset, to, animationSpec = tween(durationMs)) { value, _ -> offset = value }
    }

    private suspend fun fade(to: Float, durationMs: Int) {
        animate(coverAlpha, to, animationSpec = tween(durationMs)) { value, _ -> coverAlpha = value }
    }

    private companion object {
        const val SETTLE_FRAMES = 2
        const val REVEAL_MS = Motion.TRANSITION_MS / 2
    }
}

@Composable
internal fun SpacePreviewPage(preview: SpacePreview, modifier: Modifier) {
    val colors = remember(preview.palette) { MushinColors.from(preview.palette) }
    val image = remember(preview.background) { preview.background?.asImageBitmap() }
    Box(modifier.fillMaxSize().background(colors.background).testTag("space_preview_${preview.id}")) {
        Box(Modifier.fillMaxSize().blur(BackgroundLook.BLUR_MAX, BlurredEdgeTreatment.Rectangle)) {
            if (image != null) SpaceBackdrop(image, preview.brightness, 0f, Modifier.fillMaxSize())
            PreviewRows(colors, preview.rows, if (image != null) SpaceBackground.ROW_ALPHA_DEFAULT else 1f)
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(HomeDimens.barHeight), contentAlignment = Alignment.CenterStart) {
                SingleLineText(
                    preview.name,
                    MushinType.style(MushinType.secondarySize, colors.textPrimary.copy(alpha = NAME_ALPHA)),
                    Modifier.padding(start = NAME_PADDING),
                )
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun PreviewRows(colors: MushinColors, rows: Int, alpha: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val height = PREVIEW_ROW.toPx()
        val top = size.height * ROWS_TOP
        repeat(rows) { index ->
            drawRect(
                colors.zebra(index),
                Offset(0f, top + height * index),
                Size(size.width, height),
                alpha = alpha,
            )
        }
    }
}

private val PREVIEW_ROW = 56.dp
private val NAME_PADDING = 24.dp
private const val ROWS_TOP = 0.32f
private const val NAME_ALPHA = 0.8f
