package com.mushinlab.launcher.feature.search

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.search.SearchEngine
import org.junit.Test

class AppPickerTest {
    private val entries = listOf("Airbnb", "Binance", "Telegram").map {
        PickerEntry(AppKey(it.lowercase(), "Main", 0), it, checked = it == "Binance")
    }

    @Test
    fun blankQueryKeepsTheRestingOrder() {
        assertThat(pickerResults(entries, " ", SearchEngine())).isEqualTo(entries)
    }

    @Test
    fun queryKeepsOnlyMatchingAppsWithTheirMarks() {
        assertThat(pickerResults(entries, "bin", SearchEngine())).containsExactly(entries[1])
    }

    @Test
    fun queryTakesKeysUpToTheLimitWithoutDoubleSpaces() {
        val query = PickerQuery()
        assertThat(query.type(' ')).isFalse()
        "te le".forEach(query::type)
        assertThat(query.type(' ')).isTrue()
        assertThat(query.type(' ')).isFalse()
        assertThat(query.text).isEqualTo("te le ")
        assertThat(query.backspace()).isTrue()
        query.set("x".repeat(40))
        assertThat(query.text).hasLength(SearchController.MAX_QUERY)
        query.set("")
        assertThat(query.backspace()).isFalse()
    }
}
