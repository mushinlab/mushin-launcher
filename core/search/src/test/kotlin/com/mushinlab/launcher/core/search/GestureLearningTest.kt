package com.mushinlab.launcher.core.search

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.LearnedGesture
import com.mushinlab.launcher.core.model.ShapeVertex
import org.junit.Test

class GestureLearningTest {
    private val maps = AppKey("maps", "A", 0)
    private val mail = AppKey("mail", "A", 0)

    @Test
    fun similarityIsOneForEqualPathsAndFallsWithEdits() {
        assertThat(GestureLearning.similarity("mnbvcxs", "mnbvcxs")).isEqualTo(1.0)
        assertThat(GestureLearning.similarity("mnbvcxs", "mnbvcx")).isWithin(1e-9).of(1.0 - 1.0 / 7)
        assertThat(GestureLearning.similarity("", "")).isEqualTo(1.0)
    }

    @Test
    fun learningAddsWeightForEachRepeat() {
        val once = GestureLearning.learn(emptyList(), listOf("mnbvcxs"), maps)
        assertThat(once).containsExactly(LearnedGesture("mnbvcxs", maps, 1.0))
        val twice = GestureLearning.learn(once, listOf("mnbvcxs", "mnbvcxs"), maps)
        assertThat(twice).containsExactly(LearnedGesture("mnbvcxs", maps, 2.0))
    }

    @Test
    fun changingMindWeakensTheOldAppForSimilarPaths() {
        val old = listOf(LearnedGesture("mnbvcxs", maps, 2.0), LearnedGesture("qwerty", maps, 1.0))
        val next = GestureLearning.learn(old, listOf("mnbvcx"), mail)
        assertThat(next).containsExactly(
            LearnedGesture("mnbvcx", mail, 1.0),
            LearnedGesture("mnbvcxs", maps, 1.0),
            LearnedGesture("qwerty", maps, 1.0),
        )
        val again = GestureLearning.learn(GestureLearning.learn(next, listOf("mnbvcx"), mail), listOf("mnbvcx"), mail)
        assertThat(again.first { it.key == maps && it.path == "mnbvcxs" }.weight).isEqualTo(0.25)
        assertThat(GestureLearning.learn(again, listOf("mnbvcx"), mail).none { it.path == "mnbvcxs" }).isTrue()
    }

    @Test
    fun overlongPathsAreNotLearned() {
        val scribble = "qp".repeat(GestureLearning.MAX_PATH)
        assertThat(GestureLearning.learn(emptyList(), listOf(scribble, "mnbvcxs"), maps))
            .containsExactly(LearnedGesture("mnbvcxs", maps, 1.0))
    }

    @Test
    fun storeIsCapped() {
        val many = (0 until GestureLearning.MAX_SAMPLES).map { LearnedGesture("p$it", maps, 1.0) }
        val next = GestureLearning.learn(many, listOf("new"), mail)
        assertThat(next).hasSize(GestureLearning.MAX_SAMPLES)
    }

    @Test
    fun learningKeepsTheShapeOfTheDrawing() {
        val shape = listOf(ShapeVertex(0f, 0f), ShapeVertex(0.5f, 1f, 700), ShapeVertex(1f, 0f))
        val once = GestureLearning.learn(emptyList(), listOf(GestureSample("mnb", shape)), maps)
        assertThat(once.single().shape).isEqualTo(shape)
        val again = GestureLearning.learn(once, listOf(GestureSample("mnb")), maps)
        assertThat(again.single().shape).isEqualTo(shape)
        assertThat(again.single().weight).isEqualTo(2.0)
    }

    @Test
    fun passiveLearningNeedsADrawingThatResemblesTheAppName() {
        val keys: Map<Char, GesturePoint> = buildMap {
            listOf("qwertyuiop" to 0f, "asdfghjkl" to 0.5f, "zxcvbnm" to 1.5f).forEachIndexed { row, (letters, shift) ->
                letters.forEachIndexed { i, c -> put(c, GesturePoint(i + shift, row.toFloat())) }
            }
        }
        val decoder = GestureDecoder(keys)
        val sloppy = "discord".mapIndexed { i, c ->
            keys.getValue(c).let { GesturePoint(it.x + if (i % 2 == 0) 0.6f else -0.6f, it.y + 0.4f) }
        }
        val unrelated = "ijn".map { keys.getValue(it) }
        assertThat(GestureLearning.resembles(decoder, sloppy, listOf("discord"))).isTrue()
        assertThat(GestureLearning.resembles(decoder, unrelated, listOf("discord"))).isFalse()
        assertThat(GestureLearning.resembles(decoder, unrelated, listOf("1x"))).isFalse()
    }
}
