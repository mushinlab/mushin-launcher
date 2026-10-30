package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.OverrideLimits
import com.mushinlab.launcher.core.ui.AppIcon
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.SingleLineText

@Composable
internal fun FolderSheet(
    folderId: Long,
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    val folder = state.folders[folderId]
    if (folder == null) {
        LaunchedEffect(Unit) { back() }
        return
    }
    val colors = LocalMushinColors.current
    SettingsSheet(tag = "folder_sheet", onClose = back, scrimAlpha = HOME_SHEET_SCRIM, color = colors.background) {
        FolderNameField(folder.name) { actions.spaces.folders.rename(folderId, it) }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())) {
            folder.apps.forEach { app ->
                FolderAppRow(app) { actions.spaces.folders.removeApp(folderId, app.key) }
            }
            NavRow(stringResource(R.string.folder_add_apps), tag = "folder_add_apps") {
                push(SettingsRoute.PickFolderApps(folderId))
            }
        }
        Spacer(Modifier.height(8.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.ime.union(WindowInsets.navigationBars)))
    }
}

@Composable
private fun FolderNameField(initial: String, onChange: (String) -> Unit) {
    val colors = LocalMushinColors.current
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var name by remember { mutableStateOf(initial) }
    LaunchedEffect(Unit) { if (initial.isEmpty()) focus.requestFocus() }
    val style = MushinType.style(20.sp, colors.textPrimary, FontWeight.Medium)
    BasicTextField(
        value = name,
        onValueChange = { value ->
            name = value.take(OverrideLimits.LABEL_MAX_LENGTH)
            onChange(name)
        },
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        decorationBox = { inner ->
            if (name.isEmpty()) {
                BasicText(stringResource(R.string.folder_name_hint), style = style.copy(color = colors.textSecondary))
            }
            inner()
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CARD_MARGIN + CARD_PADDING, vertical = 4.dp)
            .focusRequester(focus)
            .testTag("folder_name"),
    )
}

@Composable
private fun FolderAppRow(app: EditorItem, onRemove: () -> Unit) {
    val colors = LocalMushinColors.current
    Row(
        Modifier
            .settingsCard()
            .heightIn(min = 56.dp)
            .padding(start = CARD_PADDING, end = 4.dp)
            .testTag("folder_app_${app.label}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.key, app.label, 28.dp)
        Spacer(Modifier.width(12.dp))
        SingleLineText(app.label, MushinType.style(17.sp, colors.textPrimary), Modifier.weight(1f))
        GlyphButton(
            Glyph.CLOSE,
            stringResource(R.string.folder_remove_app, app.label),
            colors.textSecondary,
            tag = "folder_remove_${app.label}",
            onClick = onRemove,
        )
    }
}

@Composable
internal fun FolderAppPicker(folderId: Long, state: SettingsUiState, actions: SettingsActions, back: () -> Unit) {
    val inFolder = state.folders[folderId]?.apps.orEmpty().map { it.key }.toSet()
    val visible = state.apps.filter { !it.hidden || it.key in inFolder }
    var query by remember { mutableStateOf("") }
    SettingsPage(stringResource(R.string.folder_add_apps), back, scrollable = false) {
        FilterField(query) { query = it }
        CardGap()
        LazyColumn(Modifier.weight(1f)) {
            items(filterApps(visible, query), key = { it.key.id }) { app ->
                val checked = app.key in inFolder
                PickerRow(app.label, checked, enabled = true, tag = "pick_${app.label}") {
                    if (checked) {
                        actions.spaces.folders.removeApp(folderId, app.key)
                    } else {
                        actions.spaces.folders.addApp(folderId, app.key)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ReplaceAppPicker(
    route: SettingsRoute.ReplaceApp,
    state: SettingsUiState,
    actions: SettingsActions,
    back: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val label = state.spaceItems[route.spaceId]?.firstOrNull { it.key == route.key }?.label.orEmpty()
    SettingsPage(stringResource(R.string.replace_title, label), back, scrollable = false) {
        FilterField(query) { query = it }
        LazyColumn(Modifier.weight(1f)) {
            items(filterApps(state.apps.filterNot { it.hidden }, query), key = { it.key.id }) { app ->
                NavRow(app.label, selected = app.key == route.key, tag = "pick_${app.label}") {
                    actions.spaces.replaceSpaceItem(route.spaceId, route.key, app.key)
                    back()
                }
            }
        }
    }
}
