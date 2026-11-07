package com.mushinlab.launcher.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Vector icons used for toolbar buttons; [res] is the drawable resource. */
enum class Glyph(@param:DrawableRes val res: Int) {
    ADD(R.drawable.ic_add),
    BACK(R.drawable.ic_arrow_back),
    CHECK(R.drawable.ic_check),
    CLOSE(R.drawable.ic_close),
    DELETE(R.drawable.ic_delete),
    EDIT(R.drawable.ic_edit),
    FOLDER(R.drawable.ic_folder),
    HELP(R.drawable.ic_help),
    IMAGE(R.drawable.ic_image),
    PALETTE(R.drawable.ic_palette),
    SETTINGS(R.drawable.ic_settings),
    TEXT(R.drawable.ic_text),
    WIDGETS(R.drawable.ic_widgets),
}

/** Container variants for icon actions, using only the current fixed palette. */
enum class GlyphStyle { TONAL, FILLED, OUTLINED }

/**
 * A square icon button tinted with [tint]; [description] is read by accessibility services.
 * A gradient wave starts at the touch position; the opaque container keeps contrast over wallpaper.
 */
@Composable
fun GlyphButton(
    glyph: Glyph,
    description: String,
    tint: Color,
    tag: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    glyphSize: Dp = 22.dp,
    pressedColor: Color = LocalMushinColors.current.keyPressed,
    style: GlyphStyle = when (glyph) {
        Glyph.DELETE, Glyph.CLOSE -> GlyphStyle.OUTLINED
        else -> GlyphStyle.TONAL
    },
    onClick: () -> Unit,
) {
    val colors = LocalMushinColors.current
    val shape = if (style == GlyphStyle.FILLED) RoundedCornerShape(size * FILLED_RADIUS) else CircleShape
    val background = when (style) {
        GlyphStyle.FILLED -> colors.accent
        GlyphStyle.TONAL -> colors.keyBackground
        GlyphStyle.OUTLINED -> colors.surface
    }
    val foreground = if (style == GlyphStyle.FILLED) colors.background else tint.copy(alpha = 1f)
    val outline = if (style == GlyphStyle.OUTLINED) {
        Modifier.border(1.dp, colors.textSecondary, shape)
    } else {
        Modifier
    }
    Box(
        modifier
            .size(size)
            .semantics { contentDescription = description }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size - GlyphWrapper.inset * 2)
                .clip(shape)
                .background(background)
                .then(outline)
                .pressable(
                    pressedColor = if (style == GlyphStyle.FILLED) foreground else pressedColor,
                    onClick = onClick,
                )
                .testTag("${tag}_wrapper"),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(glyph.res),
                contentDescription = null,
                colorFilter = ColorFilter.tint(foreground),
                modifier = Modifier.size(glyphSize),
            )
        }
    }
}

/** Container insets and the outline of draggable handles over a custom background. */
object GlyphWrapper {
    val inset = 4.dp
    val outline = 2.dp
}

private const val FILLED_RADIUS = 0.28f
