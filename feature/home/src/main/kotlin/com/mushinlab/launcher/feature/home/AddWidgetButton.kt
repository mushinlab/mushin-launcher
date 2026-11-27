package com.mushinlab.launcher.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.model.WidgetCell
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable
import com.mushinlab.launcher.feature.home.gesture.RowRegistry

@Composable
internal fun AddWidgetButton(panel: HomeWidgetPanel, registry: RowRegistry, onAdd: (WidgetCell) -> Unit) {
    val cell = panel.addCells.firstOrNull() ?: return
    val colors = LocalMushinColors.current
    DisposableEffect(Unit) { onDispose { registry.block(ADD_WIDGET, null) } }
    Row(
        Modifier
            .height(ADD_WIDGET_HEIGHT)
            .clip(CircleShape)
            .background(colors.keyBackground)
            .onGloballyPositioned { registry.block(ADD_WIDGET, it.boundsInRoot()) }
            .pressable(onClick = { onAdd(cell) })
            .testTag(ADD_WIDGET)
            .padding(horizontal = ADD_WIDGET_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource(Glyph.WIDGETS.res),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.iconTint),
            modifier = Modifier.size(ADD_WIDGET_GLYPH),
        )
        Spacer(Modifier.size(8.dp))
        SingleLineText(
            stringResource(R.string.home_add_widget),
            MushinType.style(MushinType.secondarySize, colors.textPrimary),
        )
    }
}

private val ADD_WIDGET_HEIGHT = 36.dp
private val ADD_WIDGET_PADDING = 14.dp
private val ADD_WIDGET_GLYPH = 20.dp
private const val ADD_WIDGET = "add_widget"
