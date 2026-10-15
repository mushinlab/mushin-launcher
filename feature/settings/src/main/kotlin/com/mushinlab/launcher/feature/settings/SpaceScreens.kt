package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.Palettes
import com.mushinlab.launcher.core.model.SpaceLimits
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinColors
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
internal fun SpacesScreen(state: SettingsUiState, actions: SettingsActions, back: () -> Unit) {
    var createType by remember { mutableStateOf<SpaceType?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val hasWork = state.spaces.any { it.type == SpaceType.WORK }
    val customCount = state.spaces.count { it.type == SpaceType.CUSTOM }
    Box {
        SettingsPage(stringResource(R.string.settings_spaces), back) {
            state.spaces.forEach { space ->
                val suffix = if (space.current) " · ${stringResource(R.string.space_current)}" else ""
                InfoRow(space.name, spaceTypeLabel(space.type) + suffix, tag = "env_${space.id}")
            }
            NoteText(stringResource(R.string.space_list_note))
            CardGap()
            NavRow(
                stringResource(R.string.space_create),
                enabled = customCount < SpaceLimits.MAX_CUSTOM,
                tag = "env_create",
            ) {
                error = null
                createType = SpaceType.CUSTOM
            }
            if (!hasWork) {
                NavRow(stringResource(R.string.space_create_work), tag = "env_create_work") {
                    error = null
                    createType = SpaceType.WORK
                }
            }
        }
        val type = createType
        if (type != null) {
            TextInputDialog(
                title = stringResource(R.string.space_create_title),
                initial = if (type == SpaceType.WORK) stringResource(R.string.space_work_default_name) else "",
                maxLength = SpaceLimits.NAME_MAX_LENGTH + 8,
                error = spaceErrorText(error),
                confirm = stringResource(R.string.settings_save),
                onConfirm = { name ->
                    scope.launch {
                        val result = actions.spaces.createSpace(name, type)
                        if (result == null) createType = null else error = result
                    }
                },
                onDismiss = { createType = null },
            )
        }
    }
}

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
        PaletteDropdown(space.preset) { actions.spaces.setPalette(space.id, it) }
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

@Composable
internal fun PaletteDropdown(selected: PalettePreset, onSelect: (PalettePreset) -> Unit) {
    val names = mapOf(
        PalettePreset.GRAPHITE to stringResource(R.string.palette_graphite),
        PalettePreset.NIGHT to stringResource(R.string.palette_night),
        PalettePreset.NAVY to stringResource(R.string.palette_navy),
        PalettePreset.MOSS to stringResource(R.string.palette_moss),
        PalettePreset.PAPER to stringResource(R.string.palette_paper),
        PalettePreset.SAND to stringResource(R.string.palette_sand),
        PalettePreset.LAGOON to stringResource(R.string.palette_lagoon),
        PalettePreset.PLUM to stringResource(R.string.palette_plum),
        PalettePreset.EMBER to stringResource(R.string.palette_ember),
        PalettePreset.MIST to stringResource(R.string.palette_mist),
        PalettePreset.ROSE to stringResource(R.string.palette_rose),
    )
    DropdownRow(
        stringResource(R.string.space_palette),
        Palettes.presets.keys.map { it to names[it].orEmpty() },
        selected,
        "palette",
        leading = { preset -> Palettes.presets[preset]?.let { Swatch(MushinColors.from(it)) } },
        onSelect = onSelect,
    )
}

@Composable
private fun Swatch(swatch: MushinColors) {
    Row(Modifier.clip(RoundedCornerShape(CARD_RADIUS))) {
        listOf(swatch.background, swatch.zebraB, swatch.textPrimary, swatch.accent).forEach {
            Box(Modifier.size(SWATCH).background(it))
        }
    }
}

private val SWATCH = 20.dp
