package com.mushinlab.launcher.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.mushinlab.launcher.core.model.Palettes
import org.junit.Test

class PopupStyleTest {
    private val palettes = Palettes.presets.mapValues { (_, palette) -> MushinColors.from(palette) }

    @Test
    fun outlineStandsOutFromThePanelOnEveryPalette() {
        palettes.forEach { (preset, colors) ->
            val outline = PopupStyle.outline(colors).compositeOver(colors.surface)
            assertWithMessage("$preset outline on surface").that(contrast(outline, colors.surface)).isAtLeast(1.9)
        }
    }

    @Test
    fun panelEdgeStandsOutFromTheDimmedScreenOnEveryPalette() {
        palettes.forEach { (preset, colors) ->
            val dimmed = PopupStyle.scrim(colors).compositeOver(colors.background)
            val outline = PopupStyle.outline(colors).compositeOver(colors.surface)
            val edge = maxOf(contrast(outline, dimmed), contrast(colors.surface, dimmed))
            assertWithMessage("$preset panel edge on scrim").that(edge).isAtLeast(1.9)
            assertWithMessage("$preset scrim darkens").that(dimmed.luminance()).isAtMost(colors.background.luminance())
        }
    }

    @Test
    fun scrimIsBlackAndLighterOnLightPalettes() {
        val dark = PopupStyle.scrim(MushinColors.from(Palettes.Navy))
        val light = PopupStyle.scrim(MushinColors.from(Palettes.Paper))
        assertThat(dark.copy(alpha = 1f)).isEqualTo(Color.Black)
        assertThat(dark.alpha).isWithin(1e-3f).of(PopupStyle.DARK_SCRIM_ALPHA)
        assertThat(light.alpha).isWithin(1e-3f).of(PopupStyle.LIGHT_SCRIM_ALPHA)
        assertThat(light.alpha).isLessThan(dark.alpha)
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }
}
