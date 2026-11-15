package com.mushinlab.launcher.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.ImmutableList

@Composable
fun WidgetPickerSheet(widgets: ImmutableList<WidgetLabel>, onChoose: (Int) -> Unit, onClose: () -> Unit) {
    HomeSheet(stringResource(R.string.widgets_add), "widget_picker", onClose) {
        NoteText(stringResource(R.string.widgets_fit_panel))
        if (widgets.isEmpty()) NoteText(stringResource(R.string.widgets_empty))
        widgets.forEach { widget ->
            NavRow(widget.title(), summary = widget.app, tag = "widget_choice_${widget.id}") { onChoose(widget.id) }
        }
    }
}

@Composable
private fun WidgetLabel.title(): String = if (clock) stringResource(R.string.widgets_time) else label
