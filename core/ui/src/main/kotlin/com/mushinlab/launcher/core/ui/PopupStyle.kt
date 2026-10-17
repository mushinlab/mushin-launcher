package com.mushinlab.launcher.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * How a popup stands out from the screen under it (ADR-0019): a shadow of [elevation], a 1 dp [outline],
 * a black [scrim] over everything behind it and, on home and search, the content under it blurred by [blurRadius].
 */
object PopupStyle {
    val elevation = 12.dp
    val outlineWidth = 1.dp
    val blurRadius = 10.dp
    const val OUTLINE_ALPHA = 0.5f
    const val DARK_SCRIM_ALPHA = 0.4f
    const val LIGHT_SCRIM_ALPHA = 0.25f

    /** Outline of a popup: [MushinColors.textSecondary] at [OUTLINE_ALPHA]. */
    fun outline(colors: MushinColors): Color = colors.textSecondary.copy(alpha = OUTLINE_ALPHA)

    /** Dim over the screen behind a popup: black at [DARK_SCRIM_ALPHA], or [LIGHT_SCRIM_ALPHA] on light palettes. */
    fun scrim(colors: MushinColors): Color = Color.Black.copy(alpha = scrimAlpha(colors.isDark))

    /** Opacity of [scrim] for a dark or light palette. */
    fun scrimAlpha(isDark: Boolean): Float = if (isDark) DARK_SCRIM_ALPHA else LIGHT_SCRIM_ALPHA
}

/** Popup background: [PopupStyle] shadow, [MushinColors.surface] fill and outline in [shape], content clipped to it. */
fun Modifier.popupSurface(colors: MushinColors, shape: Shape): Modifier = this
    .shadow(PopupStyle.elevation, shape, clip = true, ambientColor = Color.Black, spotColor = Color.Black)
    .background(colors.surface)
    .border(PopupStyle.outlineWidth, PopupStyle.outline(colors), shape)

/** Blurs this content by [PopupStyle.blurRadius] while a popup is [open] over it; the popup itself stays sharp. */
fun Modifier.popupBackdrop(open: Boolean): Modifier = if (open) blur(PopupStyle.blurRadius) else this
