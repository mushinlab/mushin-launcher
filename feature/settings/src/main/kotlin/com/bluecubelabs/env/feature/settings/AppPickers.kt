package com.bluecubelabs.env.feature.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.GestureAction
import com.bluecubelabs.env.core.model.SwipeDirection
import com.bluecubelabs.env.core.search.SearchEngine
import com.bluecubelabs.env.core.search.SearchIndex
import com.bluecubelabs.env.core.search.SearchItem
import com.bluecubelabs.env.core.ui.EnvType
import com.bluecubelabs.env.core.ui.LocalEnvColors
import com.bluecubelabs.env.core.ui.SingleLineText
import com.bluecubelabs.env.core.ui.pressable

@Composable
private fun FilterField(value: String, onChange: (String) -> Unit) {
    val colors = LocalEnvColors.current
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = EnvType.style(18.sp, colors.textPrimary),
        cursorBrush = SolidColor(colors.accent),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                androidx.compose.foundation.text.BasicText(
                    stringResource(R.string.picker_filter),
                    style = EnvType.style(18.sp, colors.textSecondary),
                )
            }
            inner()
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.dp, colors.divider, RoundedCornerShape(CARD_RADIUS))
            .padding(12.dp)
            .testTag("picker_filter"),
    )
}

private fun filterApps(apps: List<PickerApp>, query: String): List<PickerApp> {
    if (query.isBlank()) return apps
    val index = SearchIndex.build(apps.map { SearchItem(it.key, it.label) })
    val byKey = apps.associateBy { it.key }
    return SearchEngine().search(index, query, limit = apps.size).mapNotNull { byKey[it.key] }
}

@Composable
internal fun EnvAppPicker(envId: Long, state: SettingsUiState, actions: SettingsActions, back: () -> Unit) {
    val existing = state.envItems[envId].orEmpty().map { it.key }.toSet()
    var pending by remember(envId) { mutableStateOf(emptyMap<AppKey, Boolean>()) }
    LaunchedEffect(existing) { pending = pending.filter { (key, added) -> (key in existing) != added } }
    val selected = existing.filter { pending[it] != false }.toSet() + pending.filterValues { it }.keys
    var query by remember { mutableStateOf("") }
    val visible = state.apps.filter { !it.hidden || it.key in existing }
    val limit = state.capacity.takeIf { it > 0 } ?: Int.MAX_VALUE
    val full = selected.size >= limit
    SettingsPage(stringResource(R.string.picker_title), back, scrollable = false) {
        FilterField(query) { query = it }
        if (limit != Int.MAX_VALUE) {
            NoteText(
                stringResource(if (full) R.string.picker_full else R.string.picker_selected, selected.size, limit),
                if (full) LocalEnvColors.current.accent else LocalEnvColors.current.textSecondary,
            )
        }
        CardGap()
        LazyColumn(Modifier.weight(1f)) {
            items(filterApps(visible, query), key = { it.key.id }) { app ->
                val checked = app.key in selected
                PickerRow(app.label, checked, enabled = checked || !full, tag = "pick_${app.label}") {
                    pending = pending + (app.key to !checked)
                    if (checked) actions.removeEnvItem(envId, app.key) else actions.addEnvItem(envId, app.key)
                }
            }
        }
    }
}

@Composable
private fun PickerRow(label: String, selected: Boolean, enabled: Boolean, tag: String, onClick: () -> Unit) {
    val colors = LocalEnvColors.current
    Box(
        Modifier
            .settingsCard()
            .heightIn(min = 56.dp)
            .selectedBackground(selected)
            .pressable(enabled = enabled, onClick = onClick)
            .testTag(tag)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = CARD_PADDING, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        SingleLineText(label, EnvType.style(17.sp, colors.textPrimary))
    }
}

@Composable
internal fun GestureAppPicker(
    direction: SwipeDirection,
    state: SettingsUiState,
    actions: SettingsActions,
    done: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val current = (state.gestures[direction]?.action as? GestureAction.LaunchApp)?.key
    SettingsPage(directionLabel(direction), done, scrollable = false) {
        FilterField(query) { query = it }
        LazyColumn(Modifier.weight(1f)) {
            items(filterApps(state.apps.filterNot { it.hidden }, query), key = { it.key.id }) { app ->
                NavRow(app.label, selected = app.key == current, tag = "pick_${app.label}") {
                    actions.setGesture(direction, GestureAction.LaunchApp(app.key))
                    done()
                }
            }
        }
    }
}
