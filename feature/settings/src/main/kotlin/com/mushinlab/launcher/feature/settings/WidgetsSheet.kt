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
import com.mushinlab.launcher.core.model.WidgetPanelPosition
import com.mushinlab.launcher.core.model.WidgetPanelSize
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class WidgetLabel(val id: Int, val label: String)

@Immutable
data class WidgetPanelUi(
    val position: WidgetPanelPosition,
    val size: WidgetPanelSize,
    val clock: Boolean,
    val widgets: ImmutableList<WidgetLabel>,
)

@Composable
fun WidgetsSheet(
    panels: ImmutableList<WidgetPanelUi>,
    onResize: (WidgetPanelPosition, WidgetPanelSize) -> Unit,
    onAdd: (WidgetPanelPosition) -> Unit,
    onRemove: (Int) -> Unit,
    onClock: () -> Unit,
    onClose: () -> Unit,
) {
    HomeSheet(stringResource(R.string.widgets_title), "widgets_sheet", onClose) {
        panels.forEach { panel ->
            SectionHeader(
                stringResource(
                    if (panel.position ==
                        WidgetPanelPosition.TOP
                    ) {
                        R.string.widgets_top
                    } else {
                        R.string.widgets_bottom
                    },
                ),
            )
            DropdownRow(
                stringResource(R.string.widgets_height),
                listOf(
                    WidgetPanelSize.SMALL to stringResource(R.string.home_text_s),
                    WidgetPanelSize.MEDIUM to stringResource(R.string.home_text_m),
                    WidgetPanelSize.LARGE to stringResource(R.string.home_text_l),
                ),
                panel.size,
                "widget_size_${panel.position}",
            ) { onResize(panel.position, it) }
            if (panel.clock) {
                NavRow(stringResource(R.string.widgets_time), tag = "widget_clock_settings", onClick = onClock)
                WidgetRemoveRow(WidgetLabel(-1, stringResource(R.string.widgets_time)), onRemove)
            }
            panel.widgets.forEach { WidgetRemoveRow(it, onRemove) }
            NavRow(stringResource(R.string.widgets_add), tag = "widgets_add_${panel.position}") {
                onAdd(panel.position)
            }
        }
        NoteText(stringResource(R.string.widgets_note))
    }
}

@Composable
private fun WidgetRemoveRow(widget: WidgetLabel, remove: (Int) -> Unit) {
    val colors = LocalMushinColors.current
    Row(Modifier.settingsCard().padding(start = CARD_PADDING), verticalAlignment = Alignment.CenterVertically) {
        BasicText(widget.label, style = MushinType.style(17.sp, colors.textPrimary), modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        GlyphButton(
            Glyph.DELETE,
            stringResource(R.string.widgets_remove, widget.label),
            colors.textPrimary,
            "widget_remove_${widget.id}",
        ) { remove(widget.id) }
    }
}
