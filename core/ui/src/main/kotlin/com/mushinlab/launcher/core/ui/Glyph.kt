package com.mushinlab.launcher.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
    EDIT(R.drawable.ic_edit),
    HELP(R.drawable.ic_help),
    IMAGE(R.drawable.ic_image),
    PALETTE(R.drawable.ic_palette),
    SETTINGS(R.drawable.ic_settings),
    TEXT(R.drawable.ic_text),
}

/**
 * A square icon button tinted with [tint]; [description] is read by accessibility services.
 * A press shows the [LocalPressFeedback] style like any other button; a transparent [pressedColor] turns it off.
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
    onClick: () -> Unit,
) {
    val colors = LocalMushinColors.current
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
                .clip(CircleShape)
                .background(colors.surface)
                .border(GlyphWrapper.outline, PopupStyle.outline(colors), CircleShape)
                .pressable(pressedColor = pressedColor, onClick = onClick)
                .testTag("${tag}_wrapper"),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(glyph.res),
                contentDescription = null,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier.size(glyphSize),
            )
        }
    }
}

/** Circle every glyph button sits on, so an icon keeps its contrast over any palette or background image. */
object GlyphWrapper {
    val inset = 4.dp
    val outline = 2.dp
}
