package com.mushinlab.launcher.feature.settings

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.ShapeVertex
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.model.SwipeDirection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

sealed interface SettingsRoute {
    data object Root : SettingsRoute

    data object Spaces : SettingsRoute

    data class SpaceEditor(val spaceId: Long) : SettingsRoute

    data class PickApps(val spaceId: Long) : SettingsRoute

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

    data class MemoryApp(val key: AppKey, val sketch: Boolean = false) : SettingsRoute

    data object MemoryPickApp : SettingsRoute
}

@Stable
class SettingsStack {
    private val stack = mutableStateListOf<SettingsRoute>()
    var sheet: (() -> Unit)? = null
    var draft: List<ShapeVertex>? = null

    val top: SettingsRoute? get() = stack.lastOrNull()
    val isOpen: Boolean get() = stack.isNotEmpty()
    val routes: List<SettingsRoute> get() = stack.toList()

    fun open(root: SettingsRoute = SettingsRoute.Root) {
        stack.clear()
        stack += root
    }

    fun push(route: SettingsRoute) {
        stack += route
    }

    fun pop(): Boolean {
        val closeSheet = sheet
        return when {
            closeSheet != null -> {
                closeSheet()
                true
            }

            stack.isEmpty() -> false

            else -> {
                stack.removeAt(stack.lastIndex)
                true
            }
        }
    }

    fun close() {
        stack.clear()
    }

    fun restore(saved: List<SettingsRoute>) {
        stack.clear()
        stack += saved
    }

    fun size(): Int = stack.size
}

@Immutable
data class SpaceSummary(
    val id: Long,
    val name: String,
    val type: SpaceType,
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
data class LearnedDrawing(val path: String, val weight: Double, val shape: List<ShapeVertex> = emptyList())

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
    val spaces: ImmutableList<SpaceSummary> = persistentListOf(),
    val spaceItems: ImmutableMap<Long, ImmutableList<EditorItem>> = persistentMapOf(),
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
    val hasBackground: Boolean = false,
) {
    val currentSpaceId: Long? get() = spaces.firstOrNull { it.current }?.id
}
