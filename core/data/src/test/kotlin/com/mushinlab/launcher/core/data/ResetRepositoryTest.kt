package com.mushinlab.launcher.core.data

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.data.db.MushinDatabase
import com.mushinlab.launcher.core.data.repo.PrivateSpaceRepository
import com.mushinlab.launcher.core.data.repo.ResetRepository
import com.mushinlab.launcher.core.data.repo.SpaceOutcome
import com.mushinlab.launcher.core.data.repo.SpaceRepository
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.SpaceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ResetRepositoryTest {
    @get:Rule
    val database = DbTestRule()

    @Test
    fun AC_53_5_resetDropsEverythingAndSeedsNormalAndWork() = runTest {
        val spaces = SpaceRepository(database.db, backgroundScope) { database.now }
        val initial = spaces.spaces.first { it.size >= 2 }
        val normal = initial.first { it.type == SpaceType.NORMAL }
        val created = spaces.create("Custom", SpaceType.CUSTOM, PalettePreset.MOSS)
        val custom = (created as SpaceOutcome.Ok).value
        spaces.addItems(custom, listOf(AppKey("com.example.one", "Main", 0)), limit = 8)
        spaces.items.first { it.values.flatten().isNotEmpty() }

        ResetRepository(database.db) { database.now }.reset(MushinDatabase.SeedNames("Default", "Work"))

        val reset = spaces.spaces.first { list -> list.none { it.type == SpaceType.CUSTOM } }
        assertThat(reset.map { it.type }).containsExactly(SpaceType.NORMAL, SpaceType.WORK).inOrder()
        assertThat(reset.first { it.type == SpaceType.NORMAL }.name).isEqualTo("Default")
        assertThat(normal.id).isNotEqualTo(custom)
        assertThat(spaces.items.first { it[custom] == null }[custom]).isNull()
    }

    @Test
    fun ensurePrivateCreatesASinglePrivateSpace() = runTest {
        val spaces = SpaceRepository(database.db, backgroundScope) { database.now }
        val private = PrivateSpaceRepository(database.db) { database.now }
        val first = private.ensure("Private", PalettePreset.NIGHT)
        val second = private.ensure("Private", PalettePreset.NIGHT)
        assertThat(second).isEqualTo(first)
        assertThat(spaces.spaces.first { list -> list.count { it.type == SpaceType.PRIVATE } == 1 })
            .isNotEmpty()
    }
}
