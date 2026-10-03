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

    data object Search : SettingsRoute

    data object Keyboard : SettingsRoute

    data object Gestures : SettingsRoute

    data class GestureAction(val direction: SwipeDirection) : SettingsRoute

    data object Apps : SettingsRoute

    data object System : SettingsRoute
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
data class SettingsUiState(
    val settings: LauncherSettings = LauncherSettings(),
    val envs: ImmutableList<EnvSummary> = persistentListOf(),
    val envItems: ImmutableMap<Long, ImmutableList<EditorItem>> = persistentMapOf(),
    val apps: ImmutableList<PickerApp> = persistentListOf(),
    val renamed: ImmutableList<RenamedApp> = persistentListOf(),
    val gestures: ImmutableMap<SwipeDirection, GestureSummary> = persistentMapOf(),
    val capacity: Int = 0,
    val isDefaultLauncher: Boolean = false,
    val versionName: String = "",
) {
    val currentEnvId: Long? get() = envs.firstOrNull { it.current }?.id
}

interface SettingsActions {
    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings)

    suspend fun createEnv(name: String, type: EnvType): String?

    suspend fun renameEnv(id: Long, name: String): String?

    fun deleteEnv(id: Long)

    fun setPalette(id: Long, preset: PalettePreset)

    fun setEnvItems(id: Long, keys: List<AppKey>)

    fun setGesture(direction: SwipeDirection, action: GestureAction)

    fun setHidden(key: AppKey, hidden: Boolean)

    fun rename(key: AppKey, label: String?)

    fun clearHistory(envId: Long)

    fun requestDefaultLauncher()
}
