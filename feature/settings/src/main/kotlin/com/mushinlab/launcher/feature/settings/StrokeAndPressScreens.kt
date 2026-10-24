package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.PressFeedback
import com.mushinlab.launcher.core.model.StrokeColor
import com.mushinlab.launcher.core.model.StrokeWidth
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinStroke
import kotlin.math.roundToInt

@Composable
internal fun StrokeRows(s: LauncherSettings, preferences: PreferenceActions) {
    SliderRow(
        stringResource(R.string.style_stroke_width),
        s.strokeWidth.toFloat(),
        StrokeWidth.MIN_DP.toFloat()..StrokeWidth.MAX_DP.toFloat(),
        1f,
        tag = "stroke_width",
        format = { "${it.roundToInt()} dp" },
    ) { v -> preferences.updateSettings { it.copy(strokeWidth = v.roundToInt()) } }
    StrokeColorDropdown(s.strokeColor) { color -> preferences.updateSettings { it.copy(strokeColor = color) } }
    ToggleRow(stringResource(R.string.style_stroke_dots), s.strokeDots, tag = "stroke_dots") { on ->
        preferences.updateSettings { it.copy(strokeDots = on) }
    }
}

@Composable
private fun StrokeColorDropdown(selected: StrokeColor, onSelect: (StrokeColor) -> Unit) {
    val colors = LocalMushinColors.current
    val names = mapOf(
        StrokeColor.ACCENT to stringResource(R.string.stroke_accent),
        StrokeColor.TEXT to stringResource(R.string.stroke_text),
        StrokeColor.CORAL to stringResource(R.string.stroke_coral),
        StrokeColor.ORANGE to stringResource(R.string.stroke_orange),
        StrokeColor.AMBER to stringResource(R.string.stroke_amber),
        StrokeColor.LIME to stringResource(R.string.stroke_lime),
        StrokeColor.GREEN to stringResource(R.string.stroke_green),
        StrokeColor.TEAL to stringResource(R.string.stroke_teal),
        StrokeColor.SKY to stringResource(R.string.stroke_sky),
        StrokeColor.BLUE to stringResource(R.string.stroke_blue),
        StrokeColor.VIOLET to stringResource(R.string.stroke_violet),
        StrokeColor.PINK to stringResource(R.string.stroke_pink),
    )
    DropdownRow(
        stringResource(R.string.style_stroke_color),
        StrokeColor.entries.map { it to names[it].orEmpty() },
        selected,
        "stroke_color",
        leading = { color ->
            Box(
                Modifier
                    .size(STROKE_SWATCH)
                    .clip(CircleShape)
                    .background(MushinStroke(StrokeWidth.DEFAULT_DP.dp, color).color(colors)),
            )
        },
        onSelect = onSelect,
    )
}

@Composable
internal fun pressLabel(feedback: PressFeedback): String = stringResource(
    when (feedback) {
        PressFeedback.FLASH -> R.string.press_flash
        PressFeedback.HOLD -> R.string.press_hold
        PressFeedback.ACCENT -> R.string.press_accent
        PressFeedback.DIM -> R.string.press_dim
        PressFeedback.SCALE -> R.string.press_scale
    },
)

private val STROKE_SWATCH = 18.dp
