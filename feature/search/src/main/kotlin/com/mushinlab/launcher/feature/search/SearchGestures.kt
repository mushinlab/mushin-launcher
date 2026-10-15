package com.mushinlab.launcher.feature.search

import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.ShapeVertex
import com.mushinlab.launcher.core.search.GestureDecoder
import com.mushinlab.launcher.core.search.GestureLearning
import com.mushinlab.launcher.core.search.GesturePoint
import com.mushinlab.launcher.core.search.GestureSample

class SearchGestures internal constructor(
    private val sources: SearchSources,
    private val onWord: (String) -> Unit,
    private val onWords: (List<AppKey>) -> Unit,
) {
    private val drawings = mutableListOf<Drawing>()
    private var decoder: GestureDecoder? = null
    private var corrected = false

    fun decode(
        decoder: GestureDecoder,
        path: List<GesturePoint>,
        shape: List<ShapeVertex> = emptyList(),
    ): Boolean {
        val result = decoder.decode(path, sources.index.gestureVocabulary, sources.learned) ?: return true
        this.decoder = decoder
        drawings += Drawing(path, GestureSample(result.path, shape))
        if (drawings.size > 1) corrected = true
        val words = result.words
        when {
            words.size == 1 -> onWord(words.single().text)

            words.isNotEmpty() -> {
                corrected = true
                onWords(words.map { it.key })
            }
        }
        return words.isNotEmpty()
    }

    fun toLearn(key: AppKey): List<GestureSample> {
        val decoder = decoder
        if (decoder == null || !corrected) return emptyList()
        val words = sources.index.gestureVocabulary.filter { it.key == key }.map { it.latin }
        return drawings.filter { GestureLearning.resembles(decoder, it.points, words) }.map { it.sample }
    }

    internal fun markCorrected() {
        if (drawings.isNotEmpty()) corrected = true
    }

    internal fun reset() {
        decoder = null
        drawings.clear()
        corrected = false
    }
}

private class Drawing(val points: List<GesturePoint>, val sample: GestureSample)
