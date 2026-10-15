package com.mushinlab.launcher

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppEntry
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.AppOverride
import com.mushinlab.launcher.core.model.EnvType
import com.mushinlab.launcher.core.model.Environment
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.Palettes
import com.mushinlab.launcher.core.model.ProfileType
import com.mushinlab.launcher.core.model.SpaceUsage
import org.junit.Test

class SuggestionForTest {
    private val now = 1_000_000_000_000L
    private val home = AppKey("home", "Home", 0)
    private val frequent = AppKey("frequent", "Frequent", 0)
    private val swiped = AppKey("swiped", "Swiped", 0)
    private val elsewhere = AppKey("elsewhere", "Elsewhere", 0)
    private val normal = space(1, EnvType.NORMAL)
    private val work = space(2, EnvType.WORK)

    private fun space(id: Long, type: EnvType) =
        Environment(id, "s$id", type, id.toInt(), PalettePreset.GRAPHITE, Palettes.Graphite, 0)

    private fun entry(key: AppKey) = AppEntry(key, key.packageName, 0, 0, false, ProfileType.MAIN)

    private fun data() = LauncherData(
        envs = listOf(normal, work),
        current = normal,
        items = mapOf(normal.id to listOf(home), work.id to listOf(elsewhere)),
        apps = listOf(home, frequent, swiped, elsewhere).associateWith(::entry),
        overrides = emptyMap(),
        settings = LauncherSettings(),
    )

    private fun usage(key: AppKey, score: Double) = SpaceUsage(normal.id, key, 10, now, score)

    @Test
    fun appsBoundToASwipeOrPlacedInAnotherSpaceAreNeverSuggested() {
        val usage = mapOf(normal.id to listOf(usage(swiped, 9.0), usage(elsewhere, 8.0), usage(frequent, 5.0)))

        val hint = suggestionFor(data(), usage, setOf(swiped), cap = 5, now = now)

        assertThat(hint?.add).isEqualTo(frequent)
        assertThat(hint?.replace).isEqualTo(home)
        assertThat(hint?.canAdd).isTrue()
    }

    @Test
    fun noSuggestionWhenOnlyExcludedAppsAreFrequent() {
        val usage = mapOf(normal.id to listOf(usage(swiped, 9.0), usage(elsewhere, 8.0)))

        assertThat(suggestionFor(data(), usage, setOf(swiped), cap = 1, now = now)).isNull()
    }

    @Test
    fun appsBoundToASystemGestureAreNeverSuggested() {
        val usage = mapOf(normal.id to listOf(usage(frequent, 9.0)))
        val bound = data().let { d ->
            d.copy(overrides = mapOf(frequent to AppOverride(frequent, null, false, null, 0, systemShortcut = true)))
        }

        assertThat(suggestionFor(bound, usage, emptySet(), cap = 5, now = now)).isNull()
    }
}
