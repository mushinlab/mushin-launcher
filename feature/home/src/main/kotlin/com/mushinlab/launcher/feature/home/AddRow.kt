package com.mushinlab.launcher.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
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
            .testTag(HOME_ADD)
            .padding(horizontal = ADD_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AddButton(R.string.home_add_app, "home_add_app", onAddApp)
        AddButton(R.string.home_add_folder, "home_add_folder", onAddFolder)
    }
}

@Composable
private fun AddButton(@StringRes text: Int, tag: String, onClick: () -> Unit) {
    val colors = LocalMushinColors.current
    Row(
        Modifier
            .fillMaxHeight()
            .pressable(onClick = onClick)
            .testTag(tag)
            .padding(horizontal = ADD_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource(Glyph.ADD.res),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.textSecondary),
            modifier = Modifier.size(ADD_GLYPH),
        )
        Spacer(Modifier.width(6.dp))
        SingleLineText(stringResource(text), MushinType.style(MushinType.secondarySize, colors.textSecondary))
    }
}

@Composable
internal fun GlyphBadge(glyph: Glyph, size: Dp, modifier: Modifier = Modifier) {
    val colors = LocalMushinColors.current
    Box(
        modifier
            .size(size)
            .background(colors.keyBackground, CircleShape),
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
private val ADD_PADDING = 12.dp
private val ADD_GAP = 12.dp
private val ADD_GLYPH = 16.dp

private const val HOME_ADD = "home_add"
