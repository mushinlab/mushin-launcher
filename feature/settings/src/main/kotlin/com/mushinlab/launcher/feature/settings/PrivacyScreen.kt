package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.pressable

@Composable
internal fun PrivacyScreen(state: SettingsUiState, actions: SettingsActions, back: () -> Unit) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    SettingsPage(stringResource(R.string.system_privacy), back) {
        DangerRow(stringResource(R.string.privacy_delete_memory), "privacy_delete_memory") { confirm = true }
        NoteText(stringResource(R.string.privacy_delete_memory_note))
        NoteText(stringResource(R.string.system_privacy_text))
        ExpandRow(stringResource(R.string.privacy_data), expanded, "privacy_data") { expanded = !expanded }
        if (expanded) StoredData(state)
    }
    if (confirm) {
        SystemConfirmDialog(
            title = stringResource(R.string.privacy_delete_memory),
            message = stringResource(R.string.privacy_delete_memory_confirm),
            confirm = stringResource(R.string.memory_erase),
            onConfirm = {
                actions.memory.clearUsage()
                actions.memory.forgetAllGestures()
            },
            onDismiss = { confirm = false },
        )
    }
}

@Composable
private fun StoredData(state: SettingsUiState) {
    SectionHeader(stringResource(R.string.memory_usage))
    NoteText(stringResource(R.string.memory_usage_note))
    if (state.usage.isEmpty()) NoteText(stringResource(R.string.memory_usage_none))
    state.usage.forEach { row -> ValueRow(row.label, row.launches.toString(), "memory_usage_${row.label}") }
    state.spaces.firstOrNull { it.current }?.let { space ->
        SectionHeader(stringResource(R.string.memory_usefulness, space.name))
        NoteText(stringResource(R.string.memory_usefulness_note))
        if (state.usefulness.isEmpty()) NoteText(stringResource(R.string.memory_usage_none))
        state.usefulness.forEach { row -> ValueRow(row.label, row.score.toString(), "memory_score_${row.label}") }
    }
}

@Composable
private fun DangerRow(title: String, tag: String, onClick: () -> Unit) {
    Row(
        Modifier
            .settingsCard()
            .heightIn(min = 56.dp)
            .pressable(onClick = onClick)
            .testTag(tag)
            .padding(horizontal = CARD_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(title, style = MushinType.style(17.sp, DANGER, FontWeight.Medium))
    }
}

@Composable
private fun ExpandRow(title: String, expanded: Boolean, tag: String, onClick: () -> Unit) {
    val colors = LocalMushinColors.current
    Row(
        Modifier
            .settingsCard()
            .heightIn(min = 56.dp)
            .pressable(role = Role.DropdownList, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = CARD_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(title, style = MushinType.style(17.sp, colors.textPrimary), modifier = Modifier.weight(1f))
        BasicText(if (expanded) "▴" else "▾", style = MushinType.style(17.sp, colors.textSecondary))
    }
}

private val DANGER = Color(0xFFE5534B)
