package com.mushinlab.launcher.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphWrapper
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.PopupStyle
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable
import com.mushinlab.launcher.feature.home.gesture.RowRegistry

@Composable
internal fun AddRow(
    rowStyle: RowStyle,
    background: Color,
    registry: RowRegistry,
    onAddFolder: () -> Unit,
    onAddApp: () -> Unit,
) {
    DisposableEffect(Unit) { onDispose { registry.block(HOME_ADD, null) } }
    Row(
        Modifier
            .fillMaxWidth()
            .height(rowStyle.height)
            .background(background)
            .onGloballyPositioned { registry.block(HOME_ADD, it.boundsInRoot()) }
            .testTag(HOME_ADD),
    ) {
        AddButton(Glyph.FOLDER, R.string.home_add_folder, "home_add_folder", Modifier.weight(1f), onAddFolder)
        AddButton(Glyph.ADD, R.string.home_add_app, "home_add_app", Modifier.weight(1f), onAddApp)
    }
}

@Composable
private fun AddButton(glyph: Glyph, @StringRes text: Int, tag: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = LocalMushinColors.current
    Column(
        modifier
            .fillMaxHeight()
            .pressable(onClick = onClick)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GlyphBadge(glyph, HomeDimens.iconSize)
        Spacer(Modifier.height(4.dp))
        SingleLineText(stringResource(text), MushinType.style(13.sp, colors.textPrimary))
    }
}

@Composable
internal fun GlyphBadge(glyph: Glyph, size: Dp, modifier: Modifier = Modifier) {
    val colors = LocalMushinColors.current
    Box(
        modifier
            .size(size)
            .background(colors.surface, CircleShape)
            .border(GlyphWrapper.outline, PopupStyle.outline(colors), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(glyph.res),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.iconTint),
            modifier = Modifier.size(size * GLYPH_TO_BADGE),
        )
    }
}

private const val GLYPH_TO_BADGE = 0.6f

private const val HOME_ADD = "home_add"
