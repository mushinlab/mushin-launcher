package com.bluecubelabs.env.feature.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluecubelabs.env.core.model.GestureAction
import com.bluecubelabs.env.core.model.SwipeDirection
import com.bluecubelabs.env.core.search.SearchEngine
import com.bluecubelabs.env.core.search.SearchIndex
import com.bluecubelabs.env.core.search.SearchItem
import com.bluecubelabs.env.core.ui.EnvType
import com.bluecubelabs.env.core.ui.HorizontalDivider
import com.bluecubelabs.env.core.ui.LocalEnvColors

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
            .border(1.dp, colors.divider, RoundedCornerShape(6.dp))
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
    val existing = state.envItems[envId].orEmpty().map { it.key }
    var selected by remember(envId) { mutableStateOf(existing) }
    var query by remember { mutableStateOf("") }
    val visible = state.apps.filter { !it.hidden || it.key in existing }
    val limit = state.capacity.takeIf { it > 0 } ?: Int.MAX_VALUE
    SettingsPage(stringResource(R.string.picker_title), back, scrollable = false) {
        FilterField(query) { query = it }
        NavRow(
            stringResource(R.string.settings_done),
            value = if (limit !=
                Int.MAX_VALUE
            ) {
                stringResource(R.string.picker_selected, selected.size, limit)
            } else {
                null
            },
            tag = "picker_done",
        ) {
            val kept = existing.filter { it in selected }
            actions.setEnvItems(envId, kept + selected.filterNot { it in existing })
            back()
        }
        HorizontalDivider()
        LazyColumn(Modifier.weight(1f)) {
            items(filterApps(visible, query), key = { it.key.id }) { app ->
                val checked = app.key in selected
                NavRow(
                    app.label,
                    value = if (checked) "✓" else null,
                    enabled = checked || selected.size < limit,
                    tag = "pick_${app.label}",
                ) {
                    selected = if (checked) selected - app.key else selected + app.key
                }
            }
        }
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
    SettingsPage(directionLabel(direction), done, scrollable = false) {
        FilterField(query) { query = it }
        LazyColumn(Modifier.weight(1f)) {
            items(filterApps(state.apps.filterNot { it.hidden }, query), key = { it.key.id }) { app ->
                NavRow(app.label, tag = "pick_${app.label}") {
                    actions.setGesture(direction, GestureAction.LaunchApp(app.key))
                    done()
                }
            }
        }
    }
}
