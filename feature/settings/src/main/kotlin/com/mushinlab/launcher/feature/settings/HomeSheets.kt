package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.AppFont
import com.mushinlab.launcher.core.model.AppFontWeight
import com.mushinlab.launcher.core.model.ClockFormat
import com.mushinlab.launcher.core.model.HomeTextAlign
import com.mushinlab.launcher.core.model.HomeTextSize
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.PressFeedback
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinFonts
import com.mushinlab.launcher.core.ui.MushinType

@Composable
internal fun ClockSheet(state: SettingsUiState, actions: SettingsActions, back: () -> Unit) {
    val s = state.settings
    val preferences = actions.preferences
    HomeSheet(stringResource(R.string.clock_sheet), "clock_sheet", back) {
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
        ToggleRow(stringResource(R.string.home_show_date), s.showDate, tag = "show_date") { value ->
            preferences.updateSettings { it.copy(showDate = value) }
        }
        NoteText(stringResource(R.string.home_clock_note))
    }
}

@Composable
internal fun TextSheet(state: SettingsUiState, actions: SettingsActions, back: () -> Unit) {
    HomeSheet(stringResource(R.string.style_text), "text_sheet", back) {
        TextStyleRows(state.settings, actions.preferences)
        TextAlignRow(state.settings, actions.preferences)
        NoteText(stringResource(R.string.style_note))
    }
}

@Composable
internal fun ThemeSheet(state: SettingsUiState, actions: SettingsActions, back: () -> Unit) {
    val space = state.spaces.firstOrNull { it.current }
    if (space == null) {
        LaunchedEffect(Unit) { back() }
        return
    }
    val preferences = actions.preferences
    HomeSheet(stringResource(R.string.style_theme, space.name), "theme_sheet", back) {
        PalettePicker(space.preset) { actions.spaces.setPalette(space.id, it) }
        val icons = state.settings.iconMode != IconMode.NONE
        ToggleRow(stringResource(R.string.home_icons), icons, tag = "theme_icons") { on ->
            preferences.updateSettings { it.copy(iconMode = if (on) IconMode.RIGHT else IconMode.NONE) }
        }
        ToggleRow(stringResource(R.string.space_colored_icons), space.coloredIcons, tag = "theme_colored_icons") {
            actions.spaces.setColoredIcons(space.id, it)
        }
        NoteText(stringResource(R.string.space_colored_icons_note))
        NotificationRows(state, actions)
        DropdownRow(
            stringResource(R.string.settings_press),
            PressFeedback.entries.map { it to pressLabel(it) },
            state.settings.pressFeedback,
            "press",
        ) { feedback -> preferences.updateSettings { it.copy(pressFeedback = feedback) } }
        NoteText(stringResource(R.string.press_note))
    }
}

@Composable
private fun NotificationRows(state: SettingsUiState, actions: SettingsActions) {
    val counts = state.settings.notificationCounts
    ToggleRow(stringResource(R.string.home_notifications), counts, tag = "notification_counts") { on ->
        actions.preferences.updateSettings { it.copy(notificationCounts = on) }
        if (on && !state.notificationAccess) actions.system.openNotificationAccess()
    }
    if (counts && !state.notificationAccess) {
        NoteText(stringResource(R.string.home_notifications_no_access), LocalMushinColors.current.accent)
        NavRow(stringResource(R.string.home_notifications_grant), tag = "notification_access") {
            actions.system.openNotificationAccess()
        }
    } else {
        NoteText(stringResource(R.string.home_notifications_note))
    }
}

@Composable
private fun HomeSheet(title: String, tag: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalMushinColors.current
    SettingsSheet(tag = tag, onClose = onClose, scrimAlpha = HOME_SHEET_SCRIM, color = colors.background) {
        BasicText(
            title,
            style = MushinType.style(20.sp, colors.textPrimary, FontWeight.Medium),
            modifier = Modifier.padding(horizontal = CARD_MARGIN + CARD_PADDING),
        )
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState()),
        ) {
            content()
        }
        Spacer(Modifier.height(8.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun TextStyleRows(s: LauncherSettings, preferences: PreferenceActions) {
    val colors = LocalMushinColors.current
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
                fontFamily = MushinFonts.family(font),
                fontSize = 17.sp,
                fontWeight = FontWeight(s.fontWeight.value),
                color = colors.textPrimary,
            )
        },
    ) { font -> preferences.updateSettings { it.copy(font = font) } }
    DropdownRow(
        stringResource(R.string.style_weight),
        listOf(
            AppFontWeight.LIGHT to stringResource(R.string.weight_light),
            AppFontWeight.REGULAR to stringResource(R.string.weight_regular),
            AppFontWeight.MEDIUM to stringResource(R.string.weight_medium),
        ),
        s.fontWeight,
        "font_weight",
    ) { weight -> preferences.updateSettings { it.copy(fontWeight = weight) } }
    DropdownRow(
        stringResource(R.string.home_text_size),
        listOf(
            HomeTextSize.S to stringResource(R.string.home_text_s),
            HomeTextSize.M to stringResource(R.string.home_text_m),
            HomeTextSize.L to stringResource(R.string.home_text_l),
        ),
        s.homeTextSize,
        "text_size",
    ) { size -> preferences.updateSettings { it.copy(homeTextSize = size) } }
}

@Composable
private fun TextAlignRow(s: LauncherSettings, preferences: PreferenceActions) {
    DropdownRow(
        stringResource(R.string.home_text_align),
        listOf(
            HomeTextAlign.START to stringResource(R.string.home_text_align_start),
            HomeTextAlign.CENTER to stringResource(R.string.home_text_align_center),
            HomeTextAlign.END to stringResource(R.string.home_text_align_end),
        ),
        s.homeTextAlign,
        "text_align",
    ) { align -> preferences.updateSettings { it.copy(homeTextAlign = align) } }
}

private const val HOME_SHEET_SCRIM = 0.15f
