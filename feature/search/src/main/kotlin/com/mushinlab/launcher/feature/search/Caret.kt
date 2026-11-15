package com.mushinlab.launcher.feature.search

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.Motion
import com.mushinlab.launcher.core.ui.MushinType

@Composable
internal fun Caret() {
    val visibility = rememberInfiniteTransition(label = "caret").animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = PERIOD_MS
                1f at SHOWN_MS
                0f at SHOWN_MS + Motion.TRANSITION_MS
                0f at PERIOD_MS - Motion.TRANSITION_MS
            },
        ),
        label = "caret_alpha",
    )
    BasicText(
        "▏",
        style = MushinType.style(SearchDimens.queryTextSize, LocalMushinColors.current.accent, FontWeight.Light),
        modifier = Modifier.graphicsLayer { alpha = visibility.value }.testTag("caret"),
    )
}

private const val PERIOD_MS = 1200
private const val SHOWN_MS = 500
