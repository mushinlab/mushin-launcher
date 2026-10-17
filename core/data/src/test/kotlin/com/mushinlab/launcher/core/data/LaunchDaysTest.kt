package com.mushinlab.launcher.core.data

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.data.repo.HistoryRepository
import com.mushinlab.launcher.core.data.repo.PackageDataCleaner
import com.mushinlab.launcher.core.data.repo.SpaceOutcome
import com.mushinlab.launcher.core.data.repo.SpaceRepository
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.model.Usefulness
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LaunchDaysTest {
    @get:Rule
    val rule = DbTestRule()

    private val slack = AppKey("com.slack", "com.slack.Main", 0)
    private val maps = AppKey("com.google.maps", "com.google.maps.Main", 0)
    private val zone = ZoneOffset.ofHours(3)
    private val noon = LocalDateTime.of(2026, 10, 17, 12, 0)
    private val day = 24L * 60 * 60 * 1000

    private fun TestScope.history(zone: ZoneId = this@LaunchDaysTest.zone) =
        HistoryRepository(rule.db, backgroundScope, rule.clock) { zone }

    private suspend fun normalId(repo: SpaceRepository): Long =
        repo.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.NORMAL }.id

    private fun storedDays(): List<Long> = rule.db.openHelper.readableDatabase
        .query("SELECT day FROM space_launch_day ORDER BY day")
        .use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getLong(0)) } }

    @Test
    fun launchesOnTheSameLocalDayCountOnce() = runTest {
        rule.now = noon.toInstant(zone).toEpochMilli()
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val history = history()
        val space = normalId(spaces)

        repeat(3) {
            history.recordLaunch(slack, space, fromSearch = false)
            rule.now += 60_000
        }
        rule.now += day
        history.recordLaunch(slack, space, fromSearch = false)

        assertThat(history.launchDays.first { it[space]?.get(slack) == 2 }[space]).containsExactly(slack, 2)
        assertThat(storedDays()).containsExactly(noon.toLocalDate().toEpochDay(), noon.toLocalDate().toEpochDay() + 1)
    }

    @Test
    fun theLocalTimeZoneDecidesWhereADayEnds() = runTest {
        rule.now = noon.withHour(23).toInstant(ZoneOffset.UTC).toEpochMilli()
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val history = history()
        val space = normalId(spaces)

        history.recordLaunch(slack, space, fromSearch = false)
        rule.now += 2 * 60 * 60 * 1000
        history.recordLaunch(slack, space, fromSearch = false)

        assertThat(storedDays()).containsExactly(noon.toLocalDate().toEpochDay() + 1)
    }

    @Test
    fun launchesWithoutASpaceRecordNoDay() = runTest {
        val history = history()

        history.recordLaunch(slack, null, fromSearch = false)

        assertThat(storedDays()).isEmpty()
    }

    @Test
    fun onlyTheLastSevenDaysAreCountedPerSpace() = runTest {
        rule.now = noon.toInstant(zone).toEpochMilli()
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val history = history()
        val normal = normalId(spaces)
        val work = spaces.spaces.first { it.isNotEmpty() }.first { it.type == SpaceType.WORK }.id

        repeat(Usefulness.WINDOW_DAYS + 1) {
            history.recordLaunch(slack, normal, fromSearch = false)
            rule.now += day
        }
        history.recordLaunch(maps, work, fromSearch = false)

        val days = history.launchDays.first { it[work] != null }
        assertThat(days[normal]).containsExactly(slack, Usefulness.WINDOW_DAYS - 1)
        assertThat(days[work]).containsExactly(maps, 1)
    }

    @Test
    fun daysOlderThanTwoWeeksArePrunedOnALaunch() = runTest {
        rule.now = noon.toInstant(zone).toEpochMilli()
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val history = history()
        val space = normalId(spaces)
        val first = noon.toLocalDate().toEpochDay()

        history.recordLaunch(slack, space, fromSearch = false)
        rule.now += day
        history.recordLaunch(maps, space, fromSearch = false)
        rule.now += (Usefulness.DAYS_KEPT - 1) * day
        history.recordLaunch(maps, space, fromSearch = false)

        assertThat(storedDays()).containsExactly(first, first + 1, first + Usefulness.DAYS_KEPT)

        rule.now += day
        history.recordLaunch(slack, space, fromSearch = false)

        assertThat(storedDays())
            .containsExactly(first + 1, first + Usefulness.DAYS_KEPT, first + Usefulness.DAYS_KEPT + 1)
    }

    @Test
    fun clearingUsageDeletesLaunchDaysButKeepsTheHintTime() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val history = history()
        val space = normalId(spaces)
        history.recordLaunch(slack, space, fromSearch = false)
        history.recordHint(space)
        history.launchDays.first { it.isNotEmpty() }

        history.clearUsage()

        assertThat(history.launchDays.first { it.isEmpty() }).isEmpty()
        assertThat(storedDays()).isEmpty()
        assertThat(spaces.spaces.first { s -> s.any { it.hintAt != null } }.first { it.id == space }.hintAt)
            .isEqualTo(rule.now)
    }

    @Test
    fun deletingASpaceDeletesItsLaunchDays() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val history = history()
        val normal = normalId(spaces)
        val custom = (spaces.create("Чтение", SpaceType.CUSTOM, PalettePreset.NAVY) as SpaceOutcome.Ok).value
        history.recordLaunch(slack, custom, fromSearch = false)
        history.recordLaunch(maps, normal, fromSearch = false)
        history.launchDays.first { it.size == 2 }

        spaces.delete(custom)

        val days = history.launchDays.first { it.size == 1 }
        assertThat(days.keys).containsExactly(normal)
    }

    @Test
    fun removingAPackageDeletesItsLaunchDays() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val history = history()
        val space = normalId(spaces)
        history.recordLaunch(slack, space, fromSearch = false)
        history.recordLaunch(maps, space, fromSearch = false)
        history.launchDays.first { it[space]?.size == 2 }

        PackageDataCleaner(rule.db, rule.clock).onPackageRemoved(slack.packageName, slack.userSerial)

        assertThat(history.launchDays.first { it[space]?.size == 1 }[space]).containsExactly(maps, 1)
    }

    @Test
    fun aHintIsRecordedPerSpaceAtTheCurrentTime() = runTest {
        val spaces = SpaceRepository(rule.db, backgroundScope, rule.clock)
        val history = history()
        val normal = normalId(spaces)
        assertThat(spaces.spaces.first { it.isNotEmpty() }.map { it.hintAt }).containsExactly(null, null)

        history.recordHint(normal)

        val after = spaces.spaces.first { s -> s.any { it.hintAt != null } }
        assertThat(after.associate { it.type to it.hintAt })
            .containsExactly(SpaceType.NORMAL, rule.now, SpaceType.WORK, null)
    }
}
