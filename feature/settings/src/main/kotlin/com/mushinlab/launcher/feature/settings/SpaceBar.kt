package com.mushinlab.launcher.feature.settings

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.SpaceLimits
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.ui.AnchoredPanel
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.PanelItem
import com.mushinlab.launcher.core.ui.RadioMark
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable
import kotlinx.coroutines.launch

@Stable
internal class SpaceBarState {
    var creating by mutableStateOf(false)
    var menu by mutableStateOf<SpaceMenu?>(null)
    var deleting by mutableStateOf<SpaceSummary?>(null)
}

@Immutable
internal data class SpaceMenu(val space: SpaceSummary, val anchor: Offset)

internal fun creatableTypes(spaces: List<SpaceSummary>): List<SpaceType> = listOfNotNull(
    SpaceType.CUSTOM.takeIf { spaces.count { it.type == SpaceType.CUSTOM } < SpaceLimits.MAX_CUSTOM },
    SpaceType.WORK.takeIf { spaces.none { it.type == SpaceType.WORK } },
)

@Composable
internal fun SpaceBar(spaces: List<SpaceSummary>, actions: SpaceActions, bar: SpaceBarState) {
    val view = LocalView.current
    val reject = { view.performHapticFeedback(HapticFeedbackConstants.REJECT) }
    Row(Modifier.fillMaxWidth().padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f, fill = false)
                .horizontalScroll(rememberScrollState())
                .padding(start = CARD_MARGIN, end = 4.dp, top = 8.dp, bottom = 8.dp)
                .testTag("space_chips"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            spaces.forEach { space ->
                SpaceChip(space, onSelect = { actions.switchSpace(space.id) }) { anchor ->
                    if (space.type == SpaceType.NORMAL) reject() else bar.menu = SpaceMenu(space, anchor)
                }
            }
        }
        GlyphButton(
            Glyph.ADD,
            stringResource(R.string.space_create_title),
            LocalMushinColors.current.textPrimary,
            tag = "settings_add_space",
        ) {
            if (creatableTypes(spaces).isEmpty()) reject() else bar.creating = true
        }
    }
}

@Composable
private fun SpaceChip(space: SpaceSummary, onSelect: () -> Unit, onHold: (Offset) -> Unit) {
    val colors = LocalMushinColors.current
    var anchor by remember { mutableStateOf(Offset.Zero) }
    Row(
        Modifier
            .onGloballyPositioned { anchor = it.boundsInWindow().bottomCenter }
            .clip(RoundedCornerShape(CARD_RADIUS))
            .background(if (space.current) colors.accent.copy(alpha = CHIP_ALPHA) else colors.surface)
            .border(1.dp, if (space.current) colors.accent else colors.surface, RoundedCornerShape(CARD_RADIUS))
            .pressable(role = Role.RadioButton, onLongClick = { onHold(anchor) }, onClick = onSelect)
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

@Composable
internal fun SpaceBarOverlays(spaces: List<SpaceSummary>, actions: SpaceActions, bar: SpaceBarState) {
    val types = creatableTypes(spaces)
    if (bar.creating && types.isNotEmpty()) CreateSpaceDialog(types, actions) { bar.creating = false }
    bar.menu?.let { menu ->
        AnchoredPanel(anchor = menu.anchor, onDismiss = { bar.menu = null }) {
            PanelItem(stringResource(R.string.space_delete), tag = "space_delete") {
                bar.menu = null
                bar.deleting = menu.space
            }
        }
    }
    bar.deleting?.let { space ->
        DeleteSpaceDialog(space, onConfirm = { actions.deleteSpace(space.id) }, onDismiss = { bar.deleting = null })
    }
}

@Composable
private fun CreateSpaceDialog(types: List<SpaceType>, actions: SpaceActions, onClose: () -> Unit) {
    var chosen by remember { mutableStateOf(types.first()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val type = chosen.takeIf { it in types } ?: types.first()
    val options: (@Composable ColumnScope.() -> Unit)? = if (types.size > 1) {
        {
            SpaceTypeChoice(types, type) {
                chosen = it
                error = null
            }
        }
    } else {
        null
    }
    key(type) {
        TextInputDialog(
            title = stringResource(R.string.space_create_title),
            initial = if (type == SpaceType.WORK) stringResource(R.string.space_work_default_name) else "",
            maxLength = SpaceLimits.NAME_MAX_LENGTH + 8,
            error = spaceErrorText(error),
            confirm = stringResource(R.string.settings_save),
            options = options,
            onConfirm = { name ->
                scope.launch {
                    val result = actions.createSpace(name, type)
                    if (result == null) onClose() else error = result
                }
            },
            onDismiss = onClose,
        )
    }
}

@Composable
private fun SpaceTypeChoice(types: List<SpaceType>, selected: SpaceType, onSelect: (SpaceType) -> Unit) {
    val colors = LocalMushinColors.current
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        types.forEach { type ->
            Row(
                Modifier
                    .heightIn(min = 48.dp)
                    .pressable(role = Role.RadioButton) { onSelect(type) }
                    .semantics { this.selected = type == selected }
                    .testTag("space_type_${type.name}")
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioMark(type == selected)
                Spacer(Modifier.width(8.dp))
                SingleLineText(
                    spaceTypeLabel(type).replaceFirstChar { it.titlecase() },
                    MushinType.style(16.sp, colors.textPrimary),
                )
            }
        }
    }
}

@Composable
private fun DeleteSpaceDialog(space: SpaceSummary, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    SystemConfirmDialog(
        title = stringResource(R.string.space_delete),
        message = stringResource(R.string.space_delete_confirm, space.name),
        confirm = stringResource(R.string.settings_delete),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

private const val CHIP_ALPHA = 0.16f
