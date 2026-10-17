package com.mushinlab.launcher.feature.settings

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.ClockFormat
import com.mushinlab.launcher.core.model.ClockPosition
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.SingleLineText
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max

@Composable
internal fun HomeMock(settings: LauncherSettings, overImage: Boolean) {
    Layout(
        content = {
            MockHeader(settings)
            MockList(settings, overImage)
        },
        modifier = Modifier.fillMaxSize().testTag("background_mock"),
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val header = measurables[0].measure(loose)
        val list = measurables[1].measure(loose)
        val centered = (constraints.maxHeight - list.height) / 2
        val top = centered.coerceAtLeast(header.height)
        layout(constraints.maxWidth, constraints.maxHeight) {
            header.place(0, 0)
            list.place(0, top)
        }
    }
}

@Composable
private fun MockHeader(settings: LauncherSettings) {
    val colors = LocalMushinColors.current
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val now = remember { LocalDateTime.now() }
    val use24h = when (settings.clockFormat) {
        ClockFormat.SYSTEM -> DateFormat.is24HourFormat(context)
        ClockFormat.H24 -> true
        ClockFormat.H12 -> false
    }
    val align = when (settings.clockPosition) {
        ClockPosition.START -> TextAlign.Start
        ClockPosition.CENTER -> TextAlign.Center
        ClockPosition.END -> TextAlign.End
    }
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TOP_GAP))
        val textModifier = Modifier.fillMaxWidth().padding(horizontal = ROW_PADDING)
        BasicText(
            DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h:mm").format(now),
            style = MushinType.style(
                MushinType.clockSize,
                colors.textPrimary,
                FontWeight.Light,
            ).copy(textAlign = align),
            modifier = textModifier.testTag("background_mock_clock"),
        )
        if (settings.showDate) {
            Spacer(Modifier.height(4.dp))
            val date = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale).format(now)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            BasicText(
                date,
                style = MushinType.style(MushinType.dateSize, colors.textSecondary).copy(textAlign = align),
                modifier = textModifier,
            )
        }
        Spacer(Modifier.height(LIST_GAP - HANDLE_HEIGHT))
    }
}

@Composable
private fun MockList(settings: LauncherSettings, overImage: Boolean) {
    val colors = LocalMushinColors.current
    val text = MushinType.style(settings.homeTextSize.sp.sp, colors.textPrimary)
    val rowHeight = mockRowHeight(text)
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(HANDLE_HEIGHT), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(HANDLE_WIDTH, HANDLE_THICKNESS)
                    .background(colors.textSecondary.copy(alpha = HANDLE_ALPHA), RoundedCornerShape(HANDLE_THICKNESS)),
            )
        }
        repeat(MOCK_ROWS) { index ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .background(colors.row(index, overImage))
                    .testTag("background_mock_row_$index")
                    .padding(horizontal = ROW_PADDING),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SingleLineText(stringResource(R.string.background_mock_app, index + 1), text)
            }
        }
    }
}

@Composable
private fun mockRowHeight(text: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(text, density) {
        val textHeight = with(density) { measurer.measure("Ag", text).size.height.toDp() }
        max(ROW_HEIGHT.value, (textHeight + ROW_TEXT_MARGIN).value).dp
    }
}

private const val MOCK_ROWS = 5
private const val HANDLE_ALPHA = 0.35f
private val TOP_GAP = 40.dp
private val LIST_GAP = 32.dp
private val ROW_HEIGHT = 64.dp
private val ROW_TEXT_MARGIN = 16.dp
private val ROW_PADDING = 24.dp
private val HANDLE_HEIGHT = 24.dp
private val HANDLE_WIDTH = 36.dp
private val HANDLE_THICKNESS = 4.dp
