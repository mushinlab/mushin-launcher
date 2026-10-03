package com.bluecubelabs.env.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.bluecubelabs.env.core.model.Palette
import com.bluecubelabs.env.core.model.Palettes

@Immutable
data class EnvColors(
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

    companion object {
        fun from(p: Palette) = EnvColors(
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

val LocalEnvColors = staticCompositionLocalOf { EnvColors.from(Palettes.Graphite) }

@Composable
fun EnvTheme(palette: Palette, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalEnvColors provides EnvColors.from(palette), content = content)
}

object EnvType {
    fun style(size: TextUnit, color: Color, weight: FontWeight = FontWeight.Normal) = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = size,
        fontWeight = weight,
        color = color,
    )

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
