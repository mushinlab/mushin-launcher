package com.mushinlab.launcher.core.search.bench

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.search.SearchEngine
import com.mushinlab.launcher.core.search.SearchIndex
import com.mushinlab.launcher.core.search.SearchItem
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val items = syntheticApps(500)
    private val index = SearchIndex.build(items)
    private val engine = SearchEngine()
    private val queries = listOf("s", "sl", "sla", "slak", "ыдфсл", "телеграм", "tlgrm", "gm", "sber", "zzzq")

    @Test
    fun searchPerKeystroke500Apps() {
        var i = 0
        benchmarkRule.measureRepeated {
            engine.search(index, queries[i % queries.size])
            i++
        }
    }

    @Test
    fun buildIndex500Apps() {
        benchmarkRule.measureRepeated {
            SearchIndex.build(items)
        }
    }

    private fun syntheticApps(count: Int): List<SearchItem> {
        val random = Random(42)
        val latin = listOf("slack", "maps", "mail", "photo", "music", "notes", "bank", "cloud", "chat", "video", "pay")
        val cyrillic = listOf("сбер", "банк", "почта", "такси", "карты", "музыка", "заметки", "госуслуги", "погода")
        return List(count) { n ->
            val pool = if (random.nextBoolean()) latin else cyrillic
            val words =
                List(1 + random.nextInt(3)) { pool[random.nextInt(pool.size)].replaceFirstChar(Char::uppercase) }
            SearchItem(AppKey("pkg.$n", "Main", 0), words.joinToString(if (random.nextBoolean()) " " else "") + n)
        }
    }
}
