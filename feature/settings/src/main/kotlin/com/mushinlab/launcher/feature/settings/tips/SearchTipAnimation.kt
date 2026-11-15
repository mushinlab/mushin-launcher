package com.mushinlab.launcher.feature.settings.tips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.TextLayoutResult
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinColors
import com.mushinlab.launcher.feature.settings.R

@Composable
internal fun SearchTipAnimation(modifier: Modifier = Modifier) {
    val colors = LocalMushinColors.current
    val text = rememberMiniText()
    val apps = stringArrayResource(R.array.tip_apps).toList()
    val labels = remember(text, apps) { SearchLabels(text, apps) }
    TipCanvas(LOOP_MS, STILL_MS, modifier) { time -> drawSearchTip(time, colors, labels) }
}

@Immutable
private class SearchLabels(text: MiniText, apps: List<String>) {
    private val target = apps[TARGET]
    private val typed = (0..LETTERS).map { target.take(it).lowercase() }
    val home = apps.take(HOME_APPS).map { text.of(it, Scene.ROW_SIZE) }
    val queries = typed.map { text.of(it, Scene.LABEL_SIZE) }
    val results = typed.map { query ->
        val found = apps.filter { it != target && it.startsWith(query, ignoreCase = true) }
        (listOf(target) + found).take(RESULTS).map { text.of(it, Scene.ROW_SIZE) }
    }
    val title = text.of(target, Scene.TITLE_SIZE)
    val clock = text.of(CLOCK_TEXT, Scene.CLOCK_SIZE)
}

private fun DrawScope.drawSearchTip(time: Float, colors: MushinColors, labels: SearchLabels) {
    val open = ease(time, SWIPE_TO, SWIPE_TO + SLIDE_MS)
    val typed = Taps.count { time >= it + SWITCH_DELAY }
    val launch = ease(time, TAP_RESULT + SLIDE_DELAY, TAP_RESULT + SLIDE_DELAY + SLIDE_MS)
    scene(colors, loopFade(time, LOOP_MS)) {
        clockFace(colors, labels.clock, Scene.clock)
        zebraRows(colors, Scene.HOME_ROWS_TOP, labels.home)
        pencil(Scene.pencil, colors.textSecondary)
        if (open > 0f) {
            translate(top = Scene.HEIGHT * (1f - open)) {
                drawSearch(colors, labels.results[typed], labels.queries[typed])
            }
        }
        if (launch > 0f) {
            val bounds = lerp(BestRow, Rect(0f, 0f, Scene.WIDTH, Scene.HEIGHT), launch)
            drawRect(colors.accent, bounds.topLeft, bounds.size)
            val title = labels.title
            text(
                title,
                colors.background,
                bounds.center - Offset(title.size.width / 2f, title.size.height / 2f),
                launch,
            )
        }
        finger(
            colors,
            Finger.at(time),
            maxOf(pulse(time, Taps[0], Taps[1], TAP_RESULT), hold(time, SWIPE_FROM - PRESS_LEAD, SWIPE_TO)),
            phase(time, 0, FADE_MS) - phase(time, FINGER_OUT, FINGER_OUT + FADE_MS),
        )
    }
}

private fun DrawScope.drawSearch(colors: MushinColors, results: List<TextLayoutResult>, query: TextLayoutResult) {
    drawRect(colors.background, size = Size(Scene.WIDTH, Scene.HEIGHT))
    zebraRows(colors, KEYBOARD_TOP - Scene.ROW_HEIGHT * results.size, results.reversed())
    miniKeyboard(colors, KEYBOARD_TOP, query)
}

private val BestRow = Rect(0f, KEYBOARD_TOP - Scene.ROW_HEIGHT, Scene.WIDTH, KEYBOARD_TOP)
private val SwipeStart = Offset(Scene.WIDTH / 2, Scene.HEIGHT * 0.8f)
private val SwipeEnd = Offset(Scene.WIDTH / 2, Scene.HEIGHT * 0.45f)
private val FirstKey = MiniKeyboard.key(KEYBOARD_TOP, 2, 6)
private val SecondKey = MiniKeyboard.key(KEYBOARD_TOP, 1, 0)
private val Taps = listOf(1900, 2500)

private val Finger = Track(
    listOf(
        Key(0, Scene.rest),
        Key(300, Scene.rest),
        Key(SWIPE_FROM, SwipeStart),
        Key(SWIPE_TO, SwipeEnd),
        Key(1400, SwipeEnd),
        Key(1850, FirstKey),
        Key(2050, FirstKey),
        Key(2450, SecondKey),
        Key(2650, SecondKey),
        Key(3150, BestRow.center),
    ),
)

private const val CLOCK_TEXT = "12:45"
private const val LOOP_MS = 5400
private const val STILL_MS = 4200
private const val SLIDE_MS = 300
private const val SLIDE_DELAY = 100
private const val SWITCH_DELAY = 60
private const val PRESS_LEAD = 50
private const val FADE_MS = 300
private const val SWIPE_FROM = 700
private const val SWIPE_TO = 1050
private const val TAP_RESULT = 3250
private const val FINGER_OUT = 3400
private const val KEYBOARD_TOP = 236f
private const val HOME_APPS = 4
private const val TARGET = 3
private const val LETTERS = 2
private const val RESULTS = 6
