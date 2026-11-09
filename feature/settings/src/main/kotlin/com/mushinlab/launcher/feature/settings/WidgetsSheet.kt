package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import kotlinx.collections.immutable.ImmutableList

@Composable
fun WidgetsSheet(
    widgets: ImmutableList<WidgetLabel>,
    canAdd: Boolean,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onClock: () -> Unit,
    onClose: () -> Unit,
) {
    HomeSheet(stringResource(R.string.widgets_title), "widgets_sheet", onClose) {
        widgets.forEach { WidgetRemoveRow(it, onRemove, onClock) }
        if (canAdd) NavRow(stringResource(R.string.widgets_add), tag = "widgets_add_TOP", onClick = onAdd)
        NoteText(stringResource(R.string.widgets_note))
    }
}

@Composable
fun WidgetPickerSheet(widgets: ImmutableList<WidgetLabel>, onChoose: (Int) -> Unit, onClose: () -> Unit) {
    HomeSheet(stringResource(R.string.widgets_add), "widget_picker", onClose) {
        NoteText(stringResource(R.string.widgets_small_only))
        if (widgets.isEmpty()) NoteText(stringResource(R.string.widgets_empty))
        widgets.forEach { widget ->
            NavRow(widget.title(), summary = widget.app, tag = "widget_choice_${widget.id}") { onChoose(widget.id) }
        }
    }
}

@Composable
private fun WidgetRemoveRow(widget: WidgetLabel, remove: (Int) -> Unit, onClock: () -> Unit) {
    val colors = LocalMushinColors.current
    val label = widget.title()
    Row(Modifier.settingsCard().padding(start = CARD_PADDING), verticalAlignment = Alignment.CenterVertically) {
        BasicText(label, style = MushinType.style(17.sp, colors.textPrimary), modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        if (widget.clock) {
            GlyphButton(
                Glyph.EDIT,
                stringResource(R.string.clock_sheet),
                colors.textPrimary,
                "widget_clock_settings",
                onClick = onClock,
            )
        }
        GlyphButton(
            Glyph.DELETE,
            stringResource(R.string.widgets_remove, label),
            colors.textPrimary,
            "widget_remove_${widget.id}",
        ) { remove(widget.id) }
    }
}

@Composable
private fun WidgetLabel.title(): String = if (clock) stringResource(R.string.widgets_time) else label
