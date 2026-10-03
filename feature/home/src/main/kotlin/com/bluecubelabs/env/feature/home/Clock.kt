package com.bluecubelabs.env.feature.home

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bluecubelabs.env.core.model.ClockFormat
import com.bluecubelabs.env.core.ui.EnvType
import com.bluecubelabs.env.core.ui.LocalEnvColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

@Composable
fun ClockBlock(minute: State<Long>, format: ClockFormat, showDate: Boolean, modifier: Modifier = Modifier) {
    Column(modifier) {
        ClockTime(minute, format)
        if (showDate) {
            Spacer(Modifier.height(4.dp))
            ClockDate(minute)
        }
    }
}

@Composable
private fun ClockTime(minute: State<Long>, format: ClockFormat) {
    val context = LocalContext.current
    val colors = LocalEnvColors.current
    val style = remember(colors) { EnvType.style(EnvType.clockSize, colors.textPrimary, FontWeight.Light) }
    val use24h = ClockText.is24h(format, DateFormat.is24HourFormat(context))
    BasicText(
        text = ClockText.time(minute.value, ZoneId.systemDefault(), use24h),
        style = style,
        modifier = Modifier.padding(horizontal = 24.dp).testTag("clock"),
    )
}

@Composable
private fun ClockDate(minute: State<Long>) {
    val colors = LocalEnvColors.current
    val locale = LocalConfiguration.current.locales[0]
    val style = remember(colors) { EnvType.style(EnvType.dateSize, colors.textSecondary) }
    BasicText(
        text = ClockText.date(minute.value, ZoneId.systemDefault(), locale),
        style = style,
        modifier = Modifier.padding(horizontal = 24.dp).testTag("date"),
    )
}
