package com.mushinlab.launcher.feature.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import com.mushinlab.launcher.core.ui.SpaceBackdrop

internal const val BACKGROUND_FADE_MS = 2_000

@Composable
internal fun BackgroundImage(background: ImageBitmap?, state: HomeUiState, modifier: Modifier) {
    val firstSpace = remember { state.spaceId }
    key(state.spaceId) {
        val entering = state.spaceId != firstSpace && background != null
        val alpha = remember { Animatable(if (entering) 0f else 1f) }
        LaunchedEffect(Unit) { alpha.animateTo(1f, tween(BACKGROUND_FADE_MS, easing = LinearOutSlowInEasing)) }
        Crossfade(
            background,
            modifier.graphicsLayer { this.alpha = alpha.value },
            animationSpec = tween(BACKGROUND_FADE_MS, easing = LinearOutSlowInEasing),
            label = "background",
        ) { image ->
            if (image != null) {
                SpaceBackdrop(
                    image,
                    state.backgroundBrightness,
                    state.backgroundBlur,
                    Modifier.fillMaxSize().testTag("home_background"),
                )
            }
        }
    }
}
