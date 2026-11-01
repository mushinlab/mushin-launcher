package com.mushinlab.launcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.feature.search.AppPickerActions
import com.mushinlab.launcher.feature.search.AppPickerScreen
import com.mushinlab.launcher.feature.search.AppPickerState
import com.mushinlab.launcher.feature.search.PickerEntry
import com.mushinlab.launcher.feature.settings.R as SettingsR
import com.mushinlab.launcher.feature.settings.SettingsRoute
import com.mushinlab.launcher.feature.settings.SettingsUiState
import kotlinx.collections.immutable.toImmutableList

internal data class PickerModel(val entries: List<PickerEntry>, val limit: Int?, val name: String)

internal fun pickerModel(route: SettingsRoute, state: SettingsUiState): PickerModel? = when (route) {
    is SettingsRoute.PickApps -> {
        val existing = state.spaceItems[route.spaceId].orEmpty().map { it.key }.toSet()
        PickerModel(
            entries(state) { it in existing },
            state.capacity.takeIf { it > 0 },
            state.spaces.firstOrNull { it.id == route.spaceId }?.name.orEmpty(),
        )
    }

    is SettingsRoute.PickFolderApps -> state.folders[route.folderId]?.let { folder ->
        val inFolder = folder.apps.map { it.key }.toSet()
        PickerModel(entries(state) { it in inFolder }, null, folder.name)
    }

    is SettingsRoute.ReplaceApp -> PickerModel(
        entries(state) { it == route.key },
        null,
        state.spaceItems[route.spaceId]?.firstOrNull { it.key == route.key }?.label.orEmpty(),
    )

    else -> null
}

private fun entries(state: SettingsUiState, checked: (AppKey) -> Boolean): List<PickerEntry> =
    state.apps.filter { !it.hidden || checked(it.key) }.map { PickerEntry(it.key, it.label, checked(it.key)) }

@Composable
internal fun SettingsAppPicker(vm: LauncherViewModel, route: SettingsRoute, state: SettingsUiState, back: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val chrome = rememberSearchChrome(settings)
    val model = pickerModel(route, state) ?: return
    val selected = model.entries.count { it.checked }
    val limit = model.limit
    val full = limit != null && selected >= limit
    val picker = AppPickerState(
        title = when (route) {
            is SettingsRoute.ReplaceApp -> stringResource(SettingsR.string.replace_title, model.name)
            is SettingsRoute.PickFolderApps -> stringResource(SettingsR.string.folder_add_apps)
            else -> stringResource(SettingsR.string.picker_title)
        },
        entries = model.entries.toImmutableList(),
        note = when {
            limit != null -> stringResource(
                if (full) SettingsR.string.picker_full else SettingsR.string.picker_selected,
                selected,
                limit,
            )

            route is SettingsRoute.ReplaceApp -> null

            else -> model.name.takeIf { it.isNotBlank() }
        },
        noteAlert = full,
        full = full,
    )
    val actions = remember(vm, route, back) { pickerActions(vm, route, back) }
    AppPickerScreen(picker, chrome, actions)
}

private fun pickerActions(vm: LauncherViewModel, route: SettingsRoute, back: () -> Unit) = object : AppPickerActions {
    override fun onToggle(key: AppKey) {
        val state = vm.settingsState.value
        when (route) {
            is SettingsRoute.PickApps ->
                if (state.spaceItems[route.spaceId].orEmpty().any { it.key == key }) {
                    vm.spaces.removeItem(route.spaceId, key)
                } else {
                    vm.spaces.addItem(route.spaceId, key)
                }

            is SettingsRoute.PickFolderApps ->
                if (state.folders[route.folderId]?.apps.orEmpty().any { it.key == key }) {
                    vm.folders.removeApp(route.folderId, key)
                } else {
                    vm.folders.addApp(route.folderId, key)
                }

            is SettingsRoute.ReplaceApp -> {
                vm.spaces.setItems(
                    route.spaceId,
                    state.spaceItems[route.spaceId].orEmpty().map { if (it.key == route.key) key else it.key },
                )
                back()
            }

            else -> Unit
        }
    }

    override fun onSwitchLayout() = vm.searchInput.switchLayout()

    override fun onBack() = back()
}
