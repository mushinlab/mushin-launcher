package com.mushinlab.launcher.feature.home

import android.text.format.DateFormat
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.ClockFormat
import com.mushinlab.launcher.core.model.ClockInk
import com.mushinlab.launcher.core.model.ClockPosition
import com.mushinlab.launcher.core.model.ClockSize
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.GlyphWrapper
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.Motion
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.PopupStyle
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressFeedback
import com.mushinlab.launcher.core.ui.pressable
import com.mushinlab.launcher.feature.home.gesture.RowRegistry
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

    fun shortDate(epochMs: Long, zone: ZoneId, locale: Locale): String {
        val raw = DateTimeFormatter.ofPattern("EEE, d MMM", locale).format(Instant.ofEpochMilli(epochMs).atZone(zone))
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
internal fun ClockTile(
    minute: State<Long>,
    format: ClockFormat,
    size: ClockSize,
    showDate: Boolean,
    ink: ClockInk,
    pressed: Boolean,
    onBounds: (Rect) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { onBounds(it.boundsInRoot()) }
            .clip(RoundedCornerShape(TILE_RADIUS))
            .pressFeedback(pressed)
            .padding(horizontal = TILE_PADDING)
            .testTag("clock_block"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ClockTime(minute, format, size, ink)
        if (showDate) ClockDate(minute, ink)
    }
}

@Composable
private fun ClockTime(minute: State<Long>, format: ClockFormat, size: ClockSize, ink: ClockInk) {
    val context = LocalContext.current
    val style = MushinType.style(size.tileSp.sp, LocalMushinColors.current.textPrimary, FontWeight.ExtraLight)
    val use24h = ClockText.is24h(format, DateFormat.is24HourFormat(context))
    ClockLabel(
        ClockText.time(minute.value, ZoneId.systemDefault(), use24h),
        style,
        "clock",
        ink,
    )
}

@Composable
private fun ClockDate(minute: State<Long>, ink: ClockInk) {
    val locale = LocalConfiguration.current.locales[0]
    ClockLabel(
        ClockText.shortDate(minute.value, ZoneId.systemDefault(), locale),
        MushinType.style(MushinType.secondarySize, LocalMushinColors.current.textSecondary),
        "date",
        ink,
    )
}

@Composable
private fun ClockLabel(text: String, style: TextStyle, tag: String, ink: ClockInk) {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val backdrop = LocalClockBackdrop.current
    val color = remember { Animatable(ink.fixed ?: backdrop.ink(Rect.Zero)) }
    var measured by remember { mutableStateOf<ClockScene?>(null) }
    LaunchedEffect(backdrop, bounds, ink) {
        val fixed = ink.fixed
        when {
            fixed != null -> color.animateTo(fixed, tween(Motion.TRANSITION_MS))

            !bounds.isEmpty && !backdrop.loading -> {
                val target = backdrop.ink(bounds)
                val sameScene = measured == backdrop.scene
                measured = backdrop.scene
                when {
                    sameScene -> color.animateTo(target, tween(Motion.TRANSITION_MS))
                    backdrop.fadeMs > 0 -> color.animateTo(target, tween(backdrop.fadeMs))
                    else -> color.snapTo(target)
                }
            }
        }
    }
    SingleLineText(
        text,
        style.copy(color = color.value),
        Modifier.onGloballyPositioned { bounds = it.boundsInRoot() }.testTag(tag),
    )
}

private val ClockInk.fixed: Color?
    get() = when (this) {
        ClockInk.LIGHT -> Color.White
        ClockInk.DARK -> Color.Black
        ClockInk.AUTO -> null
    }

private val ClockSize.tileSp: Int
    get() = when (this) {
        ClockSize.S -> TILE_SMALL_SP
        ClockSize.M -> TILE_MEDIUM_SP
        ClockSize.L -> TILE_LARGE_SP
    }

private val TILE_RADIUS = 12.dp
private val TILE_PADDING = 12.dp
private const val TILE_SMALL_SP = 40
private const val TILE_MEDIUM_SP = 48
private const val TILE_LARGE_SP = 56
