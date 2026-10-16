package com.mushinlab.launcher.feature.settings.tips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinColors
import com.mushinlab.launcher.feature.settings.R

@Composable
internal fun TipsHintAnimation(modifier: Modifier = Modifier) {
    val colors = LocalMushinColors.current
    val text = rememberMiniText()
    val strings = listOf(
        stringResource(R.string.settings_title),
        stringResource(R.string.settings_style),
        stringResource(R.string.settings_home),
        stringResource(R.string.settings_keyboard),
        stringResource(R.string.settings_gestures),
        stringResource(R.string.settings_spaces),
        stringResource(R.string.settings_apps),
        stringResource(R.string.settings_system),
        stringResource(R.string.settings_tips),
    )
    val labels = remember(text, strings) {
        listOf(text.of(strings.first(), Scene.TITLE_SIZE)) + strings.drop(1).map { text.of(it, Scene.LABEL_SIZE) }
    }
    TipCanvas(LOOP_MS, STILL_MS, modifier) { time -> drawTipsHint(time, colors, labels) }
}

private fun DrawScope.drawTipsHint(time: Float, colors: MushinColors, labels: List<TextLayoutResult>) {
    val rows = labels.drop(1)
    val tips = rows.lastIndex
    val highlight = ease(time, TAP_TIPS, TAP_TIPS + HIGHLIGHT_MS)
    scene(colors, loopFade(time, LOOP_MS)) {
        page(colors, 0f, labels.first()) {
            rows.forEachIndexed { index, label ->
                card(index, colors.surface, label, if (index == tips) colors.accent else colors.textPrimary)
            }
            val top = Offset(Scene.CARD_INSET, Scene.CARD_TOP + Scene.CARD_STEP * tips)
            val size = Size(Scene.WIDTH - Scene.CARD_INSET * 2, Scene.CARD_HEIGHT)
            drawRoundRect(colors.accent, top, size, CornerRadius(CORNER), alpha = HIGHLIGHT_ALPHA * highlight)
            drawRoundRect(colors.accent, top, size, CornerRadius(CORNER), alpha = highlight, style = Stroke(BORDER))
        }
        finger(
            colors,
            Finger.at(time),
            pulse(time, TAP_TIPS),
            phase(time, 0, FADE_MS) - phase(time, FINGER_OUT, FINGER_OUT + FADE_MS),
        )
    }
}

private val Finger = Track(
    listOf(
        Key(0, Scene.rest),
        Key(500, Scene.rest),
        Key(1200, Scene.cardCenter(7)),
    ),
)

private const val LOOP_MS = 4000
private const val STILL_MS = 2600
private const val TAP_TIPS = 1250
private const val HIGHLIGHT_MS = 250
private const val FINGER_OUT = 2000
private const val FADE_MS = 300
private const val CORNER = 4f
private const val BORDER = 1.5f
private const val HIGHLIGHT_ALPHA = 0.2f
