package com.mushinlab.launcher.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import com.mushinlab.launcher.core.model.WidgetCell
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.feature.home.gesture.RowRegistry

@Composable
internal fun AddWidgetButton(panel: HomeWidgetPanel, registry: RowRegistry, onAdd: (WidgetCell) -> Unit) {
    val cell = panel.addCells.firstOrNull() ?: return
    DisposableEffect(Unit) { onDispose { registry.block(ADD_WIDGET, null) } }
    GlyphButton(
        Glyph.WIDGETS,
        stringResource(R.string.home_add_widget),
        LocalMushinColors.current.textPrimary,
        tag = ADD_WIDGET,
        modifier = Modifier.onGloballyPositioned { registry.block(ADD_WIDGET, it.boundsInRoot()) },
    ) { onAdd(cell) }
}

private const val ADD_WIDGET = "add_widget"
