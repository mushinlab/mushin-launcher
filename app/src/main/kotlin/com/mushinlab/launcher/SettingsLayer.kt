package com.mushinlab.launcher

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.search.GestureSample
import com.mushinlab.launcher.feature.settings.AppActions
import com.mushinlab.launcher.feature.settings.BackgroundActions
import com.mushinlab.launcher.feature.settings.BackgroundEdit
import com.mushinlab.launcher.feature.settings.BackgroundPreset
import com.mushinlab.launcher.feature.settings.BackgroundSource
import com.mushinlab.launcher.feature.settings.FolderActions
import com.mushinlab.launcher.feature.settings.MemoryActions
import com.mushinlab.launcher.feature.settings.PreferenceActions
import com.mushinlab.launcher.feature.settings.SettingsActions
import com.mushinlab.launcher.feature.settings.SettingsHost
import com.mushinlab.launcher.feature.settings.SpaceActions
import com.mushinlab.launcher.feature.settings.SystemActions

@Composable
internal fun SettingsLayer(vm: LauncherViewModel) {
    val state by vm.settingsState.collectAsStateWithLifecycle()
    val actions = remember(vm) {
        SettingsActions(
            preferences = preferenceActions(vm),
            spaces = spaceActions(vm),
            apps = appActions(vm),
            system = systemActions(vm),
            memory = memoryActions(vm),
            background = backgroundActions(vm),
        )
    }
    val editing by vm.backgrounds.source.collectAsStateWithLifecycle()
    val source = remember(editing) { editing?.let { BackgroundSource(it.spaceId, it.bitmap.asImageBitmap()) } }
    val configuration = LocalConfiguration.current
    val language = remember(configuration) { vm.appLanguage() }
    SettingsHost(vm.navigator.settings, state.copy(backgroundSource = source, appLanguage = language), actions)
}

private fun preferenceActions(vm: LauncherViewModel) = object : PreferenceActions {
    override fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) = vm.preferences.update(transform)

    override fun setGesture(direction: SwipeDirection, action: GestureAction) = vm.preferences.setGesture(
        direction,
        action,
    )
}

private fun spaceActions(vm: LauncherViewModel) = object : SpaceActions {
    override suspend fun createSpace(name: String, type: SpaceType): String? = vm.spaces.create(name, type)

    override suspend fun renameSpace(id: Long, name: String): String? = vm.spaces.rename(id, name)

    override fun deleteSpace(id: Long) = vm.spaces.delete(id)

    override fun switchSpace(id: Long) = vm.spaces.switchTo(id)

    override fun setPalette(id: Long, preset: PalettePreset) = vm.spaces.setPalette(id, preset)

    override fun setColoredIcons(id: Long, enabled: Boolean) = vm.spaces.setColoredIcons(id, enabled)

    override fun setSpaceItems(id: Long, keys: List<AppKey>) = vm.spaces.setItems(id, keys)

    override fun addSpaceItem(id: Long, key: AppKey) {
        vm.spaces.addItem(id, key)
    }

    override fun removeSpaceItem(id: Long, key: AppKey) = vm.spaces.removeItem(id, key)

    override fun replaceSpaceItem(id: Long, old: AppKey, new: AppKey) = vm.spaces.setItems(
        id,
        vm.settingsState.value.spaceItems[id].orEmpty().map { if (it.key == old) new else it.key },
    )

    override val folders = folderActions(vm)
}

private fun folderActions(vm: LauncherViewModel) = object : FolderActions {
    override fun rename(id: Long, name: String) = vm.folders.rename(id, name)

    override fun addApp(id: Long, key: AppKey) = vm.folders.addApp(id, key)

    override fun removeApp(id: Long, key: AppKey) = vm.folders.removeApp(id, key)
}

private fun appActions(vm: LauncherViewModel) = object : AppActions {
    override fun setHidden(key: AppKey, hidden: Boolean) = vm.apps.setHidden(key, hidden)

    override fun rename(key: AppKey, label: String?) = vm.apps.rename(key, label)
}

private fun systemActions(vm: LauncherViewModel) = object : SystemActions {
    override fun requestDefaultLauncher() = vm.request(LauncherEffect.RequestHomeRole)

    override fun openNotificationAccess() = vm.request(LauncherEffect.OpenNotificationAccess)

    override fun setAppLanguage(tag: String?) = vm.setAppLanguage(tag)
}

private fun backgroundActions(vm: LauncherViewModel) = object : BackgroundActions {
    override fun pick(spaceId: Long) = vm.backgrounds.pick(spaceId)

    override fun usePreset(spaceId: Long, preset: BackgroundPreset) = vm.backgrounds.usePreset(spaceId, preset)

    override fun edit(spaceId: Long) = vm.backgrounds.edit(spaceId)

    override fun close(spaceId: Long) = vm.backgrounds.close(spaceId)

    override suspend fun thumbnail(spaceId: Long): ImageBitmap? = vm.backgrounds.thumbnail(spaceId)?.asImageBitmap()

    override fun save(edit: BackgroundEdit) = vm.backgrounds.save(edit)

    override fun remove(spaceId: Long) = vm.backgrounds.remove(spaceId)
}

private fun memoryActions(vm: LauncherViewModel) = object : MemoryActions {
    override fun forgetDrawing(key: AppKey, path: String) = vm.memory.forget {
        it.key == key && it.path == path
    }

    override fun forgetApp(key: AppKey) = vm.memory.forget { it.key == key }

    override fun addDrawing(key: AppKey, sample: GestureSample) = vm.memory.add(key, sample)

    override fun forgetAllGestures() = vm.memory.forget { true }

    override fun clearUsage() = vm.memory.clearUsage()
}
