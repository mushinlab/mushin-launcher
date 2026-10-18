package com.mushinlab.launcher.core.data.repo

import androidx.room.withTransaction
import com.mushinlab.launcher.core.data.db.AppUsageEntity
import com.mushinlab.launcher.core.data.db.MushinDatabase
import com.mushinlab.launcher.core.data.db.SearchRecentEntity
import com.mushinlab.launcher.core.data.db.SpaceLaunchDayEntity
import com.mushinlab.launcher.core.data.db.SpaceUsageEntity
import com.mushinlab.launcher.core.data.db.toModel
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.AppUsage
import com.mushinlab.launcher.core.model.SearchRecent
import com.mushinlab.launcher.core.model.SpaceLimits
import com.mushinlab.launcher.core.model.SpaceUsage
import com.mushinlab.launcher.core.model.UsageDecay
import com.mushinlab.launcher.core.model.Usefulness
import java.time.Instant
import java.time.ZoneId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Launch history: global usage, usage per space, launch days per space, search recents per space and the time
 * of the last hint per space.
 *
 * Flows are collected eagerly in the given scope and start empty. [clock] returns epoch ms and [zone] the time zone
 * that defines local calendar days; both are injectable for tests.
 */
class HistoryRepository(
    private val db: MushinDatabase,
    scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    private val usageDao = db.appUsageDao()
    private val recentDao = db.searchRecentDao()
    private val spaceDao = db.spaceUsageDao()
    private val dayDao = db.spaceLaunchDayDao()

    val usage: StateFlow<ImmutableMap<AppKey, AppUsage>> = usageDao.observeAll()
        .map { list -> list.map { it.toModel() }.associateBy { it.key }.toImmutableMap() }
        .stateIn(scope, SharingStarted.Eagerly, persistentMapOf())

    val recents: StateFlow<ImmutableMap<Long, ImmutableList<SearchRecent>>> = recentDao.observeAll()
        .map { list ->
            list.groupBy { it.spaceId }
                .mapValues { (_, rows) ->
                    rows.sortedByDescending { it.lastLaunchedAt }.map { it.toModel() }.toImmutableList()
                }
                .toImmutableMap()
        }
        .stateIn(scope, SharingStarted.Eagerly, persistentMapOf())

    val spaceUsage: StateFlow<ImmutableMap<Long, ImmutableList<SpaceUsage>>> = spaceDao.observeAll()
        .map { list ->
            list.groupBy { it.spaceId }
                .mapValues { (_, rows) -> rows.map { it.toModel() }.toImmutableList() }
                .toImmutableMap()
        }
        .stateIn(scope, SharingStarted.Eagerly, persistentMapOf())

    /**
     * Number of distinct local days per app on which it was launched in a space, over the last
     * [Usefulness.WINDOW_DAYS] days including today (as of the latest change), keyed by space id.
     */
    val launchDays: StateFlow<ImmutableMap<Long, ImmutableMap<AppKey, Int>>> = dayDao.observeAll()
        .map { list ->
            val today = today()
            list.filter { it.day in today - Usefulness.WINDOW_DAYS + 1..today }
                .groupBy { it.spaceId }
                .mapValues { (_, rows) ->
                    rows.groupingBy { AppKey(it.packageName, it.activityName, it.userSerial) }
                        .eachCount()
                        .toImmutableMap()
                }
                .toImmutableMap()
        }
        .stateIn(scope, SharingStarted.Eagerly, persistentMapOf())

    /**
     * Records one launch in one transaction: global usage always; space usage and today's launch day when
     * [spaceId] is set, dropping launch days older than [Usefulness.DAYS_KEPT] days; and a search recent when the
     * launch came from search; recents are trimmed to the per-space limit.
     */
    suspend fun recordLaunch(key: AppKey, spaceId: Long?, fromSearch: Boolean) {
        val now = clock()
        db.withTransaction {
            val usage = usageDao.get(key.packageName, key.activityName, key.userSerial)
            usageDao.upsert(
                AppUsageEntity(
                    packageName = key.packageName,
                    activityName = key.activityName,
                    userSerial = key.userSerial,
                    launchCount = (usage?.launchCount ?: 0) + 1,
                    lastLaunchedAt = now,
                    decayedScore = if (usage == null) {
                        1.0
                    } else {
                        UsageDecay.increment(usage.decayedScore, usage.lastLaunchedAt, now)
                    },
                ),
            )
            if (spaceId != null) {
                val space = spaceDao.get(spaceId, key.packageName, key.activityName, key.userSerial)
                spaceDao.upsert(
                    SpaceUsageEntity(
                        spaceId = spaceId,
                        packageName = key.packageName,
                        activityName = key.activityName,
                        userSerial = key.userSerial,
                        launchCount = (space?.launchCount ?: 0) + 1,
                        lastLaunchedAt = now,
                        decayedScore = if (space == null) {
                            1.0
                        } else {
                            UsageDecay.increment(space.decayedScore, space.lastLaunchedAt, now)
                        },
                        dismissedAt = space?.dismissedAt,
                    ),
                )
                val today = dayOf(now)
                dayDao.insert(SpaceLaunchDayEntity(spaceId, key.packageName, key.activityName, key.userSerial, today))
                dayDao.deleteBefore(today - Usefulness.DAYS_KEPT)
            }
            if (fromSearch && spaceId != null) {
                val recent = recentDao.get(spaceId, key.packageName, key.activityName, key.userSerial)
                recentDao.upsert(
                    SearchRecentEntity(
                        spaceId = spaceId,
                        packageName = key.packageName,
                        activityName = key.activityName,
                        userSerial = key.userSerial,
                        lastLaunchedAt = now,
                        launchCount = (recent?.launchCount ?: 0) + 1,
                    ),
                )
                recentDao.trim(spaceId, SpaceLimits.RECENTS_PER_SPACE)
            }
        }
    }

    suspend fun clearRecents(spaceId: Long) {
        recentDao.clear(spaceId)
    }

    suspend fun clearAllRecents() {
        recentDao.clearAll()
    }

    /** Erases the launch history: launch counts, usage per space, recent apps of every space and launch days. */
    suspend fun clearUsage() {
        db.withTransaction {
            usageDao.deleteAll()
            spaceDao.deleteAll()
            dayDao.deleteAll()
            recentDao.clearAll()
        }
    }

    /** Remembers that the user rejected adding [key] to the home list of [spaceId]. */
    suspend fun dismissSuggestion(spaceId: Long, key: AppKey) {
        spaceDao.dismiss(spaceId, key.packageName, key.activityName, key.userSerial, clock())
    }

    /** Remembers that a hint in [spaceId] was accepted or dismissed now; see [Usefulness.suggest]. */
    suspend fun recordHint(spaceId: Long) {
        db.spaceDao().setHintAt(spaceId, clock())
    }

    private fun today(): Long = dayOf(clock())

    private fun dayOf(epochMs: Long): Long = Instant.ofEpochMilli(epochMs).atZone(zone()).toLocalDate().toEpochDay()
}
