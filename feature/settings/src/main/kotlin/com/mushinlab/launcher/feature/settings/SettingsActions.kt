package com.mushinlab.launcher.feature.settings

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.BackgroundCrop
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.search.GestureSample

@Immutable
class SettingsActions(
    val preferences: PreferenceActions,
    val spaces: SpaceActions,
    val apps: AppActions,
    val system: SystemActions,
    val memory: MemoryActions,
    val background: BackgroundActions,
)

interface PreferenceActions {
    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings)

    fun setGesture(direction: SwipeDirection, action: GestureAction)
}

interface SpaceActions {
    val folders: FolderActions

    suspend fun createSpace(name: String, type: SpaceType): String?

    suspend fun renameSpace(id: Long, name: String): String?

    fun deleteSpace(id: Long)

    fun switchSpace(id: Long)

    fun setPalette(id: Long, preset: PalettePreset)

    fun setColoredIcons(id: Long, enabled: Boolean)

    fun setSpaceItems(id: Long, keys: List<AppKey>)

    fun addSpaceItem(id: Long, key: AppKey)

    fun removeSpaceItem(id: Long, key: AppKey)

    fun replaceSpaceItem(id: Long, old: AppKey, new: AppKey)
}

interface FolderActions {
    fun rename(id: Long, name: String)

    fun addApp(id: Long, key: AppKey)

    fun removeApp(id: Long, key: AppKey)
}

interface AppActions {
    fun setHidden(key: AppKey, hidden: Boolean)

    fun rename(key: AppKey, label: String?)
}

interface SystemActions {
    fun requestDefaultLauncher()

    fun openNotificationAccess()

    fun setAppLanguage(tag: String?)
}

interface BackgroundActions {
    fun pick(spaceId: Long)

    fun usePreset(spaceId: Long, preset: BackgroundPreset)

    fun edit(spaceId: Long)

    fun close(spaceId: Long)

    suspend fun thumbnail(spaceId: Long): ImageBitmap?

    fun save(edit: BackgroundEdit)

    fun remove(spaceId: Long)
}

@Immutable
data class BackgroundEdit(
    val spaceId: Long,
    val crop: BackgroundCrop,
    val frameWidth: Int,
    val frameHeight: Int,
    val brightness: Float,
    val blur: Float,
    val rowAlpha: Float,
)

interface MemoryActions {
    fun forgetDrawing(key: AppKey, path: String)

    fun addDrawing(key: AppKey, sample: GestureSample)

    fun forgetApp(key: AppKey)

    fun forgetAllGestures()

    fun clearUsage()
}
