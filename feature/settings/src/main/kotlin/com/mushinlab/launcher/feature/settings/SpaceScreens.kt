package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.Palettes
import com.mushinlab.launcher.core.model.SpaceLimits
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.ui.LocalMushinColors
import kotlinx.coroutines.launch

@Composable
internal fun spaceTypeLabel(type: SpaceType): String = stringResource(
    when (type) {
        SpaceType.NORMAL -> R.string.space_type_normal
        SpaceType.WORK -> R.string.space_type_work
        SpaceType.CUSTOM -> R.string.space_type_custom
    },
)

@Composable
internal fun spaceErrorText(code: String?): String? = when (code) {
    null -> null
    ERR_EMPTY -> stringResource(R.string.error_name_empty)
    ERR_LONG -> stringResource(R.string.error_name_long)
    ERR_TAKEN -> stringResource(R.string.error_name_taken)
    ERR_WORK -> stringResource(R.string.error_work_limit)
    ERR_CUSTOM -> stringResource(R.string.error_custom_limit)
    else -> stringResource(R.string.error_generic)
}

const val ERR_EMPTY = "empty"
const val ERR_LONG = "long"
const val ERR_TAKEN = "taken"
const val ERR_WORK = "work"
const val ERR_CUSTOM = "custom"

@Composable
internal fun SpaceEditorScreen(
    spaceId: Long,
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    val space = state.spaces.firstOrNull { it.id == spaceId }
    if (space == null) {
        back()
        return
    }
    var renaming by remember { mutableStateOf(false) }
    var renameError by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Box {
        SpaceEditorPage(
            space,
            state,
            actions,
            push,
            back,
            onRename = {
                renameError = null
                renaming = true
            },
            onDelete = { deleting = true },
        )
        if (renaming) {
            TextInputDialog(
                title = stringResource(R.string.space_rename_title),
                initial = space.name,
                maxLength = SpaceLimits.NAME_MAX_LENGTH + 8,
                error = spaceErrorText(renameError),
                confirm = stringResource(R.string.settings_save),
                onConfirm = { name ->
                    scope.launch {
                        val result = actions.spaces.renameSpace(spaceId, name)
                        if (result == null) renaming = false else renameError = result
                    }
                },
                onDismiss = { renaming = false },
            )
        }
        if (deleting) {
            ConfirmDialog(
                message = stringResource(R.string.space_delete_confirm, space.name),
                confirm = stringResource(R.string.settings_delete),
                onConfirm = {
                    deleting = false
                    actions.spaces.deleteSpace(spaceId)
                    back()
                },
                onDismiss = { deleting = false },
            )
        }
    }
}

@Composable
private fun SpaceEditorPage(
    space: SpaceSummary,
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    SettingsPage(space.name, back) {
        NavRow(stringResource(R.string.space_name), value = space.name, tag = "env_rename", onClick = onRename)
        CardGap()
        PalettePicker(space.preset) { actions.spaces.setPalette(space.id, it) }
        ToggleRow(stringResource(R.string.space_colored_icons), space.coloredIcons, tag = "env_colored_icons") {
            actions.spaces.setColoredIcons(space.id, it)
        }
        NoteText(stringResource(R.string.space_colored_icons_note))
        SectionHeader(stringResource(R.string.space_items))
        SpaceItemsSection(space.id, state, actions, push)
        CardGap()
        NavRow(
            stringResource(R.string.space_delete),
            enabled = space.type != SpaceType.NORMAL,
            tag = "env_delete",
            onClick = onDelete,
        )
    }
}

@Composable
private fun SpaceItemsSection(
    spaceId: Long,
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
) {
    val items = state.spaceItems[spaceId].orEmpty()
    if (items.size > state.capacity && state.capacity > 0) {
        NoteText(
            stringResource(R.string.space_overflow, state.capacity, items.size),
            LocalMushinColors.current.accent,
        )
    }
    if (items.isEmpty()) NoteText(stringResource(R.string.space_empty))
    ReorderableItems(items, onChange = { keys -> actions.spaces.setSpaceItems(spaceId, keys) })
    NavRow(stringResource(R.string.space_add), tag = "env_add") { push(SettingsRoute.PickApps(spaceId)) }
}
