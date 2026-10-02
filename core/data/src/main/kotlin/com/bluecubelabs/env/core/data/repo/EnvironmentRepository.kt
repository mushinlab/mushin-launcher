package com.bluecubelabs.env.core.data.repo

import androidx.room.withTransaction
import com.bluecubelabs.env.core.data.db.EnvDatabase
import com.bluecubelabs.env.core.data.db.EnvironmentEntity
import com.bluecubelabs.env.core.data.db.EnvironmentItemEntity
import com.bluecubelabs.env.core.data.db.key
import com.bluecubelabs.env.core.data.db.toModel
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.EnvLimits
import com.bluecubelabs.env.core.model.EnvType
import com.bluecubelabs.env.core.model.Environment
import com.bluecubelabs.env.core.model.PalettePreset
import com.bluecubelabs.env.core.model.Palettes
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

sealed interface EnvError {
    data object NameEmpty : EnvError

    data object NameTooLong : EnvError

    data object NameTaken : EnvError

    data object WorkLimitReached : EnvError

    data object CustomLimitReached : EnvError

    data object NormalIsUnique : EnvError

    data object NormalNotDeletable : EnvError

    data object NotFound : EnvError

    data class ItemLimitReached(val limit: Int) : EnvError
}

sealed interface EnvOutcome<out T> {
    data class Ok<T>(val value: T) : EnvOutcome<T>

    data class Failed(val error: EnvError) : EnvOutcome<Nothing>
}

class EnvironmentRepository(
    private val db: EnvDatabase,
    scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val envDao = db.environmentDao()
    private val itemDao = db.environmentItemDao()

    val environments: StateFlow<ImmutableList<Environment>> = envDao.observeAll()
        .map { list -> list.map { it.toModel() }.toImmutableList() }
        .stateIn(scope, SharingStarted.Eagerly, persistentListOf())

    val items: StateFlow<ImmutableMap<Long, ImmutableList<AppKey>>> = itemDao.observeAll()
        .map { list ->
            list.groupBy { it.envId }
                .mapValues { (_, rows) -> rows.sortedBy { it.position }.map { it.key() }.toImmutableList() }
                .toImmutableMap()
        }
        .stateIn(scope, SharingStarted.Eagerly, persistentMapOf())

    suspend fun create(name: String, type: EnvType, preset: PalettePreset): EnvOutcome<Long> = db.withTransaction {
        val all = envDao.getAll()
        val limitError = when (type) {
            EnvType.NORMAL -> EnvError.NormalIsUnique

            EnvType.WORK -> EnvError.WorkLimitReached.takeIf {
                all.count { it.type == EnvType.WORK.name } >=
                    EnvLimits.MAX_WORK
            }

            EnvType.CUSTOM -> EnvError.CustomLimitReached.takeIf {
                all.count { it.type == EnvType.CUSTOM.name } >= EnvLimits.MAX_CUSTOM
            }
        }
        if (limitError != null) return@withTransaction EnvOutcome.Failed(limitError)
        when (val validated = validateName(name, all, excludeId = null)) {
            is EnvOutcome.Failed -> validated

            is EnvOutcome.Ok -> {
                val palette = Palettes.of(preset)
                val id = envDao.insert(
                    EnvironmentEntity(
                        name = validated.value,
                        type = type.name,
                        sortOrder = envDao.maxSortOrder() + 1,
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
                EnvOutcome.Ok(id)
            }
        }
    }

    suspend fun rename(id: Long, name: String): EnvOutcome<Unit> = db.withTransaction {
        val all = envDao.getAll()
        if (all.none { it.id == id }) return@withTransaction EnvOutcome.Failed(EnvError.NotFound)
        when (val validated = validateName(name, all, excludeId = id)) {
            is EnvOutcome.Failed -> validated

            is EnvOutcome.Ok -> {
                envDao.rename(id, validated.value)
                EnvOutcome.Ok(Unit)
            }
        }
    }

    suspend fun delete(id: Long): EnvOutcome<Unit> = db.withTransaction {
        val env = envDao.get(id) ?: return@withTransaction EnvOutcome.Failed(EnvError.NotFound)
        if (env.type == EnvType.NORMAL.name) return@withTransaction EnvOutcome.Failed(EnvError.NormalNotDeletable)
        envDao.delete(id)
        EnvOutcome.Ok(Unit)
    }

    suspend fun setPalette(id: Long, preset: PalettePreset) {
        val p = Palettes.of(preset)
        envDao.setPalette(
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

    suspend fun itemsOf(envId: Long): List<AppKey> = itemDao.get(envId).map { it.key() }

    suspend fun setItems(envId: Long, keys: List<AppKey>) {
        db.withTransaction {
            itemDao.deleteForEnv(envId)
            itemDao.insertAll(
                keys.distinct().mapIndexed { index, key ->
                    EnvironmentItemEntity(envId, key.packageName, key.activityName, key.userSerial, index)
                },
            )
        }
    }

    suspend fun addItems(envId: Long, keys: List<AppKey>, limit: Int): EnvOutcome<Unit> = db.withTransaction {
        val current = itemDao.get(envId).map { it.key() }
        val merged = (current + keys).distinct()
        if (merged.size > limit) return@withTransaction EnvOutcome.Failed(EnvError.ItemLimitReached(limit))
        setItems(envId, merged)
        EnvOutcome.Ok(Unit)
    }

    suspend fun removeItem(envId: Long, key: AppKey) {
        db.withTransaction {
            setItems(envId, itemDao.get(envId).map { it.key() } - key)
        }
    }

    private fun validateName(raw: String, all: List<EnvironmentEntity>, excludeId: Long?): EnvOutcome<String> {
        val name = raw.trim()
        return when {
            name.isEmpty() -> EnvOutcome.Failed(EnvError.NameEmpty)

            name.length > EnvLimits.NAME_MAX_LENGTH -> EnvOutcome.Failed(EnvError.NameTooLong)

            all.any {
                it.id != excludeId && it.name.foldCase() == name.foldCase()
            } -> EnvOutcome.Failed(EnvError.NameTaken)

            else -> EnvOutcome.Ok(name)
        }
    }

    private fun String.foldCase(): String = lowercase(Locale.ROOT)
}

fun resolveCurrentEnvironment(environments: List<Environment>, currentEnvId: Long?): Environment? =
    environments.firstOrNull { it.id == currentEnvId } ?: environments.firstOrNull { it.type == EnvType.NORMAL }
