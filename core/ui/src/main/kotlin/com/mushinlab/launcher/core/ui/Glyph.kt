package com.mushinlab.launcher.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
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
    DRAG(R.drawable.ic_drag),
    EDIT(R.drawable.ic_edit),
    FOLDER(R.drawable.ic_folder),
    FOLDER_ADD(R.drawable.ic_folder_add),
    HELP(R.drawable.ic_help),
    HIDE(R.drawable.ic_visibility_off),
    IMAGE(R.drawable.ic_image),
    INFO(R.drawable.ic_info),
    MOTION(R.drawable.ic_motion),
    PALETTE(R.drawable.ic_palette),
    SETTINGS(R.drawable.ic_settings),
    SHOW(R.drawable.ic_visibility),
    TEXT(R.drawable.ic_text),
    WIDGETS(R.drawable.ic_widgets),
}

/**
 * True where icons lie over a background image: [GlyphImage] then draws a soft blurred halo behind each icon in a
 * colour that contrasts with its tint, so a bare icon stays readable over any picture (ADR-0074).
 */
val LocalGlyphHalo = staticCompositionLocalOf { false }

/**
 * A 48 dp icon button tinted with [tint], without a container or outline (ADR-0074); [description] is read by
 * accessibility services. Pressing shows a soft circle in [pressedColor] with a gradient wave from the touch.
 */
@Composable
fun GlyphButton(
    glyph: Glyph,
    description: String,
    tint: Color,
    tag: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    glyphSize: Dp = 24.dp,
    pressedColor: Color = LocalMushinColors.current.textPrimary.copy(alpha = PRESSED_ALPHA),
    onClick: () -> Unit,
) {
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
                .pressable(pressedColor = pressedColor, onClick = onClick)
                .testTag("${tag}_wrapper"),
            contentAlignment = Alignment.Center,
        ) {
            GlyphImage(glyph, tint.copy(alpha = 1f), glyphSize)
        }
    }
}

/** [glyph] at [size] in [tint], with the [LocalGlyphHalo] halo behind it over a background image. */
@Composable
fun GlyphImage(glyph: Glyph, tint: Color, size: Dp, modifier: Modifier = Modifier) {
    val painter = painterResource(glyph.res)
    Box(modifier, contentAlignment = Alignment.Center) {
        if (LocalGlyphHalo.current) {
            val halo = if (tint.luminance() > HALO_SWITCH) Color.Black else Color.White
            Image(
                painter,
                contentDescription = null,
                colorFilter = ColorFilter.tint(halo.copy(alpha = HALO_ALPHA)),
                modifier = Modifier.size(size).blur(HALO_BLUR, BlurredEdgeTreatment.Unbounded),
            )
        }
        Image(painter, contentDescription = null, colorFilter = ColorFilter.tint(tint), modifier = Modifier.size(size))
    }
}

/** Container insets and the outline of draggable handles over a custom background. */
object GlyphWrapper {
    val inset = 4.dp
    val outline = 2.dp
}

private const val PRESSED_ALPHA = 0.08f
private const val HALO_SWITCH = 0.5f
private const val HALO_ALPHA = 0.7f
private val HALO_BLUR = 3.dp
