package com.bluecubelabs.env.core.search

import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.LearnedGesture
import com.google.common.truth.Truth.assertThat
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
    fun storeIsCapped() {
        val many = (0 until GestureLearning.MAX_SAMPLES).map { LearnedGesture("p$it", maps, 1.0) }
        val next = GestureLearning.learn(many, listOf("new"), mail)
        assertThat(next).hasSize(GestureLearning.MAX_SAMPLES)
    }
}
