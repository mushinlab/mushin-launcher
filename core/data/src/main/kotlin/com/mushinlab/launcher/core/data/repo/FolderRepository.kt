package com.mushinlab.launcher.core.data.repo

import androidx.room.withTransaction
import com.mushinlab.launcher.core.data.db.FolderEntity
import com.mushinlab.launcher.core.data.db.FolderItemEntity
import com.mushinlab.launcher.core.data.db.MushinDatabase
import com.mushinlab.launcher.core.data.db.SpaceItemEntity
import com.mushinlab.launcher.core.data.db.key
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.Folder
import com.mushinlab.launcher.core.model.FolderKeys
import com.mushinlab.launcher.core.model.OverrideLimits
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Home folders (ADR-0035): a folder is a row of `folder`, its apps are rows of `folder_item`, and its place in the
 * home list of its space is the [FolderKeys] item in `environment_item`. Deleting a space deletes its folders.
 * [folders] is collected eagerly in the given scope and starts empty.
 */
class FolderRepository(private val db: MushinDatabase, scope: CoroutineScope) {
    private val dao = db.spaceItemDao()

    val folders: StateFlow<ImmutableMap<Long, Folder>> =
        combine(dao.observeFolders(), dao.observeFolderItems()) { folders, items ->
            val apps = items.groupBy({ it.folderId }, { it.key() })
            folders.associate { it.id to Folder(it.id, it.spaceId, it.name, apps[it.id].orEmpty()) }.toImmutableMap()
        }.stateIn(scope, SharingStarted.Eagerly, persistentMapOf())

    /**
     * Creates an unnamed empty folder at the end of the home list of [spaceId]; fails without changes when the list
     * already has [limit] rows.
     */
    suspend fun create(spaceId: Long, limit: Int): SpaceOutcome<Long> = db.withTransaction {
        val items = dao.get(spaceId)
        if (items.size >= limit) return@withTransaction SpaceOutcome.Failed(SpaceError.ItemLimitReached(limit))
        val id = dao.insertFolder(FolderEntity(spaceId = spaceId, name = ""))
        val key = FolderKeys.of(id)
        val position = (items.maxOfOrNull { it.position } ?: -1) + 1
        dao.insertAll(listOf(SpaceItemEntity(spaceId, key.packageName, key.activityName, key.userSerial, position)))
        SpaceOutcome.Ok(id)
    }

    /** Renames a folder; the name is cut to [OverrideLimits.LABEL_MAX_LENGTH] characters. */
    suspend fun rename(id: Long, name: String) {
        dao.renameFolder(id, name.take(OverrideLimits.LABEL_MAX_LENGTH))
    }

    /** Replaces the apps of a folder; duplicates are dropped and order is kept. */
    suspend fun setApps(id: Long, keys: List<AppKey>) {
        db.withTransaction {
            dao.deleteFolderItems(id)
            dao.insertFolderItems(
                keys.distinct().mapIndexed { index, key ->
                    FolderItemEntity(id, key.packageName, key.activityName, key.userSerial, index)
                },
            )
        }
    }

    /** Appends [key] to the folder unless it is already there. */
    suspend fun addApp(id: Long, key: AppKey) {
        db.withTransaction { setApps(id, dao.folderItems(id).map { it.key() } + key) }
    }

    suspend fun removeApp(id: Long, key: AppKey) {
        db.withTransaction { setApps(id, dao.folderItems(id).map { it.key() } - key) }
    }

    /** Deletes a folder with its apps and its home-list row. */
    suspend fun delete(id: Long) {
        val key = FolderKeys.of(id)
        db.withTransaction {
            db.appKeyDao().deleteSpaceItems(key.packageName, key.userSerial, key.activityName)
            dao.deleteFolder(id)
        }
    }

    /** Deletes a folder that has neither a name nor apps; returns whether it was deleted. */
    suspend fun deleteIfEmpty(id: Long): Boolean = db.withTransaction {
        val folder = dao.getFolder(id)
        val empty = folder != null && folder.name.isBlank() && dao.folderItems(id).isEmpty()
        if (empty) delete(id)
        empty
    }
}
