package com.bluecubelabs.env.feature.settings

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.EnvType
import com.bluecubelabs.env.core.model.GestureAction
import com.bluecubelabs.env.core.model.LauncherSettings
import com.bluecubelabs.env.core.model.PalettePreset
import com.bluecubelabs.env.core.model.SwipeDirection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

sealed interface SettingsRoute {
    data object Root : SettingsRoute

    data object Environments : SettingsRoute

    data class EnvEditor(val envId: Long) : SettingsRoute

    data class PickApps(val envId: Long) : SettingsRoute

    data class PickGestureApp(val direction: SwipeDirection) : SettingsRoute

    data object Home : SettingsRoute

    data object Style : SettingsRoute

    data object Advanced : SettingsRoute

    data object Keyboard : SettingsRoute

    data object Gestures : SettingsRoute

    data object Press : SettingsRoute

    data object Apps : SettingsRoute

    data object System : SettingsRoute

    data object Memory : SettingsRoute

    data class MemoryApp(val key: AppKey) : SettingsRoute
}

@Stable
class SettingsStack {
    private val routes = mutableStateListOf<SettingsRoute>()

    val top: SettingsRoute? get() = routes.lastOrNull()
    val isOpen: Boolean get() = routes.isNotEmpty()

    fun open(root: SettingsRoute = SettingsRoute.Root) {
        routes.clear()
        routes += root
    }

    fun push(route: SettingsRoute) {
        routes += route
    }

    fun pop(): Boolean {
        if (routes.isEmpty()) return false
        routes.removeAt(routes.lastIndex)
        return true
    }

    fun close() {
        routes.clear()
    }

    fun size(): Int = routes.size
}

@Immutable
data class EnvSummary(
    val id: Long,
    val name: String,
    val type: EnvType,
    val preset: PalettePreset,
    val current: Boolean,
    val coloredIcons: Boolean = false,
)

@Immutable
data class EditorItem(
    val key: AppKey,
    val label: String,
    val hidden: Boolean,
    val missing: Boolean,
)

@Immutable
data class PickerApp(
    val key: AppKey,
    val label: String,
    val hidden: Boolean,
)

@Immutable
data class RenamedApp(
    val key: AppKey,
    val original: String,
    val custom: String,
)

@Immutable
data class GestureSummary(
    val action: GestureAction,
    val appLabel: String?,
    val appMissing: Boolean,
)

@Immutable
data class LearnedDrawing(val path: String, val weight: Double)

@Immutable
data class LearnedApp(
    val key: AppKey,
    val label: String,
    val drawings: ImmutableList<LearnedDrawing>,
)

@Immutable
data class UsageRow(val key: AppKey, val label: String, val launches: Int)

@Immutable
data class ScoreRow(val key: AppKey, val label: String, val score: Int)

@Immutable
data class SettingsUiState(
    val settings: LauncherSettings = LauncherSettings(),
    val envs: ImmutableList<EnvSummary> = persistentListOf(),
    val envItems: ImmutableMap<Long, ImmutableList<EditorItem>> = persistentMapOf(),
    val apps: ImmutableList<PickerApp> = persistentListOf(),
    val renamed: ImmutableList<RenamedApp> = persistentListOf(),
    val gestures: ImmutableMap<SwipeDirection, GestureSummary> = persistentMapOf(),
    val capacity: Int = 0,
    val isDefaultLauncher: Boolean = false,
    val notificationAccess: Boolean = false,
    val versionName: String = "",
    val learned: ImmutableList<LearnedApp> = persistentListOf(),
    val usage: ImmutableList<UsageRow> = persistentListOf(),
    val usefulness: ImmutableList<ScoreRow> = persistentListOf(),
) {
    val currentEnvId: Long? get() = envs.firstOrNull { it.current }?.id
}

interface SettingsActions {
    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings)

    suspend fun createEnv(name: String, type: EnvType): String?

    suspend fun renameEnv(id: Long, name: String): String?

    fun deleteEnv(id: Long)

    fun setPalette(id: Long, preset: PalettePreset)

    fun setColoredIcons(id: Long, enabled: Boolean)

    fun setEnvItems(id: Long, keys: List<AppKey>)

    fun addEnvItem(id: Long, key: AppKey)

    fun removeEnvItem(id: Long, key: AppKey)

    fun setGesture(direction: SwipeDirection, action: GestureAction)

    fun setHidden(key: AppKey, hidden: Boolean)

    fun rename(key: AppKey, label: String?)

    fun switchEnv(id: Long)

    fun requestDefaultLauncher()

    fun openNotificationAccess()

    fun forgetDrawing(key: AppKey, path: String)

    fun forgetApp(key: AppKey)

    fun forgetAllGestures()

    fun clearUsage()
}
