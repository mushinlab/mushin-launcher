package com.mushinlab.launcher.feature.settings.tips

import android.animation.ValueAnimator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.rememberTextMeasurer
import com.mushinlab.launcher.core.ui.LocalMushinFont

@Composable
internal fun TipAnimation(tip: Tip, modifier: Modifier = Modifier) {
    when (tip) {
        Tip.SPACE -> SpaceTipAnimation(modifier)
        Tip.STYLE -> StyleTipAnimation(modifier)
        Tip.MEMORY -> MemoryTipAnimation(modifier)
    }
}

@Composable
internal fun rememberMiniText(): MiniText {
    val measurer = rememberTextMeasurer()
    val font = LocalMushinFont.current
    return remember(measurer, font) { MiniText(measurer, font) }
}

@Composable
internal fun TipCanvas(loopMs: Int, stillMs: Int, modifier: Modifier, draw: DrawScope.(time: Float) -> Unit) {
    val time = rememberTipTime(loopMs, stillMs)
    Canvas(modifier.clearAndSetSemantics { }) { draw(time.value) }
}

@Composable
private fun rememberTipTime(loopMs: Int, stillMs: Int): State<Float> {
    val still = remember { !ValueAnimator.areAnimatorsEnabled() }
    if (still) return remember(stillMs) { mutableFloatStateOf(stillMs.toFloat()) }
    return rememberInfiniteTransition(label = "tip").animateFloat(
        initialValue = 0f,
        targetValue = loopMs.toFloat(),
        animationSpec = infiniteRepeatable(tween((loopMs * TIP_SLOWDOWN).toInt(), easing = LinearEasing)),
        label = "tip_time",
    )
}

private const val TIP_SLOWDOWN = 1.5f
