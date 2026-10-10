package com.bluecubelabs.env.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluecubelabs.env.core.model.AppFont
import com.bluecubelabs.env.core.model.AppFontWeight
import com.bluecubelabs.env.core.model.ClockFormat
import com.bluecubelabs.env.core.model.GestureAction
import com.bluecubelabs.env.core.model.GestureTuning
import com.bluecubelabs.env.core.model.HomeTextSize
import com.bluecubelabs.env.core.model.IconMode
import com.bluecubelabs.env.core.model.KeyboardFit
import com.bluecubelabs.env.core.model.PressFeedback
import com.bluecubelabs.env.core.model.SwipeDirection
import com.bluecubelabs.env.core.search.KeyboardLayouts
import com.bluecubelabs.env.core.ui.EnvFonts
import com.bluecubelabs.env.core.ui.EnvType
import com.bluecubelabs.env.core.ui.LocalEnvColors
import com.bluecubelabs.env.core.ui.RadioMark
import com.bluecubelabs.env.core.ui.SingleLineText
import com.bluecubelabs.env.core.ui.pressable
import kotlin.math.roundToInt

@Composable
internal fun RootScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    SettingsPage(stringResource(R.string.settings_title), back) {
        SpaceChips(state.envs, actions::switchEnv)
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
            stringResource(R.string.settings_envs),
            summary = stringResource(R.string.summary_envs),
            tag = "nav_envs",
        ) { push(SettingsRoute.Environments) }
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
    }
}

@Composable
private fun SpaceChips(envs: List<EnvSummary>, onSelect: (Long) -> Unit) {
    val colors = LocalEnvColors.current
    val scroll = rememberScrollState()
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = CARD_MARGIN, vertical = 8.dp)
            .testTag("space_chips"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        envs.forEach { env ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(CARD_RADIUS))
                    .background(if (env.current) colors.accent.copy(alpha = CHIP_ALPHA) else colors.surface)
                    .border(
                        1.dp,
                        if (env.current) colors.accent else colors.surface,
                        RoundedCornerShape(CARD_RADIUS),
                    )
                    .pressable(role = Role.RadioButton) { onSelect(env.id) }
                    .semantics { selected = env.current }
                    .testTag("space_chip_${env.id}")
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioMark(env.current)
                Spacer(Modifier.width(8.dp))
                SingleLineText(env.name, EnvType.style(16.sp, colors.textPrimary))
            }
        }
    }
}

@Composable
internal fun StyleScreen(state: SettingsUiState, actions: SettingsActions, back: () -> Unit) {
    val s = state.settings
    val colors = LocalEnvColors.current
    SettingsPage(stringResource(R.string.settings_style), back) {
        state.envs.firstOrNull { it.current }?.let { env ->
            SectionHeader(stringResource(R.string.style_theme, env.name))
            PaletteDropdown(env.preset) { actions.setPalette(env.id, it) }
            ToggleRow(stringResource(R.string.env_colored_icons), env.coloredIcons, tag = "style_colored_icons") {
                actions.setColoredIcons(env.id, it)
            }
            SectionHeader(stringResource(R.string.style_text))
        }
        DropdownRow(
            stringResource(R.string.style_font),
            listOf(
                AppFont.SYSTEM to stringResource(R.string.font_system),
                AppFont.INTER to "Inter",
                AppFont.OPEN_SANS to "Open Sans",
                AppFont.MONTSERRAT to "Montserrat",
                AppFont.CONDENSED to "Roboto Condensed",
            ),
            s.font,
            "font",
            style = { font ->
                TextStyle(
                    fontFamily = EnvFonts.family(font),
                    fontSize = 17.sp,
                    fontWeight = FontWeight(s.fontWeight.value),
                    color = colors.textPrimary,
                )
            },
        ) { font -> actions.updateSettings { it.copy(font = font) } }
        DropdownRow(
            stringResource(R.string.style_weight),
            listOf(
                AppFontWeight.LIGHT to stringResource(R.string.weight_light),
                AppFontWeight.REGULAR to stringResource(R.string.weight_regular),
                AppFontWeight.MEDIUM to stringResource(R.string.weight_medium),
            ),
            s.fontWeight,
            "font_weight",
        ) { weight -> actions.updateSettings { it.copy(fontWeight = weight) } }
        DropdownRow(
            stringResource(R.string.home_text_size),
            listOf(
                HomeTextSize.S to stringResource(R.string.home_text_s),
                HomeTextSize.M to stringResource(R.string.home_text_m),
                HomeTextSize.L to stringResource(R.string.home_text_l),
            ),
            s.homeTextSize,
            "text_size",
        ) { size -> actions.updateSettings { it.copy(homeTextSize = size) } }
        NoteText(stringResource(R.string.style_note))
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
        ) { mode -> actions.updateSettings { it.copy(iconMode = mode) } }
        DropdownRow(
            stringResource(R.string.home_clock),
            listOf(
                ClockFormat.SYSTEM to stringResource(R.string.home_clock_system),
                ClockFormat.H24 to stringResource(R.string.home_clock_24),
                ClockFormat.H12 to stringResource(R.string.home_clock_12),
            ),
            s.clockFormat,
            "clock",
        ) { format -> actions.updateSettings { it.copy(clockFormat = format) } }
        CardGap()
        ToggleRow(stringResource(R.string.home_show_date), s.showDate, tag = "show_date") { value ->
            actions.updateSettings { it.copy(showDate = value) }
        }
        NoteText(stringResource(R.string.home_clock_note))
        CardGap()
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
        DropdownRow(
            stringResource(R.string.settings_press),
            PressFeedback.entries.map { it to pressLabel(it) },
            state.settings.pressFeedback,
            "press",
        ) { feedback -> actions.updateSettings { it.copy(pressFeedback = feedback) } }
        NoteText(stringResource(R.string.press_note))
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
        CardGap()
        ToggleRow(stringResource(R.string.keyboard_haptics), s.keyboardHaptics, tag = "kb_haptics") { v ->
            actions.updateSettings { it.copy(keyboardHaptics = v) }
        }
        ToggleRow(stringResource(R.string.keyboard_digit_hints), s.keyboardDigitHints, tag = "kb_digits") { v ->
            actions.updateSettings { it.copy(keyboardDigitHints = v) }
        }
        SectionHeader(stringResource(R.string.keyboard_fit))
        SliderRow(
            stringResource(R.string.keyboard_width),
            s.keyboardWidth,
            KeyboardFit.WIDTH_MIN..KeyboardFit.WIDTH_MAX,
            KeyboardFit.STEP,
            tag = "kb_width",
            format = { "${(it * PERCENT).roundToInt()} %" },
        ) { v -> actions.updateSettings { it.copy(keyboardWidth = v) } }
        SliderRow(
            stringResource(R.string.keyboard_shift),
            s.keyboardShift,
            KeyboardFit.SHIFT_MIN..KeyboardFit.SHIFT_MAX,
            KeyboardFit.STEP * 2,
            tag = "kb_shift",
            format = { "${(it * PERCENT).roundToInt()}" },
        ) { v -> actions.updateSettings { it.copy(keyboardShift = v) } }
        NoteText(stringResource(R.string.keyboard_fit_note))
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
internal fun GesturesScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    val s = state.settings
    SettingsPage(stringResource(R.string.settings_gestures), back) {
        SectionHeader(stringResource(R.string.gestures_swipes))
        SwipeDirection.entries.forEach { direction ->
            val summary = state.gestures[direction]
            val current = summary?.action ?: GestureAction.None
            DropdownRow(
                directionLabel(direction),
                listOf(
                    GestureChoice.NONE to stringResource(R.string.action_none),
                    GestureChoice.NOTIFICATIONS to stringResource(R.string.action_notifications),
                    GestureChoice.SEARCH to stringResource(R.string.action_search),
                    GestureChoice.ALL_APPS to stringResource(R.string.action_all_apps),
                    GestureChoice.APP to if (current is GestureAction.LaunchApp) {
                        actionLabel(summary)
                    } else {
                        stringResource(R.string.action_launch_app)
                    },
                ),
                GestureChoice.of(current),
                "gesture_$direction",
            ) { choice ->
                when (choice) {
                    GestureChoice.APP -> push(SettingsRoute.PickGestureApp(direction))
                    else -> actions.setGesture(direction, choice.action)
                }
            }
        }
        NoteText(stringResource(R.string.gesture_shade_zone))
        SectionHeader(stringResource(R.string.keyboard_gesture_tuning))
        SliderRow(
            stringResource(R.string.keyboard_gesture_shift),
            s.gestureShiftPenalty,
            GestureTuning.SHIFT_PENALTY_MIN..GestureTuning.SHIFT_PENALTY_MAX,
            GestureTuning.STEP,
            tag = "kb_gesture_shift",
        ) { v -> actions.updateSettings { it.copy(gestureShiftPenalty = v) } }
        NoteText(stringResource(R.string.keyboard_gesture_shift_note))
        SliderRow(
            stringResource(R.string.keyboard_gesture_margin),
            s.gestureSuggestMargin,
            GestureTuning.SUGGEST_MARGIN_MIN..GestureTuning.SUGGEST_MARGIN_MAX,
            GestureTuning.STEP,
            tag = "kb_gesture_margin",
        ) { v -> actions.updateSettings { it.copy(gestureSuggestMargin = v) } }
        NoteText(stringResource(R.string.keyboard_gesture_margin_note))
        NavRow(stringResource(R.string.keyboard_gesture_tuning_reset), tag = "kb_gesture_tuning_reset") {
            actions.updateSettings {
                it.copy(
                    gestureShiftPenalty = GestureTuning.SHIFT_PENALTY,
                    gestureSuggestMargin = GestureTuning.SUGGEST_MARGIN,
                )
            }
        }
    }
}

internal enum class GestureChoice(val action: GestureAction) {
    NONE(GestureAction.None),
    NOTIFICATIONS(GestureAction.Notifications),
    SEARCH(GestureAction.Search),
    ALL_APPS(GestureAction.AllApps),
    APP(GestureAction.None),
    ;

    companion object {
        fun of(action: GestureAction): GestureChoice = when (action) {
            GestureAction.None -> NONE
            GestureAction.Notifications -> NOTIFICATIONS
            GestureAction.Search -> SEARCH
            GestureAction.AllApps -> ALL_APPS
            is GestureAction.LaunchApp -> APP
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
        ) { actions.requestDefaultLauncher() }
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
private const val PERCENT = 100
