package com.mushinlab.launcher.core.data

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.data.repo.FolderRepository
import com.mushinlab.launcher.core.data.repo.PackageDataCleaner
import com.mushinlab.launcher.core.data.repo.SpaceError
import com.mushinlab.launcher.core.data.repo.SpaceOutcome
import com.mushinlab.launcher.core.data.repo.SpaceRepository
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.FolderKeys
import com.mushinlab.launcher.core.model.SpaceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FolderRepositoryTest {
    @get:Rule
    val rule = DbTestRule()

    private val slack = AppKey("com.slack", "com.slack.Main", 0)
    private val maps = AppKey("com.google.maps", "com.google.maps.Main", 0)

    @Test
    fun newFolderTakesTheLastRowOfTheHomeList() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val folders = FolderRepository(rule.db, backgroundScope)
        val normal = spaces.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.NORMAL }
        spaces.setItems(normal.id, listOf(slack))
        val id = (folders.create(normal.id, limit = 5) as SpaceOutcome.Ok).value
        assertThat(spaces.itemsOf(normal.id)).containsExactly(slack, FolderKeys.of(id)).inOrder()
        val folder = folders.folders.first { id in it }.getValue(id)
        assertThat(folder.spaceId).isEqualTo(normal.id)
        assertThat(folder.name).isEmpty()
        assertThat(folder.apps).isEmpty()
    }

    @Test
    fun fullHomeListRejectsANewFolder() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val folders = FolderRepository(rule.db, backgroundScope)
        val normal = spaces.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.NORMAL }
        spaces.setItems(normal.id, listOf(slack, maps))
        assertThat(folders.create(normal.id, limit = 2)).isEqualTo(SpaceOutcome.Failed(SpaceError.ItemLimitReached(2)))
        assertThat(spaces.itemsOf(normal.id)).containsExactly(slack, maps).inOrder()
    }

    @Test
    fun appsKeepTheirOrderWithoutDuplicatesAndTheNameIsBounded() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val folders = FolderRepository(rule.db, backgroundScope)
        val normal = spaces.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.NORMAL }
        val id = (folders.create(normal.id, limit = 5) as SpaceOutcome.Ok).value
        folders.addApp(id, maps)
        folders.addApp(id, slack)
        folders.addApp(id, maps)
        folders.rename(id, "x".repeat(50))
        val folder = folders.folders.first { it[id]?.apps?.size == 2 && it[id]?.name?.isNotEmpty() == true }
            .getValue(id)
        assertThat(folder.apps).containsExactly(maps, slack).inOrder()
        assertThat(folder.name).hasLength(40)
        folders.removeApp(id, maps)
        assertThat(folders.folders.first { it[id]?.apps?.size == 1 }.getValue(id).apps).containsExactly(slack)
    }

    @Test
    fun deleteRemovesTheFolderAndItsHomeRow() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val folders = FolderRepository(rule.db, backgroundScope)
        val normal = spaces.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.NORMAL }
        spaces.setItems(normal.id, listOf(slack))
        val id = (folders.create(normal.id, limit = 5) as SpaceOutcome.Ok).value
        folders.addApp(id, maps)
        folders.delete(id)
        assertThat(spaces.itemsOf(normal.id)).containsExactly(slack)
        assertThat(folders.folders.first { id !in it }).isEmpty()
    }

    @Test
    fun onlyAFolderWithoutNameAndAppsIsDroppedAsEmpty() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val folders = FolderRepository(rule.db, backgroundScope)
        val normal = spaces.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.NORMAL }
        val named = (folders.create(normal.id, limit = 5) as SpaceOutcome.Ok).value
        folders.rename(named, "Банки")
        val filled = (folders.create(normal.id, limit = 5) as SpaceOutcome.Ok).value
        folders.addApp(filled, slack)
        val empty = (folders.create(normal.id, limit = 5) as SpaceOutcome.Ok).value
        folders.rename(empty, "   ")
        assertThat(folders.deleteIfEmpty(named)).isFalse()
        assertThat(folders.deleteIfEmpty(filled)).isFalse()
        assertThat(folders.deleteIfEmpty(empty)).isTrue()
        assertThat(spaces.itemsOf(normal.id)).containsExactly(FolderKeys.of(named), FolderKeys.of(filled)).inOrder()
    }

    @Test
    fun removedPackageLeavesFoldersAndDeletedSpaceTakesItsFolders() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val folders = FolderRepository(rule.db, backgroundScope)
        val work = spaces.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.WORK }
        val id = (folders.create(work.id, limit = 5) as SpaceOutcome.Ok).value
        folders.setApps(id, listOf(slack, maps))
        PackageDataCleaner(rule.db, rule.clock).onPackageRemoved(slack.packageName, slack.userSerial)
        assertThat(folders.folders.first { it[id]?.apps?.size == 1 }.getValue(id).apps).containsExactly(maps)
        spaces.delete(work.id)
        assertThat(folders.folders.first { it.isEmpty() }).isEmpty()
    }
}
