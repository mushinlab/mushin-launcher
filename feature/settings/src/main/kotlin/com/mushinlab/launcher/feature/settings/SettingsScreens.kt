package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.GestureTuning
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.search.KeyboardLayout
import com.mushinlab.launcher.core.search.KeyboardLayouts
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable

@Composable
internal fun RootScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    val bar = remember { SpaceBarState() }
    Box {
        SettingsPage(stringResource(R.string.settings_title), back) {
            SpaceBar(state.spaces, actions.spaces, bar)
            RootRows(push)
        }
        SpaceBarOverlays(state.spaces, actions.spaces, bar)
    }
}

@Composable
private fun RootRows(push: (SettingsRoute) -> Unit) {
    NavRow(
        stringResource(R.string.settings_keyboard),
        summary = stringResource(R.string.summary_keyboard),
        tag = "nav_keyboard",
    ) { push(SettingsRoute.Keyboard) }
    NavRow(
        stringResource(R.string.settings_gestures),
        summary = stringResource(R.string.summary_gestures),
        tag = "nav_gestures",
    ) { push(SettingsRoute.Gestures) }
    NavRow(
        stringResource(R.string.settings_apps),
        summary = stringResource(R.string.summary_apps),
        tag = "nav_apps",
    ) { push(SettingsRoute.Apps) }
    NavRow(
        stringResource(R.string.settings_tips),
        summary = stringResource(R.string.summary_tips),
        tag = "nav_tips",
    ) { push(SettingsRoute.Tips) }
    NavRow(
        stringResource(R.string.settings_system),
        summary = stringResource(R.string.summary_system),
        tag = "nav_system",
    ) { push(SettingsRoute.System) }
}

@Composable
internal fun KeyboardSettingsScreen(state: SettingsUiState, actions: SettingsActions, back: () -> Unit) {
    val s = state.settings
    val preferences = actions.preferences
    val enabled = s.keyboardLayouts.mapNotNull(KeyboardLayouts::byId)
    val available = KeyboardLayouts.all.filterNot { it in enabled }
    SettingsPage(stringResource(R.string.settings_keyboard), back) {
        SectionHeader(stringResource(R.string.keyboard_layouts))
        enabled.forEach { layout ->
            LayoutRow(layout, removable = enabled.size > 1) {
                preferences.updateSettings { it.withoutLayout(layout.id) }
            }
        }
        if (available.isNotEmpty()) {
            DropdownRow<String?>(
                stringResource(R.string.keyboard_add_layout),
                available.map { it.id to "${it.nativeName} · ${it.label}" },
                null,
                "layout_add",
            ) { id ->
                if (id != null) preferences.updateSettings { it.withLayout(id) }
            }
        }
        NoteText(stringResource(R.string.keyboard_need_one))
        CardGap()
        ToggleRow(stringResource(R.string.keyboard_haptics), s.keyboardHaptics, tag = "kb_haptics") { v ->
            preferences.updateSettings { it.copy(keyboardHaptics = v) }
        }
        ToggleRow(stringResource(R.string.keyboard_digit_hints), s.keyboardDigitHints, tag = "kb_digits") { v ->
            preferences.updateSettings { it.copy(keyboardDigitHints = v) }
        }
        NoteText(stringResource(R.string.keyboard_fit_note))
        SectionHeader(stringResource(R.string.keyboard_gesture_tuning))
        GestureTuningRows(s, preferences)
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
    NavRow(stringResource(R.string.keyboard_gesture_tuning_reset), tag = "kb_gesture_tuning_reset") {
        preferences.updateSettings {
            it.copy(
                gestureShiftPenalty = GestureTuning.SHIFT_PENALTY,
                gestureSuggestMargin = GestureTuning.SUGGEST_MARGIN,
            )
        }
    }
}

@Composable
private fun LayoutRow(layout: KeyboardLayout, removable: Boolean, onRemove: () -> Unit) {
    val colors = LocalMushinColors.current
    Row(
        Modifier
            .settingsCard()
            .heightIn(min = 56.dp)
            .testTag("layout_${layout.id}")
            .padding(start = CARD_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            BasicText(layout.nativeName, style = MushinType.style(17.sp, colors.textPrimary))
            BasicText(layout.label, style = MushinType.style(14.sp, colors.textSecondary))
        }
        Box(
            Modifier
                .heightIn(min = 56.dp)
                .pressable(enabled = removable, onClick = onRemove)
                .testTag("layout_remove_${layout.id}")
                .alpha(if (removable) 1f else DISABLED_ALPHA)
                .padding(horizontal = CARD_PADDING),
            contentAlignment = Alignment.Center,
        ) {
            SingleLineText(stringResource(R.string.keyboard_remove_layout), MushinType.style(15.sp, colors.accent))
        }
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
internal fun SystemScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
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
        NavRow(stringResource(R.string.system_version), value = state.versionName) {}
        NavRow(
            stringResource(R.string.system_privacy),
            summary = stringResource(R.string.summary_privacy),
            tag = "nav_privacy",
        ) { push(SettingsRoute.Privacy) }
        SectionHeader(stringResource(R.string.system_licenses))
        NoteText(stringResource(R.string.system_licenses_text))
    }
}
