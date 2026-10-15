package com.mushinlab.launcher.feature.home

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.ClockFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Test

class ClockTextTest {
    private val zone = ZoneOffset.UTC
    private val evening = LocalDateTime.of(2026, 10, 3, 21, 5).toInstant(zone).toEpochMilli()

    @Test
    fun AC_03_1_formats24And12Hours() {
        assertThat(ClockText.time(evening, zone, use24h = true)).isEqualTo("21:05")
        assertThat(ClockText.time(evening, zone, use24h = false)).isEqualTo("9:05")
    }

    @Test
    fun AC_03_5_systemFormatFollowsSystemFlag() {
        assertThat(ClockText.is24h(ClockFormat.SYSTEM, systemIs24h = true)).isTrue()
        assertThat(ClockText.is24h(ClockFormat.SYSTEM, systemIs24h = false)).isFalse()
        assertThat(ClockText.is24h(ClockFormat.H24, systemIs24h = false)).isTrue()
        assertThat(ClockText.is24h(ClockFormat.H12, systemIs24h = true)).isFalse()
    }

    @Test
    fun dateIsLocalizedWithCapitalFirstLetter() {
        assertThat(ClockText.date(evening, zone, Locale.forLanguageTag("ru"))).isEqualTo("Суббота, 3 октября")
        assertThat(ClockText.date(evening, zone, Locale.ENGLISH)).isEqualTo("Saturday, 3 October")
    }
}
