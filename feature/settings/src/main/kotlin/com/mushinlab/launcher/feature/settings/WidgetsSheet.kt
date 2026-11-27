package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.PopupStyle
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable
import kotlinx.collections.immutable.ImmutableList

@Composable
fun WidgetPickerSheet(
    widgets: ImmutableList<WidgetLabel>,
    art: WidgetArt,
    onChoose: (Int) -> Unit,
    onClose: () -> Unit,
) {
    HomeSheet(stringResource(R.string.widgets_add), "widget_picker", onClose) {
        NoteText(stringResource(R.string.widgets_fit_panel))
        if (widgets.isEmpty()) NoteText(stringResource(R.string.widgets_empty))
        widgetGroups(widgets).forEachIndexed { index, group ->
            WidgetGroup(group, art, onChoose, Modifier.testTag("widget_group_$index"))
        }
    }
}

internal fun widgetGroups(widgets: List<WidgetLabel>): List<List<WidgetLabel>> = widgets
    .groupBy { it.app.orEmpty() }
    .values
    .sortedWith(
        compareBy<List<WidgetLabel>>({ group ->
            group.none { it.clock }
        }, { it.first().app.orEmpty().lowercase() }),
    )

@Composable
private fun WidgetGroup(
    group: List<WidgetLabel>,
    art: WidgetArt,
    onChoose: (Int) -> Unit,
    modifier: Modifier,
) {
    val colors = LocalMushinColors.current
    val first = group.first()
    val icon by produceState<ImageBitmap?>(null, first.id) { value = art.appIcon(first.id) }
    Column(modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            Modifier.padding(horizontal = CARD_MARGIN + CARD_PADDING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(APP_ICON)) {
                icon?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
            }
            Spacer(Modifier.width(12.dp))
            SingleLineText(
                first.app.orEmpty(),
                MushinType.style(MushinType.secondarySize, colors.textPrimary, FontWeight.Medium),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = CARD_MARGIN + CARD_PADDING),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            group.forEach { WidgetCard(it, art, onChoose) }
        }
    }
}

@Composable
private fun WidgetCard(widget: WidgetLabel, art: WidgetArt, onChoose: (Int) -> Unit) {
    val colors = LocalMushinColors.current
    val title = widget.title()
    val preview by produceState<ImageBitmap?>(null, widget.id) {
        value = if (widget.clock) null else art.preview(widget.id)
    }
    Column(
        Modifier.width(CARD_WIDTH).clip(CARD_SHAPE)
            .pressable { onChoose(widget.id) }
            .semantics { contentDescription = title }
            .testTag("widget_choice_${widget.id}")
            .padding(bottom = 6.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().height(PREVIEW_HEIGHT)
                .background(colors.surface, CARD_SHAPE)
                .border(PopupStyle.outlineWidth, PopupStyle.outline(colors), CARD_SHAPE)
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            val image = preview
            when {
                widget.clock -> ClockSample()
                image != null -> Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }
        Spacer(Modifier.height(6.dp))
        BasicText(
            title,
            style = MushinType.style(CARD_TEXT, colors.textPrimary),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun ClockSample() {
    val colors = LocalMushinColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SingleLineText(SAMPLE_TIME, MushinType.style(CLOCK_SAMPLE, colors.textPrimary, FontWeight.Light))
        Box(Modifier.width(48.dp).height(4.dp).background(colors.textSecondary, RoundedCornerShape(2.dp)))
    }
}

@Composable
private fun WidgetLabel.title(): String = if (clock) stringResource(R.string.widgets_time) else label

private val CARD_WIDTH = 148.dp
private val PREVIEW_HEIGHT = 104.dp
private val APP_ICON = 28.dp
private val CARD_SHAPE = RoundedCornerShape(12.dp)
private val CLOCK_SAMPLE = 32.sp
private val CARD_TEXT = 14.sp
private const val SAMPLE_TIME = "12:45"
