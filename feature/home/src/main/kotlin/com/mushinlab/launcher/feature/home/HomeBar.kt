package com.mushinlab.launcher.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.Motion
import com.mushinlab.launcher.feature.home.gesture.RowRegistry

@Composable
internal fun HomeBar(suggestion: HomeSuggestion?, editing: Boolean, registry: RowRegistry, actions: HomeActions) {
    val colors = LocalMushinColors.current
    Row(
        Modifier.fillMaxWidth().height(HomeDimens.barHeight).padding(horizontal = BAR_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = editing,
            enter = fadeIn(tween(Motion.TRANSITION_MS)),
            exit = fadeOut(tween(Motion.TRANSITION_MS)),
        ) {
            EditTools(registry, actions)
        }
        Spacer(Modifier.weight(1f))
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
        Spacer(Modifier.width(EDIT_SIZE))
    }
}

@Composable
internal fun EditToggle(editing: Boolean, registry: RowRegistry, actions: HomeActions, modifier: Modifier) {
    val colors = LocalMushinColors.current
    val size by animateDpAsState(if (editing) DONE_SIZE else EDIT_SIZE, tween(Motion.TRANSITION_MS), label = "edit")
    val glyph by animateDpAsState(if (editing) DONE_GLYPH else EDIT_GLYPH, tween(Motion.TRANSITION_MS), label = "glyph")
    GlyphButton(
        if (editing) Glyph.CHECK else Glyph.EDIT,
        stringResource(if (editing) R.string.home_edit_done else R.string.home_edit),
        if (editing) colors.accent else colors.textSecondary.copy(alpha = BAR_ALPHA),
        tag = "home_edit",
        size = size,
        glyphSize = glyph,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(end = BAR_PADDING, bottom = (HomeDimens.barHeight - EDIT_SIZE) / 2)
            .onGloballyPositioned { registry.block(BAR_EDIT, it.boundsInRoot()) },
        onClick = actions::onEdit,
    )
}

@Composable
private fun EditTools(registry: RowRegistry, actions: HomeActions) {
    val tint = LocalMushinColors.current.textPrimary
    DisposableEffect(Unit) { onDispose { registry.block(BAR_TOOLS, null) } }
    Row(
        Modifier
            .onGloballyPositioned { registry.block(BAR_TOOLS, it.boundsInRoot()) }
            .testTag("home_edit_tools"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphButton(
            Glyph.IMAGE,
            stringResource(R.string.home_edit_background),
            tint,
            tag = "home_edit_background",
            onClick = actions::onEditBackground,
        )
        GlyphButton(
            Glyph.TEXT,
            stringResource(R.string.home_edit_text),
            tint,
            tag = "home_edit_text",
            onClick = actions::onEditText,
        )
        GlyphButton(
            Glyph.PALETTE,
            stringResource(R.string.home_edit_theme),
            tint,
            tag = "home_edit_theme",
            onClick = actions::onEditTheme,
        )
    }
}

private const val BAR_EDIT = "bar_edit"
private const val BAR_SUGGEST = "bar_suggest"
private const val BAR_TOOLS = "bar_tools"
private const val BAR_ALPHA = 0.7f
private val BAR_PADDING = 8.dp
private val EDIT_SIZE = 48.dp
private val EDIT_GLYPH = 22.dp
private val DONE_SIZE = 88.dp
private val DONE_GLYPH = 44.dp
