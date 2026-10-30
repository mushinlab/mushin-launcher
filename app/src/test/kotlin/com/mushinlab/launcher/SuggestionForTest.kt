package com.mushinlab.launcher

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.AppEntry
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.AppOverride
import com.mushinlab.launcher.core.model.FolderKeys
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.Palettes
import com.mushinlab.launcher.core.model.ProfileType
import com.mushinlab.launcher.core.model.Space
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.model.SpaceUsage
import com.mushinlab.launcher.core.model.Usefulness
import org.junit.Test

class SuggestionForTest {
    private val now = 1_000_000_000_000L
    private val home = AppKey("home", "Home", 0)
    private val frequent = AppKey("frequent", "Frequent", 0)
    private val swiped = AppKey("swiped", "Swiped", 0)
    private val elsewhere = AppKey("elsewhere", "Elsewhere", 0)
    private val normal = space(1, SpaceType.NORMAL)
    private val work = space(2, SpaceType.WORK)

    private fun space(id: Long, type: SpaceType) =
        Space(id, "s$id", type, id.toInt(), PalettePreset.GRAPHITE, Palettes.Graphite, 0)

    private fun entry(key: AppKey) = AppEntry(key, key.packageName, 0, 0, false, ProfileType.MAIN)

    private fun data() = LauncherData(
        spaces = listOf(normal, work),
        current = normal,
        items = mapOf(normal.id to listOf(home), work.id to listOf(elsewhere)),
        apps = listOf(home, frequent, swiped, elsewhere).associateWith(::entry),
        overrides = emptyMap(),
        settings = LauncherSettings(),
    )

    private fun usage(key: AppKey, score: Double) = SpaceUsage(normal.id, key, 10, now, score)

    private fun stats(vararg usage: SpaceUsage, days: Int = Usefulness.MIN_LAUNCH_DAYS) = SpaceStats(
        usage = mapOf(normal.id to usage.toList()),
        launchDays = mapOf(normal.id to usage.associate { it.key to days }),
    )

    private fun withSpace(space: Space) = data().copy(spaces = listOf(space, work), current = space)

    @Test
    fun appsBoundToASwipeOrPlacedInAnotherSpaceAreNeverSuggested() {
        val usage = stats(usage(swiped, 9.0), usage(elsewhere, 8.0), usage(frequent, 5.0))

        val hint = suggestionFor(data(), usage, setOf(swiped), cap = 5, now = now)

        assertThat(hint?.add).isEqualTo(frequent)
        assertThat(hint?.replace).isEqualTo(home)
        assertThat(hint?.canAdd).isTrue()
    }

    @Test
    fun aFolderIsNeverReplacedButTakesARow() {
        val folder = FolderKeys.of(9)
        val withFolder = data().copy(items = mapOf(normal.id to listOf(folder, home), work.id to listOf(elsewhere)))

        val hint = suggestionFor(withFolder, stats(usage(frequent, 5.0)), emptySet(), cap = 2, now = now)

        assertThat(hint?.replace).isEqualTo(home)
        assertThat(hint?.canAdd).isFalse()
    }

    @Test
    fun noSuggestionWhenOnlyExcludedAppsAreFrequent() {
        val usage = stats(usage(swiped, 9.0), usage(elsewhere, 8.0))

        assertThat(suggestionFor(data(), usage, setOf(swiped), cap = 1, now = now)).isNull()
    }

    @Test
    fun hiddenAppsAreNeverSuggested() {
        val usage = stats(usage(frequent, 9.0))
        val hidden = data().let { d ->
            d.copy(overrides = mapOf(frequent to AppOverride(frequent, null, true, null, 0)))
        }

        assertThat(suggestionFor(hidden, usage, emptySet(), cap = 5, now = now)).isNull()
    }

    @Test
    fun aSpaceObservedForLessThanAWeekGetsNoSuggestionYet() {
        val young = withSpace(normal.copy(createdAt = now - Usefulness.WEEK_MS + 1))
        val weekOld = withSpace(normal.copy(createdAt = now - Usefulness.WEEK_MS))

        assertThat(suggestionFor(young, stats(usage(frequent, 9.0)), emptySet(), cap = 5, now = now)).isNull()
        assertThat(suggestionFor(weekOld, stats(usage(frequent, 9.0)), emptySet(), cap = 5, now = now)?.add)
            .isEqualTo(frequent)
    }

    @Test
    fun aRecentlyAnsweredHintPausesSuggestionsInThatSpaceForAWeek() {
        val answered = withSpace(normal.copy(hintAt = now - Usefulness.WEEK_MS + 1))
        val weekAgo = withSpace(normal.copy(hintAt = now - Usefulness.WEEK_MS))

        assertThat(suggestionFor(answered, stats(usage(frequent, 9.0)), emptySet(), cap = 5, now = now)).isNull()
        assertThat(suggestionFor(weekAgo, stats(usage(frequent, 9.0)), emptySet(), cap = 5, now = now)?.add)
            .isEqualTo(frequent)
    }

    @Test
    fun launchesOnFewerThanThreeDaysOfTheCurrentSpaceAreNotEnough() {
        val twoDays = stats(usage(frequent, 9.0), days = Usefulness.MIN_LAUNCH_DAYS - 1)
        val otherSpaceDays = stats(usage(frequent, 9.0)).copy(launchDays = mapOf(work.id to mapOf(frequent to 7)))

        assertThat(suggestionFor(data(), twoDays, emptySet(), cap = 5, now = now)).isNull()
        assertThat(suggestionFor(data(), otherSpaceDays, emptySet(), cap = 5, now = now)).isNull()
    }
}
