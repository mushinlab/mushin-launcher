package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable

@Composable
internal fun ImageActions(replaceable: Boolean, onRemove: () -> Unit, onReplace: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = CARD_MARGIN, vertical = CARD_GAP / 2),
        horizontalArrangement = Arrangement.spacedBy(CARD_GAP * 2),
    ) {
        ActionTile(
            Glyph.DELETE,
            stringResource(R.string.space_background_remove),
            "background_remove",
            Modifier.weight(1f),
            onRemove,
        )
        if (replaceable) {
            ActionTile(
                Glyph.IMAGE,
                stringResource(R.string.background_replace),
                "background_replace",
                Modifier.weight(1f),
                onReplace,
            )
        }
    }
}

@Composable
private fun ActionTile(glyph: Glyph, label: String, tag: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = LocalMushinColors.current
    Column(
        modifier
            .clip(RoundedCornerShape(CARD_RADIUS))
            .background(colors.surface)
            .pressable(onClick = onClick)
            .testTag(tag)
            .padding(vertical = TILE_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painterResource(glyph.res),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.textPrimary),
            modifier = Modifier.size(ICON_SIZE),
        )
        Spacer(Modifier.height(4.dp))
        SingleLineText(label, MushinType.style(13.sp, colors.textSecondary))
    }
}

private val TILE_PADDING = 10.dp
private val ICON_SIZE = 22.dp
