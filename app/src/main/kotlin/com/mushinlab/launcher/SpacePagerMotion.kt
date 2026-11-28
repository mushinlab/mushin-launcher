package com.mushinlab.launcher

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import com.mushinlab.launcher.core.model.SpaceTransition
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.ui.LocalIconProvider
import com.mushinlab.launcher.core.ui.LocalMushinFont
import com.mushinlab.launcher.core.ui.LocalMushinStroke
import com.mushinlab.launcher.core.ui.Motion
import com.mushinlab.launcher.core.ui.MushinTheme
import com.mushinlab.launcher.feature.home.HomeActions
import com.mushinlab.launcher.feature.home.HomeScreen
import com.mushinlab.launcher.feature.home.WidgetContent
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class SpacePagerMotion(private val pager: SpacePager, private val scope: CoroutineScope) {
    var offset by mutableFloatStateOf(0f)
        private set
    var coverAlpha by mutableFloatStateOf(0f)
        private set
    var side by mutableStateOf<SpacePage?>(null)
        private set
    var cover by mutableStateOf<SpacePage?>(null)
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

    private suspend fun arrive(target: SpacePage) {
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
internal fun SpacePageView(
    page: SpacePage,
    icons: LauncherIconProvider,
    widgets: WidgetContent,
    minute: State<Long>,
    actions: HomeActions,
    modifier: Modifier,
) {
    MushinTheme(page.palette, LocalMushinFont.current, LocalMushinStroke.current) {
        CompositionLocalProvider(LocalIconProvider provides icons.withColors(page.coloredIcons)) {
            Box(modifier.fillMaxSize().clearAndSetSemantics { testTag = "space_preview_${page.id}" }) {
                HomeScreen(
                    state = page.state,
                    minute = minute,
                    actions = actions,
                    gesturesEnabled = { false },
                    widgets = widgets,
                )
            }
        }
    }
}
