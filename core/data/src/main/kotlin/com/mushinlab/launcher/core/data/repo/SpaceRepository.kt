package com.mushinlab.launcher.core.data.repo

import androidx.room.withTransaction
import com.mushinlab.launcher.core.data.db.MushinDatabase
import com.mushinlab.launcher.core.data.db.SpaceEntity
import com.mushinlab.launcher.core.data.db.SpaceItemEntity
import com.mushinlab.launcher.core.data.db.key
import com.mushinlab.launcher.core.data.db.toModel
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.HomeListOffset
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.Palettes
import com.mushinlab.launcher.core.model.Space
import com.mushinlab.launcher.core.model.SpaceLimits
import com.mushinlab.launcher.core.model.SpaceType
import java.util.Locale
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Reasons a space operation was rejected; mapped to messages by the interface. */
sealed interface SpaceError {
    data object NameEmpty : SpaceError

    data object NameTooLong : SpaceError

    data object NameTaken : SpaceError

    data object WorkLimitReached : SpaceError

    data object CustomLimitReached : SpaceError

    data object NormalIsUnique : SpaceError

    data object NormalNotDeletable : SpaceError

    data object NotFound : SpaceError

    data class ItemLimitReached(val limit: Int) : SpaceError
}

/** Result of a validated space operation. */
sealed interface SpaceOutcome<out T> {
    data class Ok<T>(val value: T) : SpaceOutcome<T>

    data class Failed(val error: SpaceError) : SpaceOutcome<Nothing>
}

/**
 * Spaces and their home lists.
 *
 * Changes are validated inside a transaction: names are trimmed, 1..[SpaceLimits.NAME_MAX_LENGTH] characters
 * and unique ignoring case; type limits come from [SpaceLimits]; the normal space cannot be created or deleted.
 * Flows are collected eagerly in the given scope and start empty.
 */
class SpaceRepository(
    private val db: MushinDatabase,
    scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val spaceDao = db.spaceDao()
    private val itemDao = db.spaceItemDao()

    val spaces: StateFlow<ImmutableList<Space>> = spaceDao.observeAll()
        .map { list -> list.map { it.toModel() }.toImmutableList() }
        .stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    val items: StateFlow<ImmutableMap<Long, ImmutableList<AppKey>>> = itemDao.observeAll()
        .map { list ->
            list.groupBy { it.spaceId }
                .mapValues { (_, rows) -> rows.sortedBy { it.position }.map { it.key() }.toImmutableList() }
                .toImmutableMap()
        }
        .stateIn(scope, SharingStarted.Eagerly, persistentMapOf())

    /** Creates a space at the end of the order with the colors of [preset]; returns its id. */
    suspend fun create(name: String, type: SpaceType, preset: PalettePreset): SpaceOutcome<Long> = db.withTransaction {
        val all = spaceDao.getAll()
        val limitError = when (type) {
            SpaceType.NORMAL -> SpaceError.NormalIsUnique

            SpaceType.WORK -> SpaceError.WorkLimitReached.takeIf {
                all.count { it.type == SpaceType.WORK.name } >=
                    SpaceLimits.MAX_WORK
            }

            SpaceType.CUSTOM -> SpaceError.CustomLimitReached.takeIf {
                all.count { it.type == SpaceType.CUSTOM.name } >= SpaceLimits.MAX_CUSTOM
            }
        }
        if (limitError != null) return@withTransaction SpaceOutcome.Failed(limitError)
        when (val validated = validateSpaceName(name, all, excludeId = null)) {
            is SpaceOutcome.Failed -> validated

            is SpaceOutcome.Ok -> {
                val palette = Palettes.of(preset)
                val id = spaceDao.insert(
                    SpaceEntity(
                        name = validated.value,
                        type = type.name,
                        sortOrder = spaceDao.maxSortOrder() + 1,
                        palettePreset = preset.name,
                        background = palette.background,
                        zebraA = palette.zebraA,
                        zebraB = palette.zebraB,
                        textPrimary = palette.textPrimary,
                        textSecondary = palette.textSecondary,
                        accent = palette.accent,
                        iconTint = palette.iconTint,
                        divider = palette.divider,
                        surface = palette.surface,
                        keyBackground = palette.keyBackground,
                        keyPressed = palette.keyPressed,
                        isDark = palette.isDark,
                        createdAt = clock(),
                    ),
                )
                SpaceOutcome.Ok(id)
            }
        }
    }

    suspend fun rename(id: Long, name: String): SpaceOutcome<Unit> = db.withTransaction {
        val all = spaceDao.getAll()
        if (all.none { it.id == id }) return@withTransaction SpaceOutcome.Failed(SpaceError.NotFound)
        when (val validated = validateSpaceName(name, all, excludeId = id)) {
            is SpaceOutcome.Failed -> validated

            is SpaceOutcome.Ok -> {
                spaceDao.rename(id, validated.value)
                SpaceOutcome.Ok(Unit)
            }
        }
    }

    /** Deletes a space with its home list, recents and usage; the normal space cannot be deleted. */
    suspend fun delete(id: Long): SpaceOutcome<Unit> = db.withTransaction {
        val space = spaceDao.get(id) ?: return@withTransaction SpaceOutcome.Failed(SpaceError.NotFound)
        if (space.type == SpaceType.NORMAL.name) {
            return@withTransaction SpaceOutcome.Failed(SpaceError.NormalNotDeletable)
        }
        spaceDao.delete(id)
        SpaceOutcome.Ok(Unit)
    }

    suspend fun setColoredIcons(id: Long, enabled: Boolean) {
        spaceDao.setColoredIcons(id, enabled)
    }

    /**
     * Sets the home list offset of a space in dp, clamped to 0..[HomeListOffset.MAX_DP]; null falls back to the
     * global offset.
     */
    suspend fun setListOffset(id: Long, dp: Int?) {
        spaceDao.setListOffset(id, dp?.coerceIn(0, HomeListOffset.MAX_DP))
    }

    suspend fun setPalette(id: Long, preset: PalettePreset) {
        val p = Palettes.of(preset)
        spaceDao.setPalette(
            id = id,
            preset = preset.name,
            background = p.background,
            zebraA = p.zebraA,
            zebraB = p.zebraB,
            textPrimary = p.textPrimary,
            textSecondary = p.textSecondary,
            accent = p.accent,
            iconTint = p.iconTint,
            divider = p.divider,
            surface = p.surface,
            keyBackground = p.keyBackground,
            keyPressed = p.keyPressed,
            isDark = p.isDark,
        )
    }

    suspend fun itemsOf(spaceId: Long): List<AppKey> = itemDao.get(spaceId).map { it.key() }

    /** Replaces the home list of a space; duplicates are dropped and order is kept. */
    suspend fun setItems(spaceId: Long, keys: List<AppKey>) {
        db.withTransaction {
            itemDao.deleteForSpace(spaceId)
            itemDao.insertAll(
                keys.distinct().mapIndexed { index, key ->
                    SpaceItemEntity(spaceId, key.packageName, key.activityName, key.userSerial, index)
                },
            )
        }
    }

    /** Appends [keys] not already present; fails without changes when the list would exceed [limit]. */
    suspend fun addItems(spaceId: Long, keys: List<AppKey>, limit: Int): SpaceOutcome<Unit> = db.withTransaction {
        val current = itemDao.get(spaceId).map { it.key() }
        val merged = (current + keys).distinct()
        if (merged.size > limit) return@withTransaction SpaceOutcome.Failed(SpaceError.ItemLimitReached(limit))
        setItems(spaceId, merged)
        SpaceOutcome.Ok(Unit)
    }

    suspend fun removeItem(spaceId: Long, key: AppKey) {
        db.withTransaction {
            setItems(spaceId, itemDao.get(spaceId).map { it.key() } - key)
        }
    }
}

/** The space with [currentSpaceId], or the normal space when it is unset or no longer exists. */
fun resolveCurrentSpace(spaces: List<Space>, currentSpaceId: Long?): Space? =
    spaces.firstOrNull { it.id == currentSpaceId } ?: spaces.firstOrNull { it.type == SpaceType.NORMAL }

private fun validateSpaceName(raw: String, all: List<SpaceEntity>, excludeId: Long?): SpaceOutcome<String> {
    val name = raw.trim()
    return when {
        name.isEmpty() -> SpaceOutcome.Failed(SpaceError.NameEmpty)

        name.length > SpaceLimits.NAME_MAX_LENGTH -> SpaceOutcome.Failed(SpaceError.NameTooLong)

        all.any {
            it.id != excludeId && it.name.foldCase() == name.foldCase()
        } -> SpaceOutcome.Failed(SpaceError.NameTaken)

        else -> SpaceOutcome.Ok(name)
    }
}

private fun String.foldCase(): String = lowercase(Locale.ROOT)
