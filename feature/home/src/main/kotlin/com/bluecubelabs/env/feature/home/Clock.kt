package com.bluecubelabs.env.feature.home

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bluecubelabs.env.core.model.ClockFormat
import com.bluecubelabs.env.core.model.ClockPosition
import com.bluecubelabs.env.core.ui.EnvType
import com.bluecubelabs.env.core.ui.LocalEnvColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object ClockText {
    fun time(epochMs: Long, zone: ZoneId, use24h: Boolean): String {
        val pattern = if (use24h) "HH:mm" else "h:mm"
        return DateTimeFormatter.ofPattern(pattern).format(Instant.ofEpochMilli(epochMs).atZone(zone))
    }

    fun date(epochMs: Long, zone: ZoneId, locale: Locale): String {
        val raw = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale).format(Instant.ofEpochMilli(epochMs).atZone(zone))
        return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    fun is24h(format: ClockFormat, systemIs24h: Boolean): Boolean = when (format) {
        ClockFormat.SYSTEM -> systemIs24h
        ClockFormat.H24 -> true
        ClockFormat.H12 -> false
    }
}

@Immutable
data class ClockDrag(val startX: Float, val x: Float)

object ClockSlots {
    fun left(position: ClockPosition, area: Int, block: Int, inset: Int): Int = when (position) {
        ClockPosition.START -> inset
        ClockPosition.CENTER -> (area - block) / 2
        ClockPosition.END -> area - block - inset
    }

    fun nearest(left: Int, area: Int, block: Int, inset: Int): ClockPosition =
        ClockPosition.entries.minBy { abs(left(it, area, block, inset) - left) }
}

@Composable
fun ClockArea(
    minute: State<Long>,
    format: ClockFormat,
    showDate: Boolean,
    position: ClockPosition,
    drag: ClockDrag?,
    onBounds: (Rect) -> Unit,
    onTap: () -> Unit,
    onMeasured: (area: Int, block: Int) -> Unit,
) {
    val colors = LocalEnvColors.current
    val density = LocalDensity.current
    val inset = with(density) { HomeDimens.rowPadding.roundToPx() }
    var area by remember { mutableIntStateOf(0) }
    var block by remember { mutableIntStateOf(0) }
    var height by remember { mutableIntStateOf(0) }
    val home = ClockSlots.left(position, area, block, inset)
    val left = if (drag ==
        null
    ) {
        home
    } else {
        (home + drag.x - drag.startX).roundToInt().coerceIn(0, maxOf(0, area - block))
    }
    Box(
        Modifier
            .fillMaxWidth()
            .onSizeChanged {
                area = it.width
                onMeasured(area, block)
            },
    ) {
        if (drag != null && block > 0) {
            val nearest = ClockSlots.nearest(left, area, block, inset)
            val markWidth = with(density) { SLOT_MARK_WIDTH.roundToPx() }
            Box(
                Modifier
                    .offset { IntOffset(ClockSlots.left(nearest, area, block, inset), 0) }
                    .size(with(density) { block.toDp() }, with(density) { height.toDp() })
                    .background(colors.accent.copy(alpha = SLOT_ALPHA), RoundedCornerShape(SLOT_RADIUS))
                    .border(1.dp, colors.accent.copy(alpha = SLOT_BORDER_ALPHA), RoundedCornerShape(SLOT_RADIUS)),
            )
            ClockPosition.entries.forEach { slot ->
                val slotLeft = ClockSlots.left(slot, area, block, inset)
                val x = when (slot) {
                    ClockPosition.START -> slotLeft
                    ClockPosition.CENTER -> slotLeft + (block - markWidth) / 2
                    ClockPosition.END -> slotLeft + block - markWidth
                }
                Box(
                    Modifier
                        .offset { IntOffset(x, height + SLOT_MARK_GAP.roundToPx()) }
                        .size(SLOT_MARK_WIDTH, SLOT_MARK_HEIGHT)
                        .background(
                            colors.accent.copy(alpha = if (slot == nearest) 1f else SLOT_MARK_ALPHA),
                            RoundedCornerShape(SLOT_MARK_HEIGHT),
                        )
                        .testTag("clock_slot_$slot"),
                )
            }
        }
        val align = when (position) {
            ClockPosition.START -> Alignment.Start
            ClockPosition.CENTER -> Alignment.CenterHorizontally
            ClockPosition.END -> Alignment.End
        }
        Column(
            Modifier
                .offset { IntOffset(left, 0) }
                .onSizeChanged {
                    block = it.width
                    height = it.height
                    onMeasured(area, block)
                }
                .onGloballyPositioned { onBounds(it.boundsInRoot()) }
                .semantics {
                    role = Role.Button
                    onClick {
                        onTap()
                        true
                    }
                }
                .testTag("clock_block"),
            horizontalAlignment = align,
        ) {
            ClockTime(minute, format, position)
            if (showDate) {
                Spacer(Modifier.height(4.dp))
                ClockDate(minute, position)
            }
        }
    }
}

private fun ClockPosition.textAlign(): TextAlign = when (this) {
    ClockPosition.START -> TextAlign.Start
    ClockPosition.CENTER -> TextAlign.Center
    ClockPosition.END -> TextAlign.End
}

@Composable
private fun ClockTime(minute: State<Long>, format: ClockFormat, position: ClockPosition) {
    val context = LocalContext.current
    val colors = LocalEnvColors.current
    val style = EnvType.style(EnvType.clockSize, colors.textPrimary, FontWeight.Light).copy(
        textAlign = position.textAlign(),
    )
    val use24h = ClockText.is24h(format, DateFormat.is24HourFormat(context))
    BasicText(
        text = ClockText.time(minute.value, ZoneId.systemDefault(), use24h),
        style = style,
        modifier = Modifier.testTag("clock"),
    )
}

@Composable
private fun ClockDate(minute: State<Long>, position: ClockPosition) {
    val colors = LocalEnvColors.current
    val locale = LocalConfiguration.current.locales[0]
    val style = EnvType.style(EnvType.dateSize, colors.textSecondary).copy(textAlign = position.textAlign())
    BasicText(
        text = ClockText.date(minute.value, ZoneId.systemDefault(), locale),
        style = style,
        modifier = Modifier.testTag("date"),
    )
}

private val SLOT_RADIUS = 12.dp
private val SLOT_MARK_WIDTH = 40.dp
private val SLOT_MARK_HEIGHT = 4.dp
private val SLOT_MARK_GAP = 10.dp
private const val SLOT_ALPHA = 0.08f
private const val SLOT_BORDER_ALPHA = 0.5f
private const val SLOT_MARK_ALPHA = 0.35f
