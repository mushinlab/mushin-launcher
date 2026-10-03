package com.bluecubelabs.env.core.data

import com.bluecubelabs.env.core.data.repo.GestureLearningRepository
import com.bluecubelabs.env.core.data.repo.PackageDataCleaner
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.LearnedGesture
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GestureLearningRepositoryTest {
    @get:Rule
    val rule = DbTestRule()

    private val slack = AppKey("com.slack", "com.slack.Main", 0)
    private val maps = AppKey("com.google.maps", "com.google.maps.Main", 0)

    @Test
    fun samplesAreStoredAndReplacedByTransform() = runTest {
        val repo = GestureLearningRepository(rule.db, backgroundScope)
        repo.update { it + LearnedGesture("sdfghjkl", slack, 1.0) + LearnedGesture("mnbvcxsa", maps, 2.0) }
        assertThat(repo.samples.first { it.size == 2 }).containsExactly(
            LearnedGesture("sdfghjkl", slack, 1.0),
            LearnedGesture("mnbvcxsa", maps, 2.0),
        )
        repo.update { list -> list.map { it.copy(weight = it.weight + 1) } }
        assertThat(repo.samples.first { s -> s.any { it.weight == 3.0 } }.map { it.weight }).containsExactly(2.0, 3.0)
    }

    @Test
    fun removedPackageForgetsItsGestures() = runTest {
        val repo = GestureLearningRepository(rule.db, backgroundScope)
        repo.update { listOf(LearnedGesture("sdfghjkl", slack, 1.0), LearnedGesture("mnbvcxsa", maps, 1.0)) }
        PackageDataCleaner(rule.db, rule.clock).onPackageRemoved(slack.packageName, slack.userSerial)
        assertThat(repo.samples.first { it.size == 1 }.single().key).isEqualTo(maps)
    }
}
