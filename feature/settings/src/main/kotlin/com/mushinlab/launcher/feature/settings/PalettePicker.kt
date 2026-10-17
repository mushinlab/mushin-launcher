package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.Palettes
import com.mushinlab.launcher.core.ui.GlyphWrapper
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.PopupStyle
import com.mushinlab.launcher.core.ui.pressable

@Composable
internal fun PalettePicker(selected: PalettePreset, onSelect: (PalettePreset) -> Unit) {
    val colors = LocalMushinColors.current
    val names = paletteNames()
    Column(Modifier.settingsCard().testTag("palette_picker").padding(vertical = 12.dp)) {
        BasicText(
            stringResource(R.string.space_palette) + names[selected]?.let { " · $it" }.orEmpty(),
            style = MushinType.style(17.sp, colors.textPrimary),
            modifier = Modifier.padding(horizontal = CARD_PADDING),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = CARD_PADDING - RING_GAP),
            horizontalArrangement = Arrangement.spacedBy(SWATCH_SPACING),
        ) {
            Palettes.presets.forEach { (preset, palette) ->
                PaletteSwatch(
                    swatch = MushinColors.from(palette),
                    selected = preset == selected,
                    name = names[preset].orEmpty(),
                    tag = "palette_$preset",
                ) { onSelect(preset) }
            }
        }
    }
}

@Composable
private fun PaletteSwatch(swatch: MushinColors, selected: Boolean, name: String, tag: String, onClick: () -> Unit) {
    val colors = LocalMushinColors.current
    Canvas(
        Modifier
            .size(SWATCH_SIZE)
            .clip(CircleShape)
            .pressable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = name
                this.selected = selected
            }
            .testTag(tag),
    ) {
        val inner = size.minDimension / 2 - RING_GAP.toPx()
        drawCircle(swatch.background, radius = inner)
        drawCircle(swatch.accent, radius = inner * DOT_FRACTION)
        drawCircle(PopupStyle.outline(colors), radius = inner, style = Stroke(GlyphWrapper.outline.toPx()))
        if (selected) {
            drawCircle(
                colors.accent,
                radius = size.minDimension / 2 - RING_STROKE.toPx() / 2,
                style = Stroke(RING_STROKE.toPx()),
            )
        }
    }
}

@Composable
private fun paletteNames(): Map<PalettePreset, String> = mapOf(
    PalettePreset.GRAPHITE to stringResource(R.string.palette_graphite),
    PalettePreset.NIGHT to stringResource(R.string.palette_night),
    PalettePreset.NAVY to stringResource(R.string.palette_navy),
    PalettePreset.MOSS to stringResource(R.string.palette_moss),
    PalettePreset.PAPER to stringResource(R.string.palette_paper),
    PalettePreset.SAND to stringResource(R.string.palette_sand),
    PalettePreset.LAGOON to stringResource(R.string.palette_lagoon),
    PalettePreset.PLUM to stringResource(R.string.palette_plum),
    PalettePreset.EMBER to stringResource(R.string.palette_ember),
    PalettePreset.MIST to stringResource(R.string.palette_mist),
    PalettePreset.ROSE to stringResource(R.string.palette_rose),
)

private val SWATCH_SIZE = 48.dp
private val SWATCH_SPACING = 4.dp
private val RING_GAP = 5.dp
private val RING_STROKE = 2.dp
private const val DOT_FRACTION = 0.38f
