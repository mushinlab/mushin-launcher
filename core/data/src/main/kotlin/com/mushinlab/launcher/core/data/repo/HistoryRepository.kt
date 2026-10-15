package com.mushinlab.launcher.core.data.repo

import androidx.room.withTransaction
import com.mushinlab.launcher.core.data.db.AppUsageEntity
import com.mushinlab.launcher.core.data.db.EnvDatabase
import com.mushinlab.launcher.core.data.db.SearchRecentEntity
import com.mushinlab.launcher.core.data.db.SpaceUsageEntity
import com.mushinlab.launcher.core.data.db.toModel
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.AppUsage
import com.mushinlab.launcher.core.model.EnvLimits
import com.mushinlab.launcher.core.model.SearchRecent
import com.mushinlab.launcher.core.model.SpaceUsage
import com.mushinlab.launcher.core.model.UsageDecay
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

class HistoryRepository(
    private val db: EnvDatabase,
    scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val usageDao = db.appUsageDao()
    private val recentDao = db.searchRecentDao()
    private val spaceDao = db.spaceUsageDao()

    val usage: StateFlow<ImmutableMap<AppKey, AppUsage>> = usageDao.observeAll()
        .map { list -> list.map { it.toModel() }.associateBy { it.key }.toImmutableMap() }
        .stateIn(scope, SharingStarted.Eagerly, persistentMapOf())

    val recents: StateFlow<ImmutableMap<Long, ImmutableList<SearchRecent>>> = recentDao.observeAll()
        .map { list ->
            list.groupBy { it.envId }
                .mapValues { (_, rows) ->
                    rows.sortedByDescending { it.lastLaunchedAt }.map { it.toModel() }.toImmutableList()
                }
                .toImmutableMap()
        }
        .stateIn(scope, SharingStarted.Eagerly, persistentMapOf())

    val spaceUsage: StateFlow<ImmutableMap<Long, ImmutableList<SpaceUsage>>> = spaceDao.observeAll()
        .map { list ->
            list.groupBy { it.envId }
                .mapValues { (_, rows) -> rows.map { it.toModel() }.toImmutableList() }
                .toImmutableMap()
        }
        .stateIn(scope, SharingStarted.Eagerly, persistentMapOf())

    suspend fun recordLaunch(key: AppKey, envId: Long?, fromSearch: Boolean) {
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
            if (envId != null) {
                val space = spaceDao.get(envId, key.packageName, key.activityName, key.userSerial)
                spaceDao.upsert(
                    SpaceUsageEntity(
                        envId = envId,
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
            }
            if (fromSearch && envId != null) {
                val recent = recentDao.get(envId, key.packageName, key.activityName, key.userSerial)
                recentDao.upsert(
                    SearchRecentEntity(
                        envId = envId,
                        packageName = key.packageName,
                        activityName = key.activityName,
                        userSerial = key.userSerial,
                        lastLaunchedAt = now,
                        launchCount = (recent?.launchCount ?: 0) + 1,
                    ),
                )
                recentDao.trim(envId, EnvLimits.RECENTS_PER_ENV)
            }
        }
    }

    suspend fun clearRecents(envId: Long) {
        recentDao.clear(envId)
    }

    suspend fun clearAllRecents() {
        recentDao.clearAll()
    }

    suspend fun clearUsage() {
        db.withTransaction {
            usageDao.deleteAll()
            spaceDao.deleteAll()
        }
    }

    suspend fun dismissSuggestion(envId: Long, key: AppKey) {
        spaceDao.dismiss(envId, key.packageName, key.activityName, key.userSerial, clock())
    }
}
