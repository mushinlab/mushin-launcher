package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.WidgetArrangement
import com.mushinlab.launcher.core.model.WidgetPanelMode
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class WidgetLabel(val id: Int, val label: String, val app: String? = null, val clock: Boolean = false)

@Immutable
data class WidgetPanelUi(
    val mode: WidgetPanelMode,
    val arrangement: WidgetArrangement,
    val widgets: ImmutableList<WidgetLabel>,
)

@Composable
fun WidgetsSheet(
    panel: WidgetPanelUi,
    onResize: (WidgetPanelMode, WidgetArrangement) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onClock: () -> Unit,
    onClose: () -> Unit,
) {
    HomeSheet(stringResource(R.string.widgets_title), "widgets_sheet", onClose) {
        val modes = listOfNotNull(
            if (panel.widgets.size <= WidgetPanelMode.STANDARD.capacity) {
                WidgetPanelMode.STANDARD to stringResource(R.string.widgets_standard)
            } else {
                null
            },
            WidgetPanelMode.EXPANDED to stringResource(R.string.widgets_expanded),
        )
        DropdownRow(stringResource(R.string.widgets_mode), modes, panel.mode, "widget_mode") {
            onResize(it, panel.arrangement)
        }
        if (panel.mode == WidgetPanelMode.STANDARD) {
            DropdownRow(
                stringResource(R.string.widgets_arrangement),
                listOf(
                    WidgetArrangement.ROW to stringResource(R.string.widgets_row),
                    WidgetArrangement.COLUMN to stringResource(R.string.widgets_column),
                ),
                panel.arrangement,
                "widget_arrangement",
            ) { onResize(panel.mode, it) }
        }
        if (panel.widgets.size > WidgetPanelMode.STANDARD.capacity) NoteText(stringResource(R.string.widgets_reduce))
        if (panel.widgets.size > panel.mode.capacity) NoteText(stringResource(R.string.widgets_saved_hidden))
        panel.widgets.forEach { WidgetRemoveRow(it, onRemove, onClock) }
        if (panel.widgets.size < panel.mode.capacity) {
            NavRow(stringResource(R.string.widgets_add), tag = "widgets_add_TOP", onClick = onAdd)
        }
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
