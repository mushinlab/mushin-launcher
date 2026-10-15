package com.mushinlab.launcher.core.data

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.data.repo.EnvError
import com.mushinlab.launcher.core.data.repo.EnvOutcome
import com.mushinlab.launcher.core.data.repo.EnvironmentRepository
import com.mushinlab.launcher.core.data.repo.HistoryRepository
import com.mushinlab.launcher.core.data.repo.resolveCurrentEnvironment
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.EnvType
import com.mushinlab.launcher.core.model.HomeListOffset
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.Palettes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EnvironmentRepositoryTest {
    @get:Rule
    val rule = DbTestRule()

    private val slack = AppKey("com.slack", "com.slack.Main", 0)
    private val maps = AppKey("com.google.maps", "com.google.maps.Main", 0)
    private val mail = AppKey("com.google.gm", "com.google.gm.Main", 0)

    @Test
    fun AC_05_1_seedCreatesNormalGraphiteAndWorkNavy() = runTest {
        val repo = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        val envs = repo.environments.first { it.isNotEmpty() }
        assertThat(envs.map { it.name to it.type }).containsExactly(
            "Обычный" to EnvType.NORMAL,
            "Работа" to EnvType.WORK,
        ).inOrder()
        assertThat(envs[0].palette).isEqualTo(Palettes.Graphite)
        assertThat(envs[1].palette).isEqualTo(Palettes.Navy)
    }

    @Test
    fun AC_05_4_normalIsNotDeletableAndUnique() = runTest {
        val repo = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        val normal = repo.environments.first { it.isNotEmpty() }.first { it.type == EnvType.NORMAL }
        assertThat(repo.delete(normal.id)).isEqualTo(EnvOutcome.Failed(EnvError.NormalNotDeletable))
        assertThat(
            repo.create("Ещё", EnvType.NORMAL, PalettePreset.MOSS),
        ).isEqualTo(EnvOutcome.Failed(EnvError.NormalIsUnique))
    }

    @Test
    fun AC_05_4_atMostOneWorkAndItCanBeRecreated() = runTest {
        val repo = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        assertThat(
            repo.create("Офис", EnvType.WORK, PalettePreset.NAVY),
        ).isEqualTo(EnvOutcome.Failed(EnvError.WorkLimitReached))
        val work = repo.environments.first { it.isNotEmpty() }.first { it.type == EnvType.WORK }
        assertThat(repo.delete(work.id)).isEqualTo(EnvOutcome.Ok(Unit))
        assertThat(repo.create("Офис", EnvType.WORK, PalettePreset.NAVY)).isInstanceOf(EnvOutcome.Ok::class.java)
    }

    @Test
    fun AC_05_4_atMostTwentyCustom() = runTest {
        val repo = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        repeat(20) { i ->
            assertThat(
                repo.create("Свой $i", EnvType.CUSTOM, PalettePreset.MOSS),
            ).isInstanceOf(EnvOutcome.Ok::class.java)
        }
        assertThat(repo.create("Свой 21", EnvType.CUSTOM, PalettePreset.MOSS))
            .isEqualTo(EnvOutcome.Failed(EnvError.CustomLimitReached))
    }

    @Test
    fun AC_05_6_nameIsTrimmedBoundedAndUniqueIgnoringCase() = runTest {
        val repo = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        assertThat(
            repo.create("   ", EnvType.CUSTOM, PalettePreset.MOSS),
        ).isEqualTo(EnvOutcome.Failed(EnvError.NameEmpty))
        assertThat(repo.create("x".repeat(25), EnvType.CUSTOM, PalettePreset.MOSS))
            .isEqualTo(EnvOutcome.Failed(EnvError.NameTooLong))
        assertThat(repo.create("  ОБЫЧНЫЙ ", EnvType.CUSTOM, PalettePreset.MOSS))
            .isEqualTo(EnvOutcome.Failed(EnvError.NameTaken))
        val created = repo.create("  Спорт  ", EnvType.CUSTOM, PalettePreset.MOSS)
        assertThat(created).isInstanceOf(EnvOutcome.Ok::class.java)
        val names = repo.environments.first { envs -> envs.any { it.type == EnvType.CUSTOM } }.map { it.name }
        assertThat(names).contains("Спорт")
        assertThat(
            repo.create("x".repeat(24), EnvType.CUSTOM, PalettePreset.MOSS),
        ).isInstanceOf(EnvOutcome.Ok::class.java)
    }

    @Test
    fun renameRejectsDuplicateButAllowsOwnNameInOtherCase() = runTest {
        val repo = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        val envs = repo.environments.first { it.isNotEmpty() }
        val work = envs.first { it.type == EnvType.WORK }
        assertThat(repo.rename(work.id, "обычный")).isEqualTo(EnvOutcome.Failed(EnvError.NameTaken))
        assertThat(repo.rename(work.id, "РАБОТА")).isEqualTo(EnvOutcome.Ok(Unit))
    }

    @Test
    fun itemsKeepDenseOrderAfterReorderAndRemove() = runTest {
        val repo = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        val normal = repo.environments.first { it.isNotEmpty() }.first { it.type == EnvType.NORMAL }
        repo.setItems(normal.id, listOf(slack, maps, mail))
        repo.setItems(normal.id, listOf(mail, slack, maps))
        assertThat(repo.itemsOf(normal.id)).containsExactly(mail, slack, maps).inOrder()
        repo.removeItem(normal.id, slack)
        assertThat(repo.itemsOf(normal.id)).containsExactly(mail, maps).inOrder()
        val positions = rule.db.environmentItemDao().get(normal.id).map { it.position }
        assertThat(positions).containsExactly(0, 1).inOrder()
        assertThat(repo.items.first { it[normal.id]?.size == 2 }[normal.id]).containsExactly(mail, maps).inOrder()
    }

    @Test
    fun addItemsRespectsLimitAndSkipsDuplicates() = runTest {
        val repo = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        val normal = repo.environments.first { it.isNotEmpty() }.first { it.type == EnvType.NORMAL }
        assertThat(repo.addItems(normal.id, listOf(slack, maps), limit = 2)).isEqualTo(EnvOutcome.Ok(Unit))
        assertThat(repo.addItems(normal.id, listOf(slack), limit = 2)).isEqualTo(EnvOutcome.Ok(Unit))
        assertThat(
            repo.addItems(normal.id, listOf(mail), limit = 2),
        ).isEqualTo(EnvOutcome.Failed(EnvError.ItemLimitReached(2)))
        assertThat(repo.itemsOf(normal.id)).containsExactly(slack, maps).inOrder()
    }

    @Test
    fun AC_05_5_deletingEnvCascadesItemsAndHistory() = runTest {
        val repo = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        val history = HistoryRepository(rule.db, backgroundScope, rule.clock)
        val work = repo.environments.first { it.isNotEmpty() }.first { it.type == EnvType.WORK }
        repo.setItems(work.id, listOf(slack))
        history.recordLaunch(maps, work.id, fromSearch = true)
        repo.delete(work.id)
        assertThat(rule.db.environmentItemDao().get(work.id)).isEmpty()
        assertThat(
            rule.db.searchRecentDao().get(work.id, maps.packageName, maps.activityName, maps.userSerial),
        ).isNull()
    }

    @Test
    fun AC_05_5_currentFallsBackToNormalWhenMissing() = runTest {
        val repo = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        val envs = repo.environments.first { it.isNotEmpty() }
        val work = envs.first { it.type == EnvType.WORK }
        assertThat(resolveCurrentEnvironment(envs, work.id)?.id).isEqualTo(work.id)
        assertThat(resolveCurrentEnvironment(envs, 999L)?.type).isEqualTo(EnvType.NORMAL)
        assertThat(resolveCurrentEnvironment(envs, null)?.type).isEqualTo(EnvType.NORMAL)
    }

    @Test
    fun setPaletteUpdatesAllRoles() = runTest {
        val repo = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        val normal = repo.environments.first { it.isNotEmpty() }.first { it.type == EnvType.NORMAL }
        repo.setPalette(normal.id, PalettePreset.PAPER)
        val updated = repo.environments.first { envs -> envs.any { it.preset == PalettePreset.PAPER } }
        assertThat(updated.first { it.id == normal.id }.palette).isEqualTo(Palettes.Paper)
    }

    @Test
    fun presetSpacesFollowTheCurrentPaletteEvenWithOldStoredColours() = runTest {
        val repo = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        val normal = repo.environments.first { it.isNotEmpty() }.first { it.type == EnvType.NORMAL }
        val old = 0x0E0E0E or 0xFF shl 24
        rule.db.environmentDao().setPalette(
            normal.id, PalettePreset.GRAPHITE.name, old, old, old, -1, -1, -1, -1, old, old, old, old, true,
        )
        val stored = repo.environments.first { envs ->
            envs.any {
                it.id == normal.id &&
                    it.preset == PalettePreset.GRAPHITE
            }
        }
        assertThat(stored.first { it.id == normal.id }.palette).isEqualTo(Palettes.Graphite)
        rule.db.environmentDao().setPalette(
            normal.id, PalettePreset.CUSTOM.name, old, old, old, -1, -1, -1, -1, old, old, old, old, true,
        )
        val custom = repo.environments.first { envs ->
            envs.any {
                it.id == normal.id &&
                    it.preset == PalettePreset.CUSTOM
            }
        }
        assertThat(custom.first { it.id == normal.id }.palette.textPrimary).isEqualTo(-1)
    }

    @Test
    fun colouredIconsAreSetPerSpace() = runTest {
        val repo = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        val envs = repo.environments.first { it.size >= 2 }
        val (normal, work) = envs.first { it.type == EnvType.NORMAL } to envs.first { it.type == EnvType.WORK }
        assertThat(envs.none { it.coloredIcons }).isTrue()
        repo.setColoredIcons(work.id, true)
        val updated = repo.environments.first { list -> list.any { it.id == work.id && it.coloredIcons } }
        assertThat(updated.first { it.id == normal.id }.coloredIcons).isFalse()
    }

    @Test
    fun listPositionIsSetPerSpace() = runTest {
        val repo = EnvironmentRepository(rule.db, backgroundScope, rule.clock)
        val envs = repo.environments.first { it.size >= 2 }
        val (normal, work) = envs.first { it.type == EnvType.NORMAL } to envs.first { it.type == EnvType.WORK }
        assertThat(envs.all { it.listOffset == null }).isTrue()
        repo.setListOffset(work.id, 120)
        repo.setListOffset(normal.id, HomeListOffset.MAX_DP + 100)
        val updated = repo.environments.first { list -> list.all { it.listOffset != null } }
        assertThat(updated.first { it.id == work.id }.listOffset).isEqualTo(120)
        assertThat(updated.first { it.id == normal.id }.listOffset).isEqualTo(HomeListOffset.MAX_DP)
    }
}
