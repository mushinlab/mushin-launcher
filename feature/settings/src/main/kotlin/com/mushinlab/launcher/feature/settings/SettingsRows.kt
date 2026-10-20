package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable
import kotlin.math.roundToInt

@Composable
fun NavRow(
    title: String,
    value: String? = null,
    summary: String? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    tag: String? = null,
    onClick: () -> Unit,
) {
    val colors = LocalMushinColors.current
    Row(
        Modifier
            .settingsCard()
            .heightIn(min = 56.dp)
            .selectedBackground(selected)
            .pressable(enabled = enabled, onClick = onClick)
            .let { if (tag != null) it.testTag(tag) else it }
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = CARD_PADDING, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f, fill = false)) {
            BasicText(title, style = MushinType.style(17.sp, colors.textPrimary))
            if (summary != null) {
                Spacer(Modifier.height(2.dp))
                BasicText(summary, style = MushinType.style(14.sp, colors.textSecondary))
            }
        }
        if (value != null) {
            Spacer(Modifier.width(16.dp))
            SingleLineText(value, MushinType.style(15.sp, colors.textSecondary))
        }
    }
}

@Composable
fun ToggleRow(title: String, checked: Boolean, tag: String? = null, onToggle: (Boolean) -> Unit) {
    val colors = LocalMushinColors.current
    Row(
        Modifier
            .settingsCard()
            .heightIn(min = 56.dp)
            .pressable(role = Role.Switch) { onToggle(!checked) }
            .let { if (tag != null) it.testTag(tag) else it }
            .padding(horizontal = CARD_PADDING, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(title, style = MushinType.style(17.sp, colors.textPrimary), modifier = Modifier.weight(1f))
        Switch(checked)
    }
}

@Composable
private fun Switch(checked: Boolean) {
    val colors = LocalMushinColors.current
    Box(
        Modifier
            .size(width = 40.dp, height = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) colors.accent else colors.keyBackground)
            .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (checked) colors.background else colors.textSecondary),
        )
    }
}

@Composable
fun SliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    tag: String,
    format: ((Float) -> String)? = null,
    onChange: (Float) -> Unit,
) {
    val colors = LocalMushinColors.current
    val locale = LocalConfiguration.current.locales[0]
    val slider = rememberSlider(value, range, step, onChange)
    Column(Modifier.settingsCard().padding(horizontal = CARD_PADDING, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(title, style = MushinType.style(17.sp, colors.textPrimary), modifier = Modifier.weight(1f))
            Spacer(Modifier.width(16.dp))
            SingleLineText(
                format?.invoke(slider.shown) ?: String.format(locale, "%.2f", slider.shown),
                MushinType.style(15.sp, colors.textSecondary),
            )
        }
        SliderTrack(slider, tag, Modifier.fillMaxWidth())
    }
}

/** A slider in one line: [title] in a column of [titleWidth], the track, then the [format]ted value. */
@Composable
internal fun InlineSliderRow(
    title: String,
    titleWidth: Dp,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    tag: String,
    format: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    val colors = LocalMushinColors.current
    val slider = rememberSlider(value, range, step, onChange)
    Row(
        Modifier.settingsCard().padding(horizontal = CARD_PADDING, vertical = INLINE_SLIDER_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SingleLineText(title, MushinType.style(15.sp, colors.textPrimary), Modifier.width(titleWidth))
        Spacer(Modifier.width(12.dp))
        SliderTrack(slider, tag, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        BasicText(
            format(slider.shown),
            style = MushinType.style(14.sp, colors.textSecondary).copy(textAlign = TextAlign.End),
            maxLines = 1,
            modifier = Modifier.width(INLINE_VALUE_WIDTH),
        )
    }
}

@Stable
private class SliderModel(
    initial: Float,
    val range: ClosedFloatingPointRange<Float>,
    private val step: Float,
    private val onChange: State<(Float) -> Unit>,
) {
    val steps = ((range.endInclusive - range.start) / step).roundToInt()
    var shown by mutableFloatStateOf(initial)
        private set
    private var external = initial

    fun sync(value: Float) {
        if (value != external) {
            external = value
            shown = value
        }
    }

    fun select(fraction: Float) {
        val next = range.start + (fraction.coerceIn(0f, 1f) * steps).roundToInt() * step
        if (next != shown) {
            shown = next
            onChange.value(next)
        }
    }

    fun fraction(): Float = (shown - range.start) / (range.endInclusive - range.start)
}

@Composable
private fun rememberSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    onChange: (Float) -> Unit,
): SliderModel {
    val currentOnChange = rememberUpdatedState(onChange)
    val slider = remember(range, step) { SliderModel(value, range, step, currentOnChange) }
    SideEffect { slider.sync(value) }
    return slider
}

@Composable
private fun SliderTrack(slider: SliderModel, tag: String, modifier: Modifier) {
    val colors = LocalMushinColors.current
    Box(
        modifier
            .height(SLIDER_HEIGHT)
            .testTag(tag)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(slider.shown, slider.range, slider.steps - 1)
                setProgress { target ->
                    slider.select((target - slider.range.start) / (slider.range.endInclusive - slider.range.start))
                    true
                }
            }
            .pointerInput(slider) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    slider.select(down.position.x / size.width)
                    horizontalDrag(down.id) { change ->
                        change.consume()
                        slider.select(change.position.x / size.width)
                    }
                }
            }
            .drawBehind {
                val fraction = slider.fraction()
                val track = SLIDER_TRACK.toPx()
                val y = size.height / 2
                drawRoundRect(
                    colors.textSecondary.copy(alpha = SLIDER_REST_ALPHA),
                    topLeft = Offset(0f, y - track / 2),
                    size = Size(size.width, track),
                    cornerRadius = CornerRadius(track / 2),
                )
                drawRoundRect(
                    colors.accent,
                    topLeft = Offset(0f, y - track / 2),
                    size = Size(size.width * fraction, track),
                    cornerRadius = CornerRadius(track / 2),
                )
                drawCircle(colors.accent, radius = SLIDER_THUMB.toPx(), center = Offset(size.width * fraction, y))
            },
    )
}

@Composable
fun <T> ChoiceRows(options: List<Pair<T, String>>, selected: T, tagPrefix: String, onSelect: (T) -> Unit) {
    options.forEach { (value, label) ->
        NavRow(
            title = label,
            selected = value == selected,
            tag = "${tagPrefix}_$value",
            onClick = { onSelect(value) },
        )
    }
}

private val SLIDER_HEIGHT = 40.dp
private val SLIDER_TRACK = 4.dp
private val INLINE_SLIDER_PADDING = 6.dp
private val INLINE_VALUE_WIDTH = 48.dp
private val SLIDER_THUMB = 10.dp
private const val SLIDER_REST_ALPHA = 0.35f
