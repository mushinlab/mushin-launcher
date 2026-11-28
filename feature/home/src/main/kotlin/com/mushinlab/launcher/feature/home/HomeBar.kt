package com.mushinlab.launcher.feature.home

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.Motion
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable
import com.mushinlab.launcher.feature.home.gesture.RowRegistry

@Composable
internal fun HomeBar(state: HomeUiState, editing: Boolean, registry: RowRegistry, actions: HomeActions) {
    val suggestion = state.suggestion
    val colors = LocalMushinColors.current
    val toggle by animateDpAsState(
        if (editing) DONE_WIDTH else EDIT_SIZE,
        tween(Motion.TRANSITION_MS),
        label = "toggle",
    )
    var bounds by remember { mutableStateOf<Rect?>(null) }
    DisposableEffect(Unit) { onDispose { registry.bar.set(null) } }
    Row(
        Modifier
            .fillMaxWidth()
            .height(HomeDimens.barHeight)
            .onGloballyPositioned {
                bounds = it.boundsInRoot()
                registry.bar.set(bounds)
            }
            .testTag("home_bar")
            .padding(horizontal = BAR_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarStart(state, editing, registry, actions, Modifier.weight(1f)) {
            actions.onOpenSpaces(bounds?.topCenter ?: Offset.Zero)
        }
        if (suggestion != null && !editing) {
            var anchor by remember { mutableStateOf(Offset.Zero) }
            DisposableEffect(Unit) { onDispose { registry.block(BAR_SUGGEST, null) } }
            GlyphButton(
                Glyph.HELP,
                stringResource(R.string.home_suggestion),
                colors.accent,
                tag = "home_suggestion",
                modifier = Modifier.onGloballyPositioned {
                    val bounds = it.boundsInRoot()
                    anchor = bounds.topLeft
                    registry.block(BAR_SUGGEST, bounds)
                },
            ) { actions.onSuggestion(anchor) }
        }
        Spacer(Modifier.width(toggle))
    }
}

@Composable
private fun BarStart(
    state: HomeUiState,
    editing: Boolean,
    registry: RowRegistry,
    actions: HomeActions,
    modifier: Modifier,
    onSpaces: () -> Unit,
) {
    val colors = LocalMushinColors.current
    val tap = if (editing) {
        Modifier
    } else {
        Modifier.semantics(mergeDescendants = true) {
            role = Role.Button
            onClick {
                onSpaces()
                true
            }
        }
    }
    Box(modifier.then(tap), contentAlignment = Alignment.CenterStart) {
        AnimatedVisibility(
            visible = editing,
            enter = fadeIn(tween(Motion.TRANSITION_MS)),
            exit = fadeOut(tween(Motion.TRANSITION_MS)),
        ) {
            EditTools(registry, actions)
        }
        AnimatedVisibility(
            visible = !editing,
            enter = fadeIn(tween(Motion.TRANSITION_MS)),
            exit = fadeOut(tween(Motion.TRANSITION_MS)),
        ) {
            Row(Modifier.padding(start = SPACE_PADDING), verticalAlignment = Alignment.CenterVertically) {
                SingleLineText(
                    state.spaceName,
                    MushinType.style(MushinType.secondarySize, colors.textSecondary),
                    Modifier.weight(1f, fill = false).testTag("home_space"),
                )
                if (state.spaceCount > 1 && state.spaceIndex >= 0) SpaceDots(state.spaceCount, state.spaceIndex)
            }
        }
    }
}

@Composable
private fun SpaceDots(count: Int, current: Int) {
    val colors = LocalMushinColors.current
    Row(Modifier.padding(start = DOTS_GAP), horizontalArrangement = Arrangement.spacedBy(DOT_GAP)) {
        repeat(count) { index ->
            val color = if (index == current) colors.textPrimary else colors.textSecondary.copy(alpha = DOT_ALPHA)
            Box(Modifier.size(DOT_SIZE).background(color, CircleShape).testTag("home_space_dot"))
        }
    }
}

@Composable
fun EditToggle(editing: Boolean, registry: RowRegistry, actions: HomeActions, modifier: Modifier) {
    val place = modifier
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(end = BAR_PADDING, bottom = (HomeDimens.barHeight - EDIT_SIZE) / 2)
        .onGloballyPositioned { registry.block(BAR_EDIT, it.boundsInRoot()) }
    if (editing) {
        DonePill(place, actions::onEdit)
    } else {
        GlyphButton(
            Glyph.EDIT,
            stringResource(R.string.home_edit),
            LocalMushinColors.current.textPrimary,
            tag = "home_edit",
            modifier = place,
            onClick = actions::onEdit,
        )
    }
}

@Composable
private fun DonePill(modifier: Modifier, onClick: () -> Unit) {
    val colors = LocalMushinColors.current
    Box(modifier.size(DONE_WIDTH, EDIT_SIZE), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(DONE_HEIGHT)
                .clip(CircleShape)
                .background(colors.accent)
                .pressable(onClick = onClick)
                .testTag("home_edit"),
            contentAlignment = Alignment.Center,
        ) {
            SingleLineText(
                stringResource(R.string.home_edit_done),
                MushinType.style(MushinType.popupHeaderSize, colors.background, FontWeight.Medium),
            )
        }
    }
}

@Composable
private fun EditTools(registry: RowRegistry, actions: HomeActions) {
    DisposableEffect(Unit) { onDispose { registry.block(BAR_TOOLS, null) } }
    Row(
        Modifier
            .onGloballyPositioned { registry.block(BAR_TOOLS, it.boundsInRoot()) }
            .horizontalScroll(rememberScrollState())
            .testTag("home_edit_tools"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditTab(R.string.home_edit_background, "home_edit_background", actions::onEditBackground)
        EditTab(R.string.home_edit_text, "home_edit_text", actions::onEditText)
        EditTab(R.string.home_edit_theme, "home_edit_theme", actions::onEditTheme)
        EditTab(R.string.home_edit_motion, "home_edit_motion", actions::onEditMotion)
    }
}

@Composable
private fun EditTab(@StringRes text: Int, tag: String, onClick: () -> Unit) {
    Box(
        Modifier
            .height(EDIT_SIZE)
            .pressable(onClick = onClick)
            .testTag(tag)
            .padding(horizontal = TAB_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        SingleLineText(
            stringResource(text),
            MushinType.style(MushinType.popupHeaderSize, LocalMushinColors.current.textSecondary),
        )
    }
}

private const val BAR_EDIT = "bar_edit"
private const val BAR_SUGGEST = "bar_suggest"
private const val BAR_TOOLS = "bar_tools"
private const val DOT_ALPHA = 0.4f
private val DOT_SIZE = 5.dp
private val DOT_GAP = 5.dp
private val DOTS_GAP = 10.dp
private val SPACE_PADDING = 16.dp
private val BAR_PADDING = 8.dp
private val TAB_PADDING = 8.dp
private val EDIT_SIZE = 48.dp
private val DONE_WIDTH = 96.dp
private val DONE_HEIGHT = 40.dp
