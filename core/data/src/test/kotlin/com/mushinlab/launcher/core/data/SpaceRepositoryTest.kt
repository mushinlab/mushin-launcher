package com.mushinlab.launcher.core.data

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.data.repo.HistoryRepository
import com.mushinlab.launcher.core.data.repo.SpaceError
import com.mushinlab.launcher.core.data.repo.SpaceOutcome
import com.mushinlab.launcher.core.data.repo.SpaceRepository
import com.mushinlab.launcher.core.data.repo.resolveCurrentSpace
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.HomeListOffset
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.Palettes
import com.mushinlab.launcher.core.model.SpaceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpaceRepositoryTest {
    @get:Rule
    val rule = DbTestRule()

    private val slack = AppKey("com.slack", "com.slack.Main", 0)
    private val maps = AppKey("com.google.maps", "com.google.maps.Main", 0)
    private val mail = AppKey("com.google.gm", "com.google.gm.Main", 0)

    @Test
    fun AC_05_1_seedCreatesNormalGraphiteAndWorkNavy() = runTest {
        val repo = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val spaces = repo.spaces.first { it.isNotEmpty() }
        assertThat(spaces.map { it.name to it.type }).containsExactly(
            "Обычный" to SpaceType.NORMAL,
            "Работа" to SpaceType.WORK,
        ).inOrder()
        assertThat(spaces[0].palette).isEqualTo(Palettes.Graphite)
        assertThat(spaces[1].palette).isEqualTo(Palettes.Navy)
    }

    @Test
    fun AC_05_4_normalIsNotDeletableAndUnique() = runTest {
        val repo = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val normal = repo.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.NORMAL }
        assertThat(repo.delete(normal.id)).isEqualTo(SpaceOutcome.Failed(SpaceError.NormalNotDeletable))
        assertThat(
            repo.create("Ещё", SpaceType.NORMAL, PalettePreset.MOSS),
        ).isEqualTo(SpaceOutcome.Failed(SpaceError.NormalIsUnique))
    }

    @Test
    fun AC_05_4_atMostOneWorkAndItCanBeRecreated() = runTest {
        val repo = SpaceRepository(rule.db, backgroundScope, rule.clock)
        assertThat(
            repo.create("Офис", SpaceType.WORK, PalettePreset.NAVY),
        ).isEqualTo(SpaceOutcome.Failed(SpaceError.WorkLimitReached))
        val work = repo.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.WORK }
        assertThat(repo.delete(work.id)).isEqualTo(SpaceOutcome.Ok(Unit))
        assertThat(repo.create("Офис", SpaceType.WORK, PalettePreset.NAVY)).isInstanceOf(SpaceOutcome.Ok::class.java)
    }

    @Test
    fun AC_05_4_atMostTwentyCustom() = runTest {
        val repo = SpaceRepository(rule.db, backgroundScope, rule.clock)
        repeat(20) { i ->
            assertThat(
                repo.create("Свой $i", SpaceType.CUSTOM, PalettePreset.MOSS),
            ).isInstanceOf(SpaceOutcome.Ok::class.java)
        }
        assertThat(repo.create("Свой 21", SpaceType.CUSTOM, PalettePreset.MOSS))
            .isEqualTo(SpaceOutcome.Failed(SpaceError.CustomLimitReached))
    }

    @Test
    fun AC_05_6_nameIsTrimmedBoundedAndUniqueIgnoringCase() = runTest {
        val repo = SpaceRepository(rule.db, backgroundScope, rule.clock)
        assertThat(
            repo.create("   ", SpaceType.CUSTOM, PalettePreset.MOSS),
        ).isEqualTo(SpaceOutcome.Failed(SpaceError.NameEmpty))
        assertThat(repo.create("x".repeat(25), SpaceType.CUSTOM, PalettePreset.MOSS))
            .isEqualTo(SpaceOutcome.Failed(SpaceError.NameTooLong))
        assertThat(repo.create("  ОБЫЧНЫЙ ", SpaceType.CUSTOM, PalettePreset.MOSS))
            .isEqualTo(SpaceOutcome.Failed(SpaceError.NameTaken))
        val created = repo.create("  Спорт  ", SpaceType.CUSTOM, PalettePreset.MOSS)
        assertThat(created).isInstanceOf(SpaceOutcome.Ok::class.java)
        val names = repo.spaces.first { spaces -> spaces.any { it.type == SpaceType.CUSTOM } }.map { it.name }
        assertThat(names).contains("Спорт")
        assertThat(
            repo.create("x".repeat(24), SpaceType.CUSTOM, PalettePreset.MOSS),
        ).isInstanceOf(SpaceOutcome.Ok::class.java)
    }

    @Test
    fun renameRejectsDuplicateButAllowsOwnNameInOtherCase() = runTest {
        val repo = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val spaces = repo.spaces.first { it.isNotEmpty() }
        val work = spaces.first { it.type == SpaceType.WORK }
        assertThat(repo.rename(work.id, "обычный")).isEqualTo(SpaceOutcome.Failed(SpaceError.NameTaken))
        assertThat(repo.rename(work.id, "РАБОТА")).isEqualTo(SpaceOutcome.Ok(Unit))
    }

    @Test
    fun itemsKeepDenseOrderAfterReorderAndRemove() = runTest {
        val repo = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val normal = repo.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.NORMAL }
        repo.setItems(normal.id, listOf(slack, maps, mail))
        repo.setItems(normal.id, listOf(mail, slack, maps))
        assertThat(repo.itemsOf(normal.id)).containsExactly(mail, slack, maps).inOrder()
        repo.removeItem(normal.id, slack)
        assertThat(repo.itemsOf(normal.id)).containsExactly(mail, maps).inOrder()
        val positions = rule.db.spaceItemDao().get(normal.id).map { it.position }
        assertThat(positions).containsExactly(0, 1).inOrder()
        assertThat(repo.items.first { it[normal.id]?.size == 2 }[normal.id]).containsExactly(mail, maps).inOrder()
    }

    @Test
    fun addItemsRespectsLimitAndSkipsDuplicates() = runTest {
        val repo = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val normal = repo.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.NORMAL }
        assertThat(repo.addItems(normal.id, listOf(slack, maps), limit = 2)).isEqualTo(SpaceOutcome.Ok(Unit))
        assertThat(repo.addItems(normal.id, listOf(slack), limit = 2)).isEqualTo(SpaceOutcome.Ok(Unit))
        assertThat(
            repo.addItems(normal.id, listOf(mail), limit = 2),
        ).isEqualTo(SpaceOutcome.Failed(SpaceError.ItemLimitReached(2)))
        assertThat(repo.itemsOf(normal.id)).containsExactly(slack, maps).inOrder()
    }

    @Test
    fun AC_05_5_deletingSpaceCascadesItemsAndHistory() = runTest {
        val repo = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val history = HistoryRepository(rule.db, backgroundScope, rule.clock)
        val work = repo.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.WORK }
        repo.setItems(work.id, listOf(slack))
        history.recordLaunch(maps, work.id, fromSearch = true)
        repo.delete(work.id)
        assertThat(rule.db.spaceItemDao().get(work.id)).isEmpty()
        assertThat(
            rule.db.searchRecentDao().get(work.id, maps.packageName, maps.activityName, maps.userSerial),
        ).isNull()
    }

    @Test
    fun AC_05_5_currentFallsBackToNormalWhenMissing() = runTest {
        val repo = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val spaces = repo.spaces.first { it.isNotEmpty() }
        val work = spaces.first { it.type == SpaceType.WORK }
        assertThat(resolveCurrentSpace(spaces, work.id)?.id).isEqualTo(work.id)
        assertThat(resolveCurrentSpace(spaces, 999L)?.type).isEqualTo(SpaceType.NORMAL)
        assertThat(resolveCurrentSpace(spaces, null)?.type).isEqualTo(SpaceType.NORMAL)
    }

    @Test
    fun setPaletteUpdatesAllRoles() = runTest {
        val repo = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val normal = repo.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.NORMAL }
        repo.setPalette(normal.id, PalettePreset.PAPER)
        val updated = repo.spaces.first { spaces -> spaces.any { it.preset == PalettePreset.PAPER } }
        assertThat(updated.first { it.id == normal.id }.palette).isEqualTo(Palettes.Paper)
    }

    @Test
    fun presetSpacesFollowTheCurrentPaletteEvenWithOldStoredColours() = runTest {
        val repo = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val normal = repo.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.NORMAL }
        val old = 0x0E0E0E or 0xFF shl 24
        rule.db.spaceDao().setPalette(
            normal.id, PalettePreset.GRAPHITE.name, old, old, old, -1, -1, -1, -1, old, old, old, old, true,
        )
        val stored = repo.spaces.first { spaces ->
            spaces.any {
                it.id == normal.id &&
                    it.preset == PalettePreset.GRAPHITE
            }
        }
        assertThat(stored.first { it.id == normal.id }.palette).isEqualTo(Palettes.Graphite)
        rule.db.spaceDao().setPalette(
            normal.id, PalettePreset.CUSTOM.name, old, old, old, -1, -1, -1, -1, old, old, old, old, true,
        )
        val custom = repo.spaces.first { spaces ->
            spaces.any {
                it.id == normal.id &&
                    it.preset == PalettePreset.CUSTOM
            }
        }
        assertThat(custom.first { it.id == normal.id }.palette.textPrimary).isEqualTo(-1)
    }

    @Test
    fun colouredIconsAreSetPerSpace() = runTest {
        val repo = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val spaces = repo.spaces.first { it.size >= 2 }
        val (normal, work) = spaces.first { it.type == SpaceType.NORMAL } to spaces.first { it.type == SpaceType.WORK }
        assertThat(spaces.none { it.coloredIcons }).isTrue()
        repo.setColoredIcons(work.id, true)
        val updated = repo.spaces.first { list -> list.any { it.id == work.id && it.coloredIcons } }
        assertThat(updated.first { it.id == normal.id }.coloredIcons).isFalse()
    }

    @Test
    fun listPositionIsSetPerSpace() = runTest {
        val repo = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val spaces = repo.spaces.first { it.size >= 2 }
        val (normal, work) = spaces.first { it.type == SpaceType.NORMAL } to spaces.first { it.type == SpaceType.WORK }
        assertThat(spaces.all { it.listOffset == null }).isTrue()
        repo.setListOffset(work.id, 120)
        repo.setListOffset(normal.id, HomeListOffset.MAX_DP + 100)
        val updated = repo.spaces.first { list -> list.all { it.listOffset != null } }
        assertThat(updated.first { it.id == work.id }.listOffset).isEqualTo(120)
        assertThat(updated.first { it.id == normal.id }.listOffset).isEqualTo(HomeListOffset.MAX_DP)
    }
}
