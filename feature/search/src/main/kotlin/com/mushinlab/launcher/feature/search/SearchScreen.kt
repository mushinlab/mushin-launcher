package com.mushinlab.launcher.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.ui.AppIcon
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.popupBackdrop
import com.mushinlab.launcher.core.ui.pressable
import kotlin.math.abs
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SearchScreen(
    controller: SearchController,
    mode: SearchMode,
    chrome: SearchChrome,
    actions: SearchActions,
    modifier: Modifier = Modifier,
    active: Boolean = true,
) {
    val colors = LocalMushinColors.current
    val closePx = with(LocalDensity.current) { SearchDimens.closeSwipe.toPx() }
    var lastDown by remember { mutableStateOf(Offset.Zero) }
    val onLongPress: (AppKey, String) -> Unit = { key, label -> actions.onLongPress(key, label, lastDown) }
    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("search"),
    ) {
        val gap = SearchDimens.paneGap
        val pane = Modifier.clip(RoundedCornerShape(SearchDimens.paneRadius)).background(colors.surface)
        val inputPane = Modifier.background(colors.surface, RoundedCornerShape(SearchDimens.paneRadius))
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = gap)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        lastDown = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial).position
                    }
                }
                .swipeDownToClose(closePx, actions::onClose)
                .popupBackdrop(controller.panels.open),
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            SearchHeader(controller)
            Spacer(Modifier.height(SearchDimens.headerGap))
            ResultsPane(
                controller,
                chrome.iconMode,
                actions,
                onLongPress,
                Modifier.weight(1f).fillMaxWidth().then(pane),
            )
            Spacer(Modifier.height(gap))
            val typing = mode == SearchMode.TYPING
            InputPane(controller, chrome, actions, typing, active, inputPane)
            Spacer(Modifier.height(if (typing) KeyboardDimens.bottomLift else gap))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
        val panels = controller.panels
        panels.menu?.let { SearchMenuPanel(it, actions) }
        panels.options?.let { OptionsPanel(it, chrome, controller.showHidden, actions, panels::closeOptions) }
    }
}

@Composable
private fun SearchHeader(controller: SearchController) {
    val colors = LocalMushinColors.current
    var anchor by remember { mutableStateOf(Offset.Zero) }
    Row(
        Modifier.fillMaxWidth().height(SearchDimens.headerHeight).testTag("search_header"),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphButton(
            Glyph.SETTINGS,
            stringResource(R.string.search_options),
            colors.textSecondary,
            tag = "search_options",
            modifier = Modifier.onGloballyPositioned { anchor = it.boundsInRoot().bottomCenter },
        ) { controller.panels.openOptions(anchor) }
    }
}

@Composable
private fun Modifier.swipeDownToClose(thresholdPx: Float, onClose: () -> Unit): Modifier {
    val currentOnClose by rememberUpdatedState(onClose)
    val connection = remember(thresholdPx) {
        object : NestedScrollConnection {
            var pull = 0f

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    pull += available.y
                    if (pull > thresholdPx) {
                        pull = Float.NEGATIVE_INFINITY
                        currentOnClose()
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pull = 0f
                return Velocity.Zero
            }
        }
    }
    return this
        .nestedScroll(connection)
        .pointerInput(thresholdPx) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                while (change != null && change.pressed && !change.isConsumed) {
                    val delta = change.position - down.position
                    change = if (delta.y > thresholdPx && abs(delta.x) < delta.y) {
                        currentOnClose()
                        null
                    } else {
                        awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    }
                }
            }
        }
}

@Composable
private fun InputPane(
    controller: SearchController,
    chrome: SearchChrome,
    actions: SearchActions,
    typing: Boolean,
    active: Boolean,
    pane: Modifier,
) {
    var total by remember { mutableFloatStateOf(0f) }
    var dragged by remember { mutableStateOf<KeyboardFrame?>(null) }
    val width = dragged?.fraction(total) ?: chrome.keyboardWidth
    val shift = dragged?.shift(total) ?: chrome.keyboardShift
    Box(Modifier.fillMaxWidth().onSizeChanged { total = it.width.toFloat() }) {
        Column(
            Modifier
                .fillMaxWidth(width)
                .align(BiasAlignment(shift, 0f))
                .then(pane)
                .testTag("input_pane")
                .padding(top = SearchDimens.paneTop),
        ) {
            QueryRow(controller, chrome.haptics, actions)
            if (typing) {
                val glide = remember(chrome.gestureShiftPenalty, chrome.gestureSuggestMargin, controller) {
                    Glide(chrome.gestureShiftPenalty, chrome.gestureSuggestMargin, controller.gestures::decode)
                }
                Keyboard(
                    chrome.layout,
                    chrome.nextLayoutLabel,
                    chrome.digitHints,
                    chrome.haptics,
                    actions,
                    glide,
                    clearTrail = controller.query.isEmpty() && !controller.suggesting,
                    excludeSystemGestures = active,
                )
            }
        }
        if (typing && controller.panels.resizing && total > 0f) {
            ResizeOverlay(
                frame = dragged ?: KeyboardFrame.of(width, shift, total),
                total = total,
                onDrag = { dragged = it },
                onRelease = { frame ->
                    actions.onKeyboardFit(frame.fraction(total), frame.shift(total))
                },
                onExit = controller.panels::toggleResize,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
    LaunchedEffect(chrome.keyboardWidth, chrome.keyboardShift) { dragged = null }
}

@Composable
private fun QueryRow(controller: SearchController, haptics: Boolean, actions: SearchActions) {
    val colors = LocalMushinColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(SearchDimens.queryHeight)
            .pressable(pressedColor = Color.Transparent, onClick = actions::onShowKeyboard)
            .testTag("query_row")
            .padding(start = SearchDimens.padding, end = KeyboardDimens.gap / 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (controller.suggesting) {
            SuggestionIcons(controller.rows, actions, Modifier.weight(1f))
        } else {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = controller.query,
                    style = MushinType.style(SearchDimens.queryTextSize, colors.textPrimary),
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false).testTag("query"),
                )
                BasicText("▏", style = MushinType.style(SearchDimens.queryTextSize, colors.accent, FontWeight.Light))
            }
        }
        if (controller.query.isNotEmpty() || controller.suggesting) {
            DeleteButton(controller.fromGesture, haptics, actions)
        }
    }
}

@Composable
private fun SuggestionIcons(rows: ImmutableList<SearchRow>, actions: SearchActions, modifier: Modifier) {
    val description = stringResource(R.string.search_suggestions)
    Row(
        modifier
            .horizontalScroll(rememberScrollState())
            .semantics { contentDescription = description }
            .testTag("suggestions"),
        horizontalArrangement = Arrangement.spacedBy(SearchDimens.suggestionGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        rows.forEachIndexed { index, row ->
            AppIcon(
                row.key,
                row.label,
                SearchDimens.suggestionIcon,
                Modifier
                    .pressable { actions.onLaunch(row.key) }
                    .semantics { contentDescription = row.label }
                    .testTag("suggestion_$index"),
            )
        }
    }
}

internal const val HIGHLIGHT_ALPHA = 0.12f
internal const val PANE_ZEBRA_ALPHA = 0.05f
