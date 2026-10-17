package com.mushinlab.launcher.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.AppFont
import com.mushinlab.launcher.core.model.AppFontWeight
import com.mushinlab.launcher.core.model.Palette
import com.mushinlab.launcher.core.model.Palettes

/**
 * Compose colors of a [Palette]; [zebra] returns the alternating background of the list row at an index and
 * [row] the same color as drawn on the home screen, slightly see-through over a background image.
 */
@Immutable
data class MushinColors(
    val background: Color,
    val zebraA: Color,
    val zebraB: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val iconTint: Color,
    val divider: Color,
    val surface: Color,
    val keyBackground: Color,
    val keyPressed: Color,
    val isDark: Boolean,
) {
    fun zebra(index: Int): Color = if (index % 2 == 0) zebraA else zebraB

    /** Background of the home row at [index]: [zebra], at [BackgroundLook.ROW_ALPHA] over an image. */
    fun row(index: Int, overImage: Boolean): Color =
        if (overImage) zebra(index).copy(alpha = BackgroundLook.ROW_ALPHA) else zebra(index)

    companion object {
        fun from(p: Palette) = MushinColors(
            background = Color(p.background),
            zebraA = Color(p.zebraA),
            zebraB = Color(p.zebraB),
            textPrimary = Color(p.textPrimary),
            textSecondary = Color(p.textSecondary),
            accent = Color(p.accent),
            iconTint = Color(p.iconTint),
            divider = Color(p.divider),
            surface = Color(p.surface),
            keyBackground = Color(p.keyBackground),
            keyPressed = Color(p.keyPressed),
            isDark = p.isDark,
        )
    }
}

/** Current palette colors; Graphite outside [MushinTheme]. */
val LocalMushinColors = staticCompositionLocalOf { MushinColors.from(Palettes.Graphite) }

/** Interface font family and default weight. */
@Immutable
data class MushinFont(val family: FontFamily, val weight: FontWeight) {
    companion object {
        val Default = MushinFont(FontFamily.Default, FontWeight.Normal)

        fun of(font: AppFont, weight: AppFontWeight) = MushinFont(MushinFonts.family(font), FontWeight(weight.value))
    }
}

/** Current interface font. */
val LocalMushinFont = staticCompositionLocalOf { MushinFont.Default }

/** Provides palette colors, font and gesture trail style to [content]. There is no Material theme. */
@Composable
fun MushinTheme(
    palette: Palette,
    font: MushinFont = MushinFont.Default,
    stroke: MushinStroke = MushinStroke.Default,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalMushinColors provides MushinColors.from(palette),
        LocalMushinFont provides font,
        LocalMushinStroke provides stroke,
        content = content,
    )
}

/** Text sizes of the interface and [style], which applies the current font. */
object MushinType {
    /** Text style in the current font; [FontWeight.Normal] means the user's chosen weight. */
    @Composable
    @ReadOnlyComposable
    fun style(size: TextUnit, color: Color, weight: FontWeight = FontWeight.Normal): TextStyle {
        val font = LocalMushinFont.current
        return TextStyle(
            fontFamily = font.family,
            fontSize = size,
            fontWeight = if (weight == FontWeight.Normal) font.weight else weight,
            color = color,
        )
    }

    val clockSize = 72.sp
    val dateSize = 18.sp
    val resultSize = 22.sp
    val recentSize = 16.sp
    val querySize = 24.sp
    val keySize = 22.sp
    val digitHintSize = 10.sp
    val badgeSize = 13.sp
    val popupSize = 16.sp
    val popupHeaderSize = 14.sp
    val secondarySize = 16.sp
}
