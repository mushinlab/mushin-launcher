package com.mushinlab.launcher.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    BACK(R.drawable.ic_arrow_back),
    EDIT(R.drawable.ic_edit),
    HELP(R.drawable.ic_help),
    SETTINGS(R.drawable.ic_settings),
}

/**
 * A square icon button tinted with [tint]; [description] is read by accessibility services.
 * Press feedback is off unless [pressedColor] is set (see [pressable]).
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
    pressedColor: Color = Color.Transparent,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(size)
            .pressable(pressedColor = pressedColor, onClick = onClick)
            .semantics { contentDescription = description }
            .testTag(tag),
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
