package com.mushinlab.launcher.feature.search

import androidx.compose.ui.text.font.FontWeight
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MatchHighlightTest {
    @Test
    fun AC_67_2_firstCaseInsensitiveMatchIsMedium() {
        val text = matchHighlight("Voice Search", "CH")
        assertThat(text.text).isEqualTo("Voice Search")
        val span = text.spanStyles.single()
        assertThat(text.text.substring(span.start, span.end)).isEqualTo("ch")
        assertThat(span.item.fontWeight).isEqualTo(FontWeight.Medium)
    }

    @Test
    fun AC_67_2_noMatchOrEmptyQueryLeavesThePlainLabel() {
        assertThat(matchHighlight("Slack", "слак").spanStyles).isEmpty()
        assertThat(matchHighlight("Slack", " ").spanStyles).isEmpty()
    }
}
