package com.mushinlab.launcher.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

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
