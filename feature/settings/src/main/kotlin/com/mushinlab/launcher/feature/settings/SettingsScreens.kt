package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.ClockFormat
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.search.KeyboardLayouts
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.RadioMark
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable

@Composable
internal fun RootScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    SettingsPage(stringResource(R.string.settings_title), back) {
        SpaceChips(state.spaces, actions.spaces::switchSpace)
        NavRow(
            stringResource(R.string.settings_style),
            summary = stringResource(R.string.summary_style),
            tag = "nav_style",
        ) { push(SettingsRoute.Style) }
        NavRow(
            stringResource(R.string.settings_home),
            summary = stringResource(R.string.summary_home),
            tag = "nav_home",
        ) { push(SettingsRoute.Home) }
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
            stringResource(R.string.settings_spaces),
            summary = stringResource(R.string.summary_spaces),
            tag = "nav_envs",
        ) { push(SettingsRoute.Spaces) }
        NavRow(
            stringResource(R.string.settings_apps),
            summary = stringResource(R.string.summary_apps),
            tag = "nav_apps",
        ) { push(SettingsRoute.Apps) }
        NavRow(
            stringResource(R.string.settings_system),
            summary = stringResource(R.string.summary_system),
            tag = "nav_system",
        ) { push(SettingsRoute.System) }
        NavRow(
            stringResource(R.string.settings_tips),
            summary = stringResource(R.string.summary_tips),
            tag = "nav_tips",
        ) { push(SettingsRoute.Tips) }
    }
}

@Composable
private fun SpaceChips(spaces: List<SpaceSummary>, onSelect: (Long) -> Unit) {
    val colors = LocalMushinColors.current
    val scroll = rememberScrollState()
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = CARD_MARGIN, vertical = 8.dp)
            .testTag("space_chips"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        spaces.forEach { space ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(CARD_RADIUS))
                    .background(if (space.current) colors.accent.copy(alpha = CHIP_ALPHA) else colors.surface)
                    .border(
                        1.dp,
                        if (space.current) colors.accent else colors.surface,
                        RoundedCornerShape(CARD_RADIUS),
                    )
                    .pressable(role = Role.RadioButton) { onSelect(space.id) }
                    .semantics { selected = space.current }
                    .testTag("space_chip_${space.id}")
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioMark(space.current)
                Spacer(Modifier.width(8.dp))
                SingleLineText(space.name, MushinType.style(16.sp, colors.textPrimary))
            }
        }
    }
}

@Composable
internal fun AdvancedScreen(state: SettingsUiState, push: (SettingsRoute) -> Unit, back: () -> Unit) {
    SettingsPage(stringResource(R.string.settings_advanced), back) {
        NavRow(
            stringResource(R.string.settings_press),
            value = pressLabel(state.settings.pressFeedback),
            summary = stringResource(R.string.summary_press),
            tag = "nav_press",
        ) { push(SettingsRoute.Press) }
        NavRow(
            stringResource(R.string.settings_memory),
            summary = stringResource(R.string.summary_memory),
            tag = "nav_memory",
        ) { push(SettingsRoute.Memory) }
    }
}

@Composable
internal fun HomeSettingsScreen(state: SettingsUiState, actions: SettingsActions, back: () -> Unit) {
    val s = state.settings
    val preferences = actions.preferences
    SettingsPage(stringResource(R.string.settings_home), back) {
        DropdownRow(
            stringResource(R.string.home_icons),
            listOf(
                IconMode.NONE to stringResource(R.string.home_icons_none),
                IconMode.LEFT to stringResource(R.string.home_icons_left),
                IconMode.RIGHT to stringResource(R.string.home_icons_right),
            ),
            s.iconMode,
            "icon_mode",
        ) { mode -> preferences.updateSettings { it.copy(iconMode = mode) } }
        DropdownRow(
            stringResource(R.string.home_clock),
            listOf(
                ClockFormat.SYSTEM to stringResource(R.string.home_clock_system),
                ClockFormat.H24 to stringResource(R.string.home_clock_24),
                ClockFormat.H12 to stringResource(R.string.home_clock_12),
            ),
            s.clockFormat,
            "clock",
        ) { format -> preferences.updateSettings { it.copy(clockFormat = format) } }
        CardGap()
        ToggleRow(stringResource(R.string.home_show_date), s.showDate, tag = "show_date") { value ->
            preferences.updateSettings { it.copy(showDate = value) }
        }
        NoteText(stringResource(R.string.home_clock_note))
        CardGap()
        ToggleRow(stringResource(R.string.home_notifications), s.notificationCounts, tag = "notification_counts") {
            preferences.updateSettings { settings -> settings.copy(notificationCounts = it) }
            if (it && !state.notificationAccess) actions.system.openNotificationAccess()
        }
        if (s.notificationCounts && !state.notificationAccess) {
            NoteText(stringResource(R.string.home_notifications_no_access), LocalMushinColors.current.accent)
            NavRow(stringResource(R.string.home_notifications_grant), tag = "notification_access") {
                actions.system.openNotificationAccess()
            }
        } else {
            NoteText(stringResource(R.string.home_notifications_note))
        }
    }
}

@Composable
internal fun KeyboardSettingsScreen(state: SettingsUiState, actions: SettingsActions, back: () -> Unit) {
    val s = state.settings
    val preferences = actions.preferences
    val enabled = s.keyboardLayouts
    val ordered = enabled + KeyboardLayouts.all.map { it.id }.filterNot { it in enabled }
    SettingsPage(stringResource(R.string.settings_keyboard), back) {
        SectionHeader(stringResource(R.string.keyboard_layouts))
        ordered.forEachIndexed { index, id ->
            val layout = KeyboardLayouts.byId(id) ?: return@forEachIndexed
            val on = id in enabled
            ToggleRow(layout.label, on, tag = "layout_$id") { value ->
                val next = if (value) enabled + id else enabled - id
                if (next.isNotEmpty()) {
                    preferences.updateSettings {
                        it.copy(
                            keyboardLayouts = next,
                            activeKeyboardLayout = if (it.activeKeyboardLayout in
                                next
                            ) {
                                it.activeKeyboardLayout
                            } else {
                                next.first()
                            },
                        )
                    }
                }
            }
            if (on && index > 0) {
                NavRow("↑ ${layout.label}", tag = "layout_up_$id") {
                    val list = enabled.toMutableList()
                    val i = list.indexOf(id)
                    if (i > 0) {
                        list.removeAt(i)
                        list.add(i - 1, id)
                        preferences.updateSettings { it.copy(keyboardLayouts = list) }
                    }
                }
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
        NavRow(stringResource(R.string.system_version), value = state.versionName) {}
        NavRow(
            stringResource(R.string.settings_advanced),
            summary = stringResource(R.string.summary_advanced),
            tag = "nav_advanced",
        ) { push(SettingsRoute.Advanced) }
        SectionHeader(stringResource(R.string.system_privacy))
        NoteText(stringResource(R.string.system_privacy_text))
        SectionHeader(stringResource(R.string.system_licenses))
        NoteText(stringResource(R.string.system_licenses_text))
    }
}

private const val CHIP_ALPHA = 0.16f
