package com.bluecubelabs.env.core.search

import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.AppUsage
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SearchEngineTest {
    private val engine = SearchEngine()
    private val now = 10_000_000_000L

    private fun app(label: String, custom: String? = null, hidden: Boolean = false) =
        SearchItem(AppKey("pkg.${label.lowercase()}", "Main", 0), label, custom, hidden)

    private val slack = app("Slack")
    private val slides = app("Slides")
    private val signal = app("Signal")
    private val telegram = app("Telegram")
    private val googleMaps = app("Google Maps")
    private val gmail = app("Gmail")
    private val sber = app("СберБанк")
    private val camera = app("Камера")
    private val settings = app("Settings")
    private val spotify = app("Spotify")
    private val whatsApp = app("WhatsApp")
    private val youTube = app("YouTube")

    private val ten = listOf(slack, telegram, gmail, googleMaps, sber, camera, settings, spotify, whatsApp, youTube)

    private fun labels(items: List<SearchItem>, query: String, usage: Map<AppKey, AppUsage> = emptyMap()) =
        engine.search(SearchIndex.build(items), query, usage, now).map { it.label }

    @Test
    fun sl_slackOrSlidesInTop2_andSlackWithUsageIsFirst() {
        val plain = labels(listOf(slack, slides, signal), "sl")
        assertThat(plain.take(2)).containsExactly("Slack", "Slides")
        val usage = mapOf(slack.key to AppUsage(slack.key, 3, now - 1000, 3.0))
        assertThat(labels(listOf(slides, signal, slack), "sl", usage).first()).isEqualTo("Slack")
    }

    @Test
    fun wrongLayoutRu_isFirst() {
        assertThat(labels(listOf(slack), "ыдфсл").first()).isEqualTo("Slack")
        assertThat(labels(ten + slides + signal, "ыдфсл").first()).isEqualTo("Slack")
    }

    @Test
    fun wrongLayoutEnForCyrillicSpelling_isTop3() {
        assertThat(labels(listOf(slack), "ckfr").take(3)).contains("Slack")
        assertThat(labels(ten + slides + signal, "ckfr").take(3)).contains("Slack")
    }

    @Test
    fun cyrillicTransliteration_isTop3() {
        assertThat(labels(listOf(slack), "слак").take(3)).contains("Slack")
        assertThat(labels(ten + slides + signal, "слак").take(3)).contains("Slack")
    }

    @Test
    fun cyrillicTelegram_isFirst() {
        assertThat(labels(listOf(telegram), "телеграм").first()).isEqualTo("Telegram")
        assertThat(labels(ten, "телеграм").first()).isEqualTo("Telegram")
    }

    @Test
    fun subsequence_tlgrm_isTop3() {
        assertThat(labels(listOf(telegram), "tlgrm").take(3)).contains("Telegram")
        assertThat(labels(ten, "tlgrm").take(3)).contains("Telegram")
    }

    @Test
    fun initials_gm_findsBothGoogleMapsAndGmail() {
        assertThat(labels(listOf(googleMaps, gmail), "gm").take(3)).containsExactly("Google Maps", "Gmail")
        assertThat(labels(ten, "gm").take(3)).containsAtLeast("Google Maps", "Gmail")
    }

    @Test
    fun latinQueryFindsCyrillicName() {
        assertThat(labels(listOf(sber), "sber").first()).isEqualTo("СберБанк")
        assertThat(labels(ten, "sber").first()).isEqualTo("СберБанк")
    }

    @Test
    fun renamedApp_isFoundByCustomAndOriginalName() {
        val mail = app("Gmail", custom = "Почта")
        assertThat(labels(listOf(mail), "поч").first()).isEqualTo("Почта")
        assertThat(labels(ten - gmail + mail, "поч").first()).isEqualTo("Почта")
        assertThat(labels(ten - gmail + mail, "gm").take(3)).contains("Почта")
    }

    @Test
    fun typo_slakc_isTop3() {
        assertThat(labels(listOf(slack), "slakc").take(3)).contains("Slack")
        assertThat(labels(ten + slides + signal, "slakc").take(3)).contains("Slack")
    }

    @Test
    fun fuzzyWithOneEdit_findsTelegram() {
        assertThat(labels(ten, "telegarm").take(3)).contains("Telegram")
    }

    @Test
    fun emptyQuery_isAlphabeticalCyrillicFirst() {
        val index = SearchIndex.build(ten)
        assertThat(index.alphabetical.map { it.displayLabel }).containsExactly(
            "Камера",
            "СберБанк",
            "Gmail",
            "Google Maps",
            "Settings",
            "Slack",
            "Spotify",
            "Telegram",
            "WhatsApp",
            "YouTube",
        ).inOrder()
        assertThat(engine.search(index, "", emptyMap(), now)).isEmpty()
    }

    @Test
    fun garbage_xyzq_findsNothing() {
        assertThat(labels(ten, "xyzq")).isEmpty()
    }

    @Test
    fun AC_09_4_hiddenAppsOnlyOnExactOrPrefix() {
        val hiddenSlack = app("Slack", hidden = true)
        val index = SearchIndex.build(listOf(hiddenSlack, telegram))
        assertThat(index.alphabetical.map { it.displayLabel }).containsExactly("Telegram")
        val prefix = engine.search(index, "sla", emptyMap(), now)
        assertThat(prefix.map { it.label }).containsExactly("Slack")
        assertThat(prefix.single().hidden).isTrue()
        assertThat(engine.search(index, "ack", emptyMap(), now)).isEmpty()
        assertThat(engine.search(index, "slk", emptyMap(), now)).isEmpty()
        assertThat(engine.search(index, "слак", emptyMap(), now).single().level).isEqualTo(MatchLevel.EXACT)
    }

    @Test
    fun showHiddenModeFindsHiddenAppsLikeVisibleOnes() {
        val hiddenSlack = app("Slack", hidden = true)
        val index = SearchIndex.build(listOf(hiddenSlack, telegram))
        assertThat(index.alphabeticalWithHidden.map { it.displayLabel }).containsExactly("Slack", "Telegram").inOrder()
        val infix = engine.search(index, "ack", emptyMap(), now, includeHidden = true)
        assertThat(infix.map { it.label }).containsExactly("Slack")
        assertThat(infix.single().hidden).isTrue()
    }

    @Test
    fun wordPrefix_findsSecondWord() {
        val results = engine.search(SearchIndex.build(ten), "maps", emptyMap(), now)
        assertThat(results.first().label).isEqualTo("Google Maps")
        assertThat(results.first().level).isEqualTo(MatchLevel.WORD_PREFIX)
    }

    @Test
    fun camelCaseWords_areSplit() {
        assertThat(labels(ten, "app").first()).isEqualTo("WhatsApp")
        assertThat(labels(ten, "bank").first()).isEqualTo("СберБанк")
    }

    @Test
    fun concatenatedMultiWordName_isPrefix() {
        assertThat(labels(ten, "googlemaps").first()).isEqualTo("Google Maps")
    }

    @Test
    fun yo_isNormalizedToYe() {
        val app = app("Ёлка")
        assertThat(labels(listOf(app), "елк").first()).isEqualTo("Ёлка")
        assertThat(labels(listOf(app), "ёлк").first()).isEqualTo("Ёлка")
    }

    @Test
    fun resultsAreCappedAt50() {
        val many = (1..80).map { app("App$it") }
        assertThat(engine.search(SearchIndex.build(many), "app", emptyMap(), now)).hasSize(50)
    }

    @Test
    fun recentLaunchWithinHourAddsBonus() {
        val a = app("Notes")
        val b = app("Notion")
        val usage = mapOf(b.key to AppUsage(b.key, 1, now - 10 * 60 * 1000, 1.0))
        assertThat(labels(listOf(a, b), "no", usage).first()).isEqualTo("Notion")
    }

    @Test
    fun optionsDisableLayoutSwapAndTranslit() {
        val strict = SearchEngine(SearchOptions(fuzzy = false, layoutSwap = false, translit = false))
        val index = SearchIndex.build(ten)
        assertThat(strict.search(index, "ыдфсл", emptyMap(), now)).isEmpty()
        assertThat(strict.search(index, "слак", emptyMap(), now)).isEmpty()
        assertThat(strict.search(index, "telegarm", emptyMap(), now)).isEmpty()
        assertThat(strict.search(index, "tele", emptyMap(), now).map { it.label }).contains("Telegram")
    }
}
