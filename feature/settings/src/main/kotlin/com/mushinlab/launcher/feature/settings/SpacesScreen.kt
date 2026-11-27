package com.mushinlab.launcher.feature.settings

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.SpaceLimits
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.RadioMark
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable
import kotlinx.coroutines.launch

@Stable
private class SpaceListState {
    var creating by mutableStateOf(false)
    var deleting by mutableStateOf<SpaceSummary?>(null)
    var renaming by mutableStateOf<SpaceSummary?>(null)
}

internal fun creatableTypes(spaces: List<SpaceSummary>): List<SpaceType> = listOfNotNull(
    SpaceType.CUSTOM.takeIf { spaces.count { it.type == SpaceType.CUSTOM } < SpaceLimits.MAX_CUSTOM },
    SpaceType.WORK.takeIf { spaces.none { it.type == SpaceType.WORK } },
)

@Composable
internal fun SpacesScreen(spaces: List<SpaceSummary>, actions: SpaceActions, back: () -> Unit) {
    val view = LocalView.current
    val list = remember { SpaceListState() }
    Box {
        SettingsPage(stringResource(R.string.settings_spaces), back) {
            spaces.forEach { space ->
                SpaceRow(space, onRename = { list.renaming = space }, onDelete = { list.deleting = space })
            }
            NavRow(stringResource(R.string.space_create_title), tag = "settings_add_space") {
                if (creatableTypes(spaces).isEmpty()) {
                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                } else {
                    list.creating = true
                }
            }
        }
        SpaceDialogs(spaces, actions, list)
    }
}

@Composable
private fun SpaceRow(space: SpaceSummary, onRename: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalMushinColors.current
    Row(
        Modifier
            .settingsCard()
            .heightIn(min = 56.dp)
            .testTag("space_row_${space.id}")
            .padding(start = CARD_PADDING, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            SingleLineText(space.name, MushinType.style(17.sp, colors.textPrimary))
            SingleLineText(spaceTypeLabel(space.type), MushinType.style(14.sp, colors.textSecondary))
        }
        GlyphButton(
            Glyph.EDIT,
            stringResource(R.string.space_rename),
            colors.textPrimary,
            tag = "space_rename_${space.id}",
            onClick = onRename,
        )
        if (space.type != SpaceType.NORMAL) {
            GlyphButton(
                Glyph.DELETE,
                stringResource(R.string.space_delete),
                colors.danger,
                tag = "space_delete_${space.id}",
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun SpaceDialogs(spaces: List<SpaceSummary>, actions: SpaceActions, list: SpaceListState) {
    val types = creatableTypes(spaces)
    if (list.creating && types.isNotEmpty()) CreateSpaceDialog(types, actions) { list.creating = false }
    list.renaming?.let { space ->
        RenameSpaceDialog(space, actions) { list.renaming = null }
    }
    list.deleting?.let { space ->
        DeleteSpaceDialog(space, onConfirm = { actions.deleteSpace(space.id) }, onDismiss = { list.deleting = null })
    }
}

@Composable
private fun CreateSpaceDialog(types: List<SpaceType>, actions: SpaceActions, onClose: () -> Unit) {
    var chosen by remember { mutableStateOf(types.first()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val type = chosen.takeIf { it in types } ?: types.first()
    val options: (@Composable ColumnScope.() -> Unit)? = if (types.size > 1) {
        {
            SpaceTypeChoice(types, type) {
                chosen = it
                error = null
            }
        }
    } else {
        null
    }
    key(type) {
        TextInputDialog(
            title = stringResource(R.string.space_create_title),
            initial = if (type == SpaceType.WORK) stringResource(R.string.space_work_default_name) else "",
            maxLength = SpaceLimits.NAME_MAX_LENGTH + 8,
            error = spaceErrorText(error),
            confirm = stringResource(R.string.settings_save),
            options = options,
            onConfirm = { name ->
                scope.launch {
                    val result = actions.createSpace(name, type)
                    if (result == null) onClose() else error = result
                }
            },
            onDismiss = onClose,
        )
    }
}

@Composable
private fun SpaceTypeChoice(types: List<SpaceType>, selected: SpaceType, onSelect: (SpaceType) -> Unit) {
    val colors = LocalMushinColors.current
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        types.forEach { type ->
            Row(
                Modifier
                    .heightIn(min = 48.dp)
                    .pressable(role = Role.RadioButton) { onSelect(type) }
                    .semantics { this.selected = type == selected }
                    .testTag("space_type_${type.name}")
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioMark(type == selected)
                Spacer(Modifier.width(8.dp))
                SingleLineText(
                    spaceTypeLabel(type).replaceFirstChar { it.titlecase() },
                    MushinType.style(16.sp, colors.textPrimary),
                )
            }
        }
    }
}

@Composable
private fun DeleteSpaceDialog(space: SpaceSummary, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    SystemConfirmDialog(
        title = stringResource(R.string.space_delete),
        message = stringResource(R.string.space_delete_confirm, space.name),
        confirm = stringResource(R.string.settings_delete),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
