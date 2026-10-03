package com.bluecubelabs.env.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.bluecubelabs.env.core.model.ClockFormat
import com.bluecubelabs.env.core.model.GestureAction
import com.bluecubelabs.env.core.model.HomeTextSize
import com.bluecubelabs.env.core.model.IconMode
import com.bluecubelabs.env.core.model.PressFeedback
import com.bluecubelabs.env.core.model.SwipeDirection
import com.bluecubelabs.env.core.search.KeyboardLayouts
import com.bluecubelabs.env.core.ui.HorizontalDivider
import com.bluecubelabs.env.core.ui.LocalEnvColors

@Composable
internal fun RootScreen(state: SettingsUiState, push: (SettingsRoute) -> Unit, back: () -> Unit) {
    SettingsPage(stringResource(R.string.settings_title), back) {
        state.envs.firstOrNull { it.current }?.let { env ->
            SectionHeader(stringResource(R.string.settings_current_env, env.name))
            NavRow(
                stringResource(R.string.env_items),
                value = state.envItems[env.id].orEmpty().size.toString(),
                tag = "nav_env_apps",
            ) { push(SettingsRoute.EnvApps(env.id)) }
            HorizontalDivider()
        }
        NavRow(stringResource(R.string.settings_envs), tag = "nav_envs") { push(SettingsRoute.Environments) }
        NavRow(stringResource(R.string.settings_home), tag = "nav_home") { push(SettingsRoute.Home) }
        NavRow(stringResource(R.string.settings_search), tag = "nav_search") { push(SettingsRoute.Search) }
        NavRow(stringResource(R.string.settings_keyboard), tag = "nav_keyboard") { push(SettingsRoute.Keyboard) }
        NavRow(stringResource(R.string.settings_gestures), tag = "nav_gestures") { push(SettingsRoute.Gestures) }
        NavRow(
            stringResource(R.string.settings_press),
            value = pressLabel(state.settings.pressFeedback),
            tag = "nav_press",
        ) { push(SettingsRoute.Press) }
        NavRow(stringResource(R.string.settings_apps), tag = "nav_apps") { push(SettingsRoute.Apps) }
        NavRow(stringResource(R.string.settings_system), tag = "nav_system") { push(SettingsRoute.System) }
    }
}

@Composable
internal fun HomeSettingsScreen(state: SettingsUiState, actions: SettingsActions, back: () -> Unit) {
    val s = state.settings
    SettingsPage(stringResource(R.string.settings_home), back) {
        SectionHeader(stringResource(R.string.home_icons))
        ChoiceRows(
            listOf(
                IconMode.NONE to stringResource(R.string.home_icons_none),
                IconMode.LEFT to stringResource(R.string.home_icons_left),
                IconMode.RIGHT to stringResource(R.string.home_icons_right),
            ),
            s.iconMode,
            "icon_mode",
        ) { mode -> actions.updateSettings { it.copy(iconMode = mode) } }
        SectionHeader(stringResource(R.string.home_clock))
        ChoiceRows(
            listOf(
                ClockFormat.SYSTEM to stringResource(R.string.home_clock_system),
                ClockFormat.H24 to stringResource(R.string.home_clock_24),
                ClockFormat.H12 to stringResource(R.string.home_clock_12),
            ),
            s.clockFormat,
            "clock",
        ) { format -> actions.updateSettings { it.copy(clockFormat = format) } }
        HorizontalDivider()
        ToggleRow(stringResource(R.string.home_show_date), s.showDate, tag = "show_date") { value ->
            actions.updateSettings { it.copy(showDate = value) }
        }
        SectionHeader(stringResource(R.string.home_text_size))
        ChoiceRows(
            listOf(
                HomeTextSize.S to stringResource(R.string.home_text_s),
                HomeTextSize.M to stringResource(R.string.home_text_m),
                HomeTextSize.L to stringResource(R.string.home_text_l),
            ),
            s.homeTextSize,
            "text_size",
        ) { size -> actions.updateSettings { it.copy(homeTextSize = size) } }
        HorizontalDivider()
        ToggleRow(stringResource(R.string.home_notifications), s.notificationCounts, tag = "notification_counts") {
            actions.updateSettings { settings -> settings.copy(notificationCounts = it) }
            if (it && !state.notificationAccess) actions.openNotificationAccess()
        }
        if (s.notificationCounts && !state.notificationAccess) {
            NoteText(stringResource(R.string.home_notifications_no_access), LocalEnvColors.current.accent)
            NavRow(stringResource(R.string.home_notifications_grant), tag = "notification_access") {
                actions.openNotificationAccess()
            }
        } else {
            NoteText(stringResource(R.string.home_notifications_note))
        }
    }
}

@Composable
internal fun pressLabel(feedback: PressFeedback): String = stringResource(
    when (feedback) {
        PressFeedback.FLASH -> R.string.press_flash
        PressFeedback.HOLD -> R.string.press_hold
        PressFeedback.ACCENT -> R.string.press_accent
        PressFeedback.DIM -> R.string.press_dim
        PressFeedback.SCALE -> R.string.press_scale
    },
)

@Composable
internal fun PressSettingsScreen(state: SettingsUiState, actions: SettingsActions, back: () -> Unit) {
    SettingsPage(stringResource(R.string.settings_press), back) {
        ChoiceRows(
            PressFeedback.entries.map { it to pressLabel(it) },
            state.settings.pressFeedback,
            "press",
        ) { feedback -> actions.updateSettings { it.copy(pressFeedback = feedback) } }
        NoteText(stringResource(R.string.press_note))
    }
}

@Composable
internal fun SearchSettingsScreen(state: SettingsUiState, actions: SettingsActions, back: () -> Unit) {
    val s = state.settings
    var confirm by remember { mutableStateOf(false) }
    val current = state.envs.firstOrNull { it.current }
    Box {
        SettingsPage(stringResource(R.string.settings_search), back) {
            ToggleRow(stringResource(R.string.search_fuzzy), s.searchFuzzy, tag = "search_fuzzy") { v ->
                actions.updateSettings { it.copy(searchFuzzy = v) }
            }
            ToggleRow(stringResource(R.string.search_layout_swap), s.searchLayoutSwap, tag = "search_swap") { v ->
                actions.updateSettings { it.copy(searchLayoutSwap = v) }
            }
            ToggleRow(stringResource(R.string.search_translit), s.searchTranslit, tag = "search_translit") { v ->
                actions.updateSettings { it.copy(searchTranslit = v) }
            }
            HorizontalDivider()
            NavRow(
                stringResource(R.string.search_clear_history),
                value = current?.name,
                enabled = current != null,
                tag = "clear_history",
            ) {
                confirm = true
            }
        }
        if (confirm && current != null) {
            ConfirmDialog(
                message = stringResource(R.string.search_clear_confirm, current.name),
                confirm = stringResource(R.string.settings_clear),
                onConfirm = {
                    actions.clearHistory(current.id)
                    confirm = false
                },
                onDismiss = { confirm = false },
            )
        }
    }
}

@Composable
internal fun KeyboardSettingsScreen(state: SettingsUiState, actions: SettingsActions, back: () -> Unit) {
    val s = state.settings
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
                    actions.updateSettings {
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
                        actions.updateSettings { it.copy(keyboardLayouts = list) }
                    }
                }
            }
        }
        NoteText(stringResource(R.string.keyboard_need_one))
        HorizontalDivider()
        ToggleRow(stringResource(R.string.keyboard_haptics), s.keyboardHaptics, tag = "kb_haptics") { v ->
            actions.updateSettings { it.copy(keyboardHaptics = v) }
        }
        ToggleRow(stringResource(R.string.keyboard_digit_hints), s.keyboardDigitHints, tag = "kb_digits") { v ->
            actions.updateSettings { it.copy(keyboardDigitHints = v) }
        }
        HorizontalDivider()
        ToggleRow(
            stringResource(R.string.keyboard_gesture_letters),
            s.keyboardGestureLetters,
            tag = "kb_gesture_letters",
        ) { v -> actions.updateSettings { it.copy(keyboardGestureLetters = v) } }
        NoteText(stringResource(R.string.keyboard_gesture_letters_note))
    }
}

@Composable
internal fun directionLabel(direction: SwipeDirection): String = stringResource(
    when (direction) {
        SwipeDirection.UP -> R.string.gesture_up
        SwipeDirection.DOWN -> R.string.gesture_down
        SwipeDirection.LEFT -> R.string.gesture_left
        SwipeDirection.RIGHT -> R.string.gesture_right
    },
)

@Composable
internal fun actionLabel(summary: GestureSummary?): String {
    val action = summary?.action ?: GestureAction.None
    return when (action) {
        GestureAction.None -> stringResource(R.string.action_none)

        GestureAction.Notifications -> stringResource(R.string.action_notifications)

        GestureAction.Search -> stringResource(R.string.action_search)

        GestureAction.AllApps -> stringResource(R.string.action_all_apps)

        is GestureAction.LaunchApp -> if (summary?.appMissing != false) {
            stringResource(R.string.action_app_missing)
        } else {
            summary.appLabel.orEmpty()
        }
    }
}

@Composable
internal fun GesturesScreen(state: SettingsUiState, push: (SettingsRoute) -> Unit, back: () -> Unit) {
    SettingsPage(stringResource(R.string.settings_gestures), back) {
        SwipeDirection.entries.forEach { direction ->
            NavRow(
                directionLabel(direction),
                value = actionLabel(state.gestures[direction]),
                tag = "gesture_$direction",
            ) {
                push(SettingsRoute.GestureAction(direction))
            }
        }
        NoteText(stringResource(R.string.gesture_shade_zone))
    }
}

@Composable
internal fun GestureActionScreen(
    direction: SwipeDirection,
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    val current = state.gestures[direction]?.action ?: GestureAction.None
    SettingsPage(directionLabel(direction), back) {
        val simple = listOf(
            GestureAction.None to stringResource(R.string.action_none),
            GestureAction.Notifications to stringResource(R.string.action_notifications),
            GestureAction.Search to stringResource(R.string.action_search),
            GestureAction.AllApps to stringResource(R.string.action_all_apps),
        )
        simple.forEach { (action, label) ->
            NavRow(label, selected = action == current, tag = "action_$label") {
                actions.setGesture(direction, action)
                back()
            }
        }
        NavRow(
            stringResource(R.string.action_launch_app),
            value = if (current is GestureAction.LaunchApp) actionLabel(state.gestures[direction]) else null,
            selected = current is GestureAction.LaunchApp,
            tag = "action_app",
        ) { push(SettingsRoute.PickGestureApp(direction)) }
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
                actions.setHidden(app.key, false)
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
                actions.rename(app.key, null)
            }
        }
    }
}

@Composable
internal fun SystemScreen(state: SettingsUiState, actions: SettingsActions, back: () -> Unit) {
    SettingsPage(stringResource(R.string.settings_system), back) {
        NavRow(
            stringResource(R.string.system_default),
            value = stringResource(
                if (state.isDefaultLauncher) R.string.system_default_yes else R.string.system_default_no,
            ),
            enabled = !state.isDefaultLauncher,
            tag = "system_default",
        ) { actions.requestDefaultLauncher() }
        NavRow(stringResource(R.string.system_version), value = state.versionName) {}
        SectionHeader(stringResource(R.string.system_licenses))
        NoteText(stringResource(R.string.system_licenses_text))
    }
}
