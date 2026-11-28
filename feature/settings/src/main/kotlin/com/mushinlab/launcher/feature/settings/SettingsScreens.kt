package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.GestureTuning
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.search.KeyboardLayouts
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable

@Composable
internal fun RootScreen(push: (SettingsRoute) -> Unit, back: () -> Unit) {
    SettingsPage(stringResource(R.string.settings_title), back) {
        RootRows(push)
    }
}

@Composable
private fun RootRows(push: (SettingsRoute) -> Unit) {
    NavRow(
        stringResource(R.string.settings_spaces),
        tag = "nav_spaces",
    ) { push(SettingsRoute.Spaces) }
    NavRow(
        stringResource(R.string.settings_keyboard),
        tag = "nav_keyboard",
    ) { push(SettingsRoute.Keyboard) }
    NavRow(
        stringResource(R.string.settings_gestures),
        tag = "nav_gestures",
    ) { push(SettingsRoute.Gestures) }
    NavRow(
        stringResource(R.string.settings_apps),
        tag = "nav_apps",
    ) { push(SettingsRoute.Apps) }
    NavRow(
        stringResource(R.string.settings_tips),
        tag = "nav_tips",
    ) { push(SettingsRoute.Tips) }
    NavRow(
        stringResource(R.string.settings_system),
        tag = "nav_system",
    ) { push(SettingsRoute.System) }
}

@Composable
internal fun KeyboardSettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    val s = state.settings
    val preferences = actions.preferences
    SettingsPage(stringResource(R.string.settings_keyboard), back) {
        SectionHeader(stringResource(R.string.keyboard_layouts))
        KeyboardLayouts.all.forEach { layout ->
            LayoutRow(layout.id, layout.nativeName, s.keyboardLayouts) {
                preferences.updateSettings {
                    if (layout.id in it.keyboardLayouts) it.withoutLayout(layout.id) else it.withLayout(layout.id)
                }
            }
        }
        SectionHeader(stringResource(R.string.keyboard_feel))
        ToggleRow(stringResource(R.string.keyboard_haptics), s.keyboardHaptics, tag = "kb_haptics") { v ->
            preferences.updateSettings { it.copy(keyboardHaptics = v) }
        }
        NavRow(
            stringResource(R.string.settings_advanced),
            summary = stringResource(R.string.keyboard_gesture_tuning),
            tag = "nav_keyboard_advanced",
        ) { push(SettingsRoute.KeyboardAdvanced) }
    }
}

@Composable
internal fun KeyboardAdvancedScreen(state: SettingsUiState, actions: SettingsActions, back: () -> Unit) {
    val s = state.settings
    val preferences = actions.preferences
    SettingsPage(stringResource(R.string.settings_advanced), back) {
        SectionHeader(stringResource(R.string.keyboard_gesture_tuning))
        GestureTuningRows(s, preferences)
        SectionHeader(stringResource(R.string.keyboard_restore))
        ButtonRow(stringResource(R.string.keyboard_gesture_tuning_reset), tag = "kb_gesture_tuning_reset") {
            preferences.updateSettings {
                it.copy(
                    gestureShiftPenalty = GestureTuning.SHIFT_PENALTY,
                    gestureSuggestMargin = GestureTuning.SUGGEST_MARGIN,
                )
            }
        }
    }
}

@Composable
private fun GestureTuningRows(s: LauncherSettings, preferences: PreferenceActions) {
    SliderRow(
        stringResource(R.string.keyboard_gesture_shift),
        s.gestureShiftPenalty,
        GestureTuning.SHIFT_PENALTY_MIN..GestureTuning.SHIFT_PENALTY_MAX,
        GestureTuning.STEP,
        tag = "kb_gesture_shift",
    ) { v -> preferences.updateSettings { it.copy(gestureShiftPenalty = v) } }
    NoteText(stringResource(R.string.keyboard_gesture_shift_note))
    SliderRow(
        stringResource(R.string.keyboard_gesture_margin),
        s.gestureSuggestMargin,
        GestureTuning.SUGGEST_MARGIN_MIN..GestureTuning.SUGGEST_MARGIN_MAX,
        GestureTuning.STEP,
        tag = "kb_gesture_margin",
    ) { v -> preferences.updateSettings { it.copy(gestureSuggestMargin = v) } }
    NoteText(stringResource(R.string.keyboard_gesture_margin_note))
}

@Composable
private fun ButtonRow(title: String, tag: String, onClick: () -> Unit) {
    val colors = LocalMushinColors.current
    Box(
        Modifier
            .settingsCard()
            .heightIn(min = 48.dp)
            .pressable(onClick = onClick)
            .testTag(tag)
            .padding(horizontal = CARD_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        SingleLineText(title, MushinType.style(16.sp, colors.accent, FontWeight.Medium))
    }
}

@Composable
internal fun AppsSettingsScreen(state: SettingsUiState, actions: SettingsActions, back: () -> Unit) {
    val hidden = state.apps.filter { it.hidden }
    SettingsPage(stringResource(R.string.settings_apps), back) {
        SectionHeader(stringResource(R.string.apps_hidden))
        if (hidden.isEmpty()) NoteText(stringResource(R.string.apps_none_hidden))
        hidden.forEach { app ->
            NavRow(app.label, value = stringResource(R.string.apps_show), tag = "unhide_${app.label}") {
                actions.apps.setHidden(app.key, false)
            }
        }
        SectionHeader(stringResource(R.string.apps_renamed))
        if (state.renamed.isEmpty()) NoteText(stringResource(R.string.apps_none_renamed))
        state.renamed.forEach { app ->
            NavRow(
                "${app.custom} ← ${app.original}",
                value = stringResource(R.string.settings_reset),
                tag = "unrename_${app.custom}",
            ) {
                actions.apps.rename(app.key, null)
            }
        }
    }
}

@Composable
internal fun SystemScreen(state: SettingsUiState, actions: SettingsActions, back: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    SettingsPage(stringResource(R.string.settings_system), back) {
        NavRow(
            stringResource(R.string.system_default),
            value = stringResource(
                if (state.isDefaultLauncher) R.string.system_default_yes else R.string.system_default_no,
            ),
            enabled = !state.isDefaultLauncher,
            tag = "system_default",
        ) { actions.system.requestDefaultLauncher() }
        DropdownRow(
            stringResource(R.string.system_language),
            listOf<Pair<String?, String>>(null to stringResource(R.string.system_language_default)) +
                AppLanguages.all,
            state.appLanguage,
            "system_language",
        ) { actions.system.setAppLanguage(it) }
        PrivacyRows(state, actions)
        SectionHeader(stringResource(R.string.system_about))
        NavRow(stringResource(R.string.system_version), value = state.versionName) {}
        NoteText(stringResource(R.string.system_licenses_text))
        DangerRow(stringResource(R.string.advanced_reset_app), "reset_app") { confirming = true }
        NoteText(stringResource(R.string.advanced_reset_text))
    }
    if (confirming) {
        ConfirmDialog(
            message = stringResource(R.string.advanced_reset_text),
            confirm = stringResource(R.string.advanced_reset_confirm),
            onConfirm = {
                confirming = false
                actions.system.resetApp()
            },
            onDismiss = { confirming = false },
        )
    }
}

@Composable
private fun LayoutRow(id: String, name: String, chosen: List<String>, onToggle: () -> Unit) {
    val colors = LocalMushinColors.current
    val order = chosen.indexOf(id)
    val enabled = order < 0 || chosen.size > 1
    Row(
        Modifier
            .settingsCard()
            .heightIn(min = 48.dp)
            .pressable(enabled = enabled, role = Role.Checkbox, onClick = onToggle)
            .semantics { selected = order >= 0 }
            .testTag("layouts_$id")
            .padding(horizontal = CARD_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SingleLineText(
            name,
            MushinType.style(17.sp, if (order >= 0) colors.textPrimary else colors.textSecondary),
            Modifier.weight(1f),
        )
        if (order >= 0) BasicText("${order + 1}", style = MushinType.style(14.sp, colors.accent))
    }
}
