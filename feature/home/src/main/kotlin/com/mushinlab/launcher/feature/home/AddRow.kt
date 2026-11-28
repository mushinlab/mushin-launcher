package com.mushinlab.launcher.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.GlyphImage
import com.mushinlab.launcher.core.ui.LocalMushinColors
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
        val tint = LocalMushinColors.current.textSecondary
        GlyphButton(Glyph.ADD, stringResource(R.string.home_add_app), tint, tag = "home_add_app", onClick = onAddApp)
        GlyphButton(
            Glyph.FOLDER_ADD,
            stringResource(R.string.home_add_folder),
            tint,
            tag = "home_add_folder",
            onClick = onAddFolder,
        )
    }
}

@Composable
internal fun GlyphBadge(glyph: Glyph, size: Dp, modifier: Modifier = Modifier) {
    GlyphImage(glyph, LocalMushinColors.current.iconTint, size * GLYPH_TO_BADGE, modifier.size(size))
}

private const val GLYPH_TO_BADGE = 0.6f
private val ADD_PADDING = 4.dp

private const val HOME_ADD = "home_add"
