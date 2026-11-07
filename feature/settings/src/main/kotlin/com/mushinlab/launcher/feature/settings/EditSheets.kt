package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType

@Composable
fun EditItemSheet(label: String, onClose: () -> Unit, onRemove: () -> Unit, onReplace: () -> Unit) {
    SettingsSheet(
        "edit_item_sheet",
        onClose,
        scrimAlpha = HOME_SHEET_SCRIM,
        color = LocalMushinColors.current.background,
    ) {
        BasicText(
            label,
            style = MushinType.style(20.sp, LocalMushinColors.current.textPrimary),
            modifier = Modifier.padding(horizontal = CARD_MARGIN + CARD_PADDING, vertical = 8.dp),
        )
        NavRow(stringResource(R.string.edit_remove_from_list), tag = "edit_item_remove", onClick = onRemove)
        NavRow(stringResource(R.string.edit_replace), tag = "edit_item_replace", onClick = onReplace)
        Spacer(Modifier.height(8.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
fun EditTipsSheet(onClose: () -> Unit) {
    HomeSheet(stringResource(R.string.edit_tips), "edit_tips_sheet", onClose) {
        SectionHeader(stringResource(R.string.edit_tip_list_title))
        NoteText(stringResource(R.string.edit_tip_list))
        SectionHeader(stringResource(R.string.edit_tip_widgets_title))
        NoteText(stringResource(R.string.edit_tip_widgets))
        SectionHeader(stringResource(R.string.edit_tip_font_title))
        NoteText(stringResource(R.string.edit_tip_font))
        SectionHeader(stringResource(R.string.edit_tip_theme_title))
        NoteText(stringResource(R.string.edit_tip_theme))
    }
}
