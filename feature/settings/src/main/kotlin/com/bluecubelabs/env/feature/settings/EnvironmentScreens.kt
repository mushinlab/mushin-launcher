package com.bluecubelabs.env.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluecubelabs.env.core.model.EnvLimits
import com.bluecubelabs.env.core.model.EnvType
import com.bluecubelabs.env.core.model.PalettePreset
import com.bluecubelabs.env.core.model.Palettes
import com.bluecubelabs.env.core.ui.EnvColors
import com.bluecubelabs.env.core.ui.EnvType as Type
import com.bluecubelabs.env.core.ui.HorizontalDivider
import com.bluecubelabs.env.core.ui.LocalEnvColors
import com.bluecubelabs.env.core.ui.SingleLineText
import com.bluecubelabs.env.core.ui.pressable
import kotlinx.coroutines.launch

@Composable
internal fun envTypeLabel(type: EnvType): String = stringResource(
    when (type) {
        EnvType.NORMAL -> R.string.env_type_normal
        EnvType.WORK -> R.string.env_type_work
        EnvType.CUSTOM -> R.string.env_type_custom
    },
)

@Composable
internal fun envErrorText(code: String?): String? = when (code) {
    null -> null
    ERR_EMPTY -> stringResource(R.string.error_name_empty)
    ERR_LONG -> stringResource(R.string.error_name_long)
    ERR_TAKEN -> stringResource(R.string.error_name_taken)
    ERR_WORK -> stringResource(R.string.error_work_limit)
    ERR_CUSTOM -> stringResource(R.string.error_custom_limit)
    else -> stringResource(R.string.error_generic)
}

const val ERR_EMPTY = "empty"
const val ERR_LONG = "long"
const val ERR_TAKEN = "taken"
const val ERR_WORK = "work"
const val ERR_CUSTOM = "custom"

@Composable
internal fun EnvironmentsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    var createType by remember { mutableStateOf<EnvType?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val hasWork = state.envs.any { it.type == EnvType.WORK }
    val customCount = state.envs.count { it.type == EnvType.CUSTOM }
    Box {
        SettingsPage(stringResource(R.string.settings_envs), back) {
            state.envs.forEach { env ->
                val suffix = if (env.current) " · ${stringResource(R.string.env_current)}" else ""
                NavRow(env.name, value = envTypeLabel(env.type) + suffix, tag = "env_${env.id}") {
                    push(SettingsRoute.EnvEditor(env.id))
                }
            }
            HorizontalDivider()
            NavRow(
                stringResource(R.string.env_create),
                enabled = customCount < EnvLimits.MAX_CUSTOM,
                tag = "env_create",
            ) {
                error = null
                createType = EnvType.CUSTOM
            }
            if (!hasWork) {
                NavRow(stringResource(R.string.env_create_work), tag = "env_create_work") {
                    error = null
                    createType = EnvType.WORK
                }
            }
        }
        val type = createType
        if (type != null) {
            TextInputDialog(
                title = stringResource(R.string.env_create_title),
                initial = if (type == EnvType.WORK) stringResource(R.string.env_work_default_name) else "",
                maxLength = EnvLimits.NAME_MAX_LENGTH + 8,
                error = envErrorText(error),
                confirm = stringResource(R.string.settings_save),
                onConfirm = { name ->
                    scope.launch {
                        val result = actions.createEnv(name, type)
                        if (result == null) createType = null else error = result
                    }
                },
                onDismiss = { createType = null },
            )
        }
    }
}

@Composable
internal fun EnvEditorScreen(
    envId: Long,
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    val env = state.envs.firstOrNull { it.id == envId }
    if (env == null) {
        back()
        return
    }
    var renaming by remember { mutableStateOf(false) }
    var renameError by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Box {
        SettingsPage(env.name, back) {
            NavRow(stringResource(R.string.env_name), value = env.name, tag = "env_rename") {
                renameError = null
                renaming = true
            }
            SectionHeader(stringResource(R.string.env_palette))
            PalettePicker(env.preset) { actions.setPalette(envId, it) }
            SectionHeader(stringResource(R.string.env_items))
            EnvItemsSection(envId, state, actions, push)
            HorizontalDivider()
            NavRow(stringResource(R.string.env_delete), enabled = env.type != EnvType.NORMAL, tag = "env_delete") {
                deleting = true
            }
        }
        if (renaming) {
            TextInputDialog(
                title = stringResource(R.string.env_rename_title),
                initial = env.name,
                maxLength = EnvLimits.NAME_MAX_LENGTH + 8,
                error = envErrorText(renameError),
                confirm = stringResource(R.string.settings_save),
                onConfirm = { name ->
                    scope.launch {
                        val result = actions.renameEnv(envId, name)
                        if (result == null) renaming = false else renameError = result
                    }
                },
                onDismiss = { renaming = false },
            )
        }
        if (deleting) {
            ConfirmDialog(
                message = stringResource(R.string.env_delete_confirm, env.name),
                confirm = stringResource(R.string.settings_delete),
                onConfirm = {
                    deleting = false
                    actions.deleteEnv(envId)
                    back()
                },
                onDismiss = { deleting = false },
            )
        }
    }
}

@Composable
internal fun EnvAppsScreen(
    envId: Long,
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    val env = state.envs.firstOrNull { it.id == envId }
    if (env == null) {
        back()
        return
    }
    SettingsPage("${env.name} · ${stringResource(R.string.env_items)}", back) {
        EnvItemsSection(envId, state, actions, push)
    }
}

@Composable
private fun EnvItemsSection(
    envId: Long,
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
) {
    val items = state.envItems[envId].orEmpty()
    if (items.size > state.capacity && state.capacity > 0) {
        NoteText(
            stringResource(R.string.env_overflow, state.capacity, items.size),
            LocalEnvColors.current.accent,
        )
    }
    if (items.isEmpty()) NoteText(stringResource(R.string.env_empty))
    ReorderableItems(items, onChange = { keys -> actions.setEnvItems(envId, keys) })
    NavRow(stringResource(R.string.env_add), tag = "env_add") { push(SettingsRoute.PickApps(envId)) }
}

@Composable
private fun PalettePicker(selected: PalettePreset, onSelect: (PalettePreset) -> Unit) {
    val names = mapOf(
        PalettePreset.GRAPHITE to stringResource(R.string.palette_graphite),
        PalettePreset.NAVY to stringResource(R.string.palette_navy),
        PalettePreset.PAPER to stringResource(R.string.palette_paper),
        PalettePreset.MOSS to stringResource(R.string.palette_moss),
    )
    Palettes.presets.forEach { (preset, palette) ->
        val swatch = EnvColors.from(palette)
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .selectedBackground(preset == selected)
                .pressable { onSelect(preset) }
                .testTag("palette_$preset")
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(28.dp).background(swatch.background))
            Box(Modifier.size(28.dp).background(swatch.zebraB))
            Box(Modifier.size(28.dp).background(swatch.textPrimary))
            Box(Modifier.size(28.dp).background(swatch.accent))
            Spacer(Modifier.width(16.dp))
            BasicText(
                names[preset].orEmpty(),
                style = Type.style(17.sp, LocalEnvColors.current.textPrimary),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ReorderableItems(
    items: List<EditorItem>,
    onChange: (List<com.bluecubelabs.env.core.model.AppKey>) -> Unit,
) {
    val colors = LocalEnvColors.current
    val density = LocalDensity.current
    val rowHeightPx = with(density) { ROW_HEIGHT.toPx() }
    var order by remember(items) { mutableStateOf(items) }
    var dragKey by remember { mutableStateOf<com.bluecubelabs.env.core.model.AppKey?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val currentOrder by rememberUpdatedState(order)
    val moveUp = stringResource(R.string.env_move_up)
    val moveDown = stringResource(R.string.env_move_down)
    fun move(from: Int, to: Int): List<EditorItem> {
        val list = currentOrder.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        order = list
        return list
    }
    order.forEachIndexed { index, item ->
        key(item.key.id) {
            val dragging = dragKey == item.key
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(ROW_HEIGHT)
                    .graphicsLayer { translationY = if (dragging) dragOffset else 0f }
                    .background(if (dragging) colors.accent.copy(alpha = DRAG_ALPHA) else colors.zebra(index))
                    .semantics {
                        customActions = listOfNotNull(
                            if (index > 0) {
                                CustomAccessibilityAction(moveUp) {
                                    onChange(move(index, index - 1).map { it.key })
                                    true
                                }
                            } else {
                                null
                            },
                            if (index < order.lastIndex) {
                                CustomAccessibilityAction(moveDown) {
                                    onChange(move(index, index + 1).map { it.key })
                                    true
                                }
                            } else {
                                null
                            },
                        )
                    }
                    .testTag("editor_item_$index"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(48.dp).pressable {
                        val next = currentOrder.filterNot { it.key == item.key }
                        order = next
                        onChange(next.map { it.key })
                    }.testTag("editor_remove_$index"),
                    contentAlignment = Alignment.Center,
                ) { BasicText("−", style = Type.style(22.sp, colors.textPrimary)) }
                val note = when {
                    item.missing -> " · ${stringResource(R.string.env_item_missing)}"
                    item.hidden -> " · ${stringResource(R.string.env_item_hidden)}"
                    else -> ""
                }
                SingleLineText(item.label + note, Type.style(17.sp, colors.textPrimary), Modifier.weight(1f))
                Box(
                    Modifier
                        .size(48.dp)
                        .testTag("editor_handle_$index")
                        .pointerInput(item.key) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                if (!awaitHold(down.id)) return@awaitEachGesture
                                dragKey = item.key
                                dragOffset = 0f
                                dragUntilUp(down.id) { dy ->
                                    dragOffset += dy
                                    val from = currentOrder.indexOfFirst { it.key == item.key }
                                    if (dragOffset > rowHeightPx / 2 && from < currentOrder.lastIndex) {
                                        move(from, from + 1)
                                        dragOffset -= rowHeightPx
                                    } else if (dragOffset < -rowHeightPx / 2 && from > 0) {
                                        move(from, from - 1)
                                        dragOffset += rowHeightPx
                                    }
                                }
                                dragKey = null
                                dragOffset = 0f
                                onChange(currentOrder.map { it.key })
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) { BasicText("≡", style = Type.style(22.sp, colors.textSecondary)) }
            }
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitHold(id: PointerId): Boolean = withTimeoutOrNull(HANDLE_HOLD_MS) {
    var released = false
    while (!released) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == id }
        released = change == null || !change.pressed
    }
} == null

private suspend fun AwaitPointerEventScope.dragUntilUp(id: PointerId, onDelta: (Float) -> Unit) {
    var change = awaitPointerEvent().changes.firstOrNull { it.id == id }
    while (change != null && change.pressed) {
        onDelta(change.positionChange().y)
        change.consume()
        change = awaitPointerEvent().changes.firstOrNull { it.id == id }
    }
}

private val ROW_HEIGHT = 56.dp
private const val DRAG_ALPHA = 0.12f
private const val HANDLE_HOLD_MS = 200L
