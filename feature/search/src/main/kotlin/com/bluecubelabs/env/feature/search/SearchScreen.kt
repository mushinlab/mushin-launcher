package com.bluecubelabs.env.feature.search

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.IconMode
import com.bluecubelabs.env.core.model.SwipeDirection
import com.bluecubelabs.env.core.search.KeyboardLayout
import com.bluecubelabs.env.core.ui.AnchoredPanel
import com.bluecubelabs.env.core.ui.AppIcon
import com.bluecubelabs.env.core.ui.EnvType
import com.bluecubelabs.env.core.ui.Glyph
import com.bluecubelabs.env.core.ui.GlyphButton
import com.bluecubelabs.env.core.ui.HorizontalDivider
import com.bluecubelabs.env.core.ui.LocalEnvColors
import com.bluecubelabs.env.core.ui.PanelHeader
import com.bluecubelabs.env.core.ui.PanelItem
import com.bluecubelabs.env.core.ui.SingleLineText
import com.bluecubelabs.env.core.ui.SwipeMarks
import com.bluecubelabs.env.core.ui.VerticalDivider
import com.bluecubelabs.env.core.ui.pressable
import kotlin.math.abs
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class RecentRow(val key: AppKey, val label: String)

@Immutable
data class SearchChrome(
    val iconMode: IconMode,
    val layout: KeyboardLayout,
    val nextLayoutLabel: String,
    val digitHints: Boolean,
    val haptics: Boolean,
    val gestureShiftPenalty: Float,
    val gestureSuggestMargin: Float,
    val keyboardWidth: Float = 1f,
    val keyboardShift: Float = 0f,
    val fuzzy: Boolean = true,
    val layoutSwap: Boolean = true,
    val translit: Boolean = true,
    val coloredIcons: Boolean = false,
)

enum class SearchOption { FUZZY, LAYOUT_SWAP, TRANSLIT, COLORED_ICONS }

interface SearchActions : KeyboardActions {
    fun onLaunch(key: AppKey)

    fun onShowKeyboard()

    fun onRename(key: AppKey)

    fun onLongPress(key: AppKey, label: String, anchor: Offset)

    fun onToggleInEnv(key: AppKey, add: Boolean)

    fun onAppInfo(key: AppKey)

    fun onUninstall(key: AppKey)

    fun onToggleHidden(key: AppKey, hidden: Boolean)

    fun onToggleShortcut(key: AppKey, bound: Boolean)

    fun onToggleShowHidden()

    fun onSearchOption(option: SearchOption, enabled: Boolean)

    fun onClearHistory()

    fun onKeyboardFit(width: Float, shift: Float)

    fun onDismissMenu()
}

object SearchDimens {
    val resultHeight: Dp = 56.dp
    val recentHeight: Dp = 52.dp
    val queryHeight: Dp = 36.dp
    val queryTextSize: TextUnit = 18.sp
    val padding: Dp = 16.dp
    val resultIcon: Dp = 24.dp
    val recentIcon: Dp = 20.dp
    val paneTop: Dp = KeyboardDimens.verticalPadding + KeyboardDimens.gap / 2
    val anchorLength: Dp = 48.dp
    val anchorThickness: Dp = 6.dp
    val anchorInset: Dp = 10.dp
    val anchorTouch: Dp = 32.dp
    val suggestionIcon: Dp = 28.dp
    val suggestionGap: Dp = 12.dp
    val headerHeight: Dp = 48.dp
    val paneGap: Dp = 8.dp
    val paneRadius: Dp = 16.dp
    val closeSwipe: Dp = 72.dp
    const val MAX_RECENTS = 10
}

@Composable
fun SearchScreen(
    controller: SearchController,
    mode: SearchMode,
    recents: ImmutableList<RecentRow>,
    chrome: SearchChrome,
    actions: SearchActions,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEnvColors.current
    val closePx = with(LocalDensity.current) { SearchDimens.closeSwipe.toPx() }
    var lastDown by remember { mutableStateOf(Offset.Zero) }
    val onLongPress: (AppKey, String) -> Unit = { key, label -> actions.onLongPress(key, label, lastDown) }
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("search"),
    ) {
        val gap = SearchDimens.paneGap
        val recentsWidth = ((maxWidth - gap * 3).value / 3).toInt().dp
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
                .swipeDownToClose(closePx, actions::onClose),
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            SearchHeader(controller)
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                ResultsPane(
                    controller,
                    chrome.iconMode,
                    actions,
                    onLongPress,
                    Modifier.weight(1f).fillMaxHeight().then(pane),
                )
                RecentsPane(
                    recents,
                    chrome.iconMode,
                    actions,
                    onLongPress,
                    Modifier.width(recentsWidth).fillMaxHeight().then(pane),
                )
            }
            Spacer(Modifier.height(gap))
            val typing = mode == SearchMode.TYPING
            InputPane(controller, chrome, actions, typing, inputPane)
            Spacer(Modifier.height(if (typing) KeyboardDimens.bottomLift else gap))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
        controller.menu?.let { SearchMenuPanel(it, actions) }
        controller.options?.let { OptionsPanel(it, chrome, controller.showHidden, actions, controller::closeOptions) }
    }
}

@Composable
private fun SearchHeader(controller: SearchController) {
    val colors = LocalEnvColors.current
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
        ) { controller.openOptions(anchor) }
    }
}

@Composable
private fun OptionsPanel(
    anchor: Offset,
    chrome: SearchChrome,
    showHidden: Boolean,
    actions: SearchActions,
    onDismiss: () -> Unit,
) {
    var confirm by remember { mutableStateOf(false) }
    AnchoredPanel(anchor = anchor, onDismiss = onDismiss) {
        if (confirm) {
            PanelHeader(stringResource(R.string.search_clear_confirm))
            PanelItem(stringResource(R.string.search_clear), tag = "search_clear_confirm") {
                actions.onClearHistory()
                onDismiss()
            }
            PanelItem(stringResource(R.string.search_cancel), tag = "search_clear_cancel") { confirm = false }
        } else {
            PanelHeader(stringResource(R.string.search_options))
            listOf(
                Triple(SearchOption.FUZZY, R.string.search_fuzzy, chrome.fuzzy),
                Triple(SearchOption.LAYOUT_SWAP, R.string.search_layout_swap, chrome.layoutSwap),
                Triple(SearchOption.TRANSLIT, R.string.search_translit, chrome.translit),
                Triple(SearchOption.COLORED_ICONS, R.string.search_colored_icons, chrome.coloredIcons),
            ).forEach { (option, label, on) ->
                PanelItem(
                    (if (on) "✓  " else "     ") + stringResource(label),
                    tag = "search_option_$option",
                ) { actions.onSearchOption(option, !on) }
            }
            PanelItem(
                (if (showHidden) "✓  " else "     ") + stringResource(R.string.search_show_hidden),
                tag = "search_option_show_hidden",
            ) { actions.onToggleShowHidden() }
            HorizontalDivider()
            PanelItem(stringResource(R.string.search_clear_history), tag = "search_clear_history") { confirm = true }
        }
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
private fun ResultsPane(
    controller: SearchController,
    iconMode: IconMode,
    actions: SearchActions,
    onLongPress: (AppKey, String) -> Unit,
    modifier: Modifier,
) {
    val colors = LocalEnvColors.current
    val rows = controller.rows
    val listState = rememberLazyListState()
    LaunchedEffect(controller.query, controller.suggesting) { listState.scrollToItem(0) }
    if (rows.isEmpty() && controller.query.isNotEmpty()) {
        Box(modifier.padding(SearchDimens.padding).testTag("no_results"), contentAlignment = Alignment.BottomStart) {
            BasicText(
                stringResource(R.string.search_nothing_found),
                style = EnvType.style(EnvType.secondarySize, colors.textSecondary),
            )
        }
        return
    }
    val selected = controller.selected
    LazyColumn(modifier.testTag("results"), state = listState, reverseLayout = true) {
        itemsIndexed(rows, key = { _, row -> row.key.id }) { index, row ->
            AppRowItem(
                label = row.label,
                key = row.key,
                index = index,
                height = SearchDimens.resultHeight,
                textSize = EnvType.resultSize.value,
                iconMode = iconMode,
                iconSize = SearchDimens.resultIcon,
                hidden = row.hidden,
                swipes = row.swipes,
                shortcut = row.shortcut,
                highlighted = index == selected,
                tag = "result_$index",
                onLongClick = { onLongPress(row.key, row.label) },
                onClick = { actions.onLaunch(row.key) },
            )
        }
    }
}

@Composable
private fun RecentsPane(
    recents: ImmutableList<RecentRow>,
    iconMode: IconMode,
    actions: SearchActions,
    onLongPress: (AppKey, String) -> Unit,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier.testTag("recents")) {
        val fit = (maxHeight / SearchDimens.recentHeight).toInt().coerceIn(0, SearchDimens.MAX_RECENTS)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            recents.take(fit).asReversed().forEachIndexed { reversedIndex, row ->
                val index = recents.take(fit).size - 1 - reversedIndex
                AppRowItem(
                    label = row.label,
                    key = row.key,
                    index = index,
                    height = SearchDimens.recentHeight,
                    textSize = EnvType.recentSize.value,
                    iconMode = iconMode,
                    iconSize = SearchDimens.recentIcon,
                    hidden = false,
                    swipes = persistentListOf(),
                    shortcut = false,
                    highlighted = false,
                    tag = "recent_$index",
                    onLongClick = { onLongPress(row.key, row.label) },
                    onClick = { actions.onLaunch(row.key) },
                )
            }
        }
    }
}

@Composable
private fun AppRowItem(
    label: String,
    key: AppKey,
    index: Int,
    height: Dp,
    textSize: Float,
    iconMode: IconMode,
    iconSize: Dp,
    hidden: Boolean,
    swipes: ImmutableList<SwipeDirection>,
    shortcut: Boolean,
    highlighted: Boolean,
    tag: String,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
) {
    val colors = LocalEnvColors.current
    val background = when {
        highlighted -> colors.accent.copy(alpha = HIGHLIGHT_ALPHA)
        index % 2 == 1 -> colors.textPrimary.copy(alpha = PANE_ZEBRA_ALPHA)
        else -> Color.Transparent
    }
    val textStyle = EnvType.style(textSize.sp, colors.textPrimary)
    Row(
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(background)
            .pressable(onLongClick = onLongClick, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = SearchDimens.padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconMode == IconMode.LEFT) {
            AppIcon(key, label, iconSize)
            Spacer(Modifier.width(12.dp))
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            SingleLineText(label, textStyle, Modifier.weight(1f, fill = false))
            if (swipes.isNotEmpty() || shortcut) {
                Spacer(Modifier.width(8.dp))
                SwipeMarks(
                    swipes,
                    textSize.sp,
                    colors.textSecondary,
                    Modifier.testTag("${tag}_swipe"),
                    shortcut = shortcut,
                )
            }
            if (hidden) {
                Spacer(Modifier.width(6.dp))
                SingleLineText(
                    stringResource(R.string.search_hidden_suffix),
                    EnvType.style(EnvType.recentSize, colors.textSecondary),
                )
            }
        }
        if (iconMode == IconMode.RIGHT) {
            Spacer(Modifier.width(12.dp))
            AppIcon(key, label, iconSize)
        }
    }
}

@Composable
private fun InputPane(
    controller: SearchController,
    chrome: SearchChrome,
    actions: SearchActions,
    typing: Boolean,
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
            QueryRow(controller, actions)
            if (typing) {
                val glide = remember(chrome.gestureShiftPenalty, chrome.gestureSuggestMargin, controller) {
                    Glide(chrome.gestureShiftPenalty, chrome.gestureSuggestMargin, controller::applyGesture)
                }
                Keyboard(
                    chrome.layout,
                    chrome.nextLayoutLabel,
                    chrome.digitHints,
                    chrome.haptics,
                    actions,
                    glide,
                    clearTrail = controller.query.isEmpty() && !controller.suggesting,
                    drawn = controller.fromGesture,
                )
            }
        }
        if (typing && controller.resizing && total > 0f) {
            ResizeOverlay(
                frame = dragged ?: KeyboardFrame.of(width, shift, total),
                total = total,
                onDrag = { dragged = it },
                onRelease = { frame ->
                    actions.onKeyboardFit(frame.fraction(total), frame.shift(total))
                },
                onExit = controller::toggleResize,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
    LaunchedEffect(chrome.keyboardWidth, chrome.keyboardShift) { dragged = null }
}

private enum class Anchor { START, END, MOVE }

@Composable
private fun ResizeOverlay(
    frame: KeyboardFrame,
    total: Float,
    onDrag: (KeyboardFrame) -> Unit,
    onRelease: (KeyboardFrame) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier,
) {
    val colors = LocalEnvColors.current
    val current by rememberUpdatedState(frame)
    val density = LocalDensity.current
    val view = LocalView.current
    val inset = with(density) { SearchDimens.anchorInset.toPx() }
    val touch = with(density) { SearchDimens.anchorTouch.toPx() }
    var grabbed by remember { mutableStateOf<Anchor?>(null) }
    Canvas(
        modifier
            .testTag("resize_overlay")
            .pointerInput(total) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    val start = current
                    val p = down.position
                    val anchor = when {
                        abs(p.x - start.left - inset) < touch -> Anchor.START
                        abs(p.x - start.right + inset) < touch -> Anchor.END
                        abs(p.y - size.height + inset) < touch && p.x in start.left..start.right -> Anchor.MOVE
                        else -> null
                    }
                    if (anchor != null) {
                        grabbed = anchor
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    var moved = start
                    var change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    while (change != null && change.pressed) {
                        change.consume()
                        val dx = change.position.x - p.x
                        moved = when (anchor) {
                            Anchor.START -> start.dragStart(dx, total)
                            Anchor.END -> start.dragEnd(dx, total)
                            Anchor.MOVE -> start.move(dx, total)
                            null -> start
                        }
                        if (anchor != null) onDrag(moved)
                        change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    }
                    change?.consume()
                    grabbed = null
                    if (anchor == null) {
                        onExit()
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                        onRelease(moved)
                    }
                }
            },
    ) {
        val length = SearchDimens.anchorLength.toPx()
        val thickness = SearchDimens.anchorThickness.toPx()
        val radius = SearchDimens.paneRadius.toPx()
        drawRoundRect(
            colors.textSecondary,
            cornerRadius = CornerRadius(radius),
            style = Stroke(
                width = BOUNDS_STROKE.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH.toPx(), DASH.toPx())),
            ),
        )
        drawRect(colors.background.copy(alpha = RESIZE_SCRIM), Offset(frame.left, 0f), Size(frame.width, size.height))
        fun anchorColor(anchor: Anchor) = if (grabbed == anchor) colors.textPrimary else colors.accent
        fun anchorWidth(anchor: Anchor) = if (grabbed == anchor) thickness * GRABBED_SCALE else thickness
        val mid = size.height / 2
        listOf(Anchor.START to frame.left + inset, Anchor.END to frame.right - inset).forEach { (anchor, x) ->
            drawLine(
                anchorColor(anchor),
                Offset(x, mid - length / 2),
                Offset(x, mid + length / 2),
                anchorWidth(anchor),
                StrokeCap.Round,
            )
        }
        val center = frame.left + frame.width / 2
        val bottom = size.height - inset
        drawLine(
            anchorColor(Anchor.MOVE),
            Offset(center - length / 2, bottom),
            Offset(center + length / 2, bottom),
            anchorWidth(Anchor.MOVE),
            StrokeCap.Round,
        )
    }
}

@Composable
private fun QueryRow(controller: SearchController, actions: SearchActions) {
    val colors = LocalEnvColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(SearchDimens.queryHeight)
            .pressable(pressedColor = Color.Transparent, onClick = actions::onShowKeyboard)
            .testTag("query_row")
            .padding(horizontal = SearchDimens.padding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (controller.suggesting) {
            SuggestionIcons(controller.rows, actions, Modifier.weight(1f))
        } else {
            BasicText(
                text = controller.query,
                style = EnvType.style(SearchDimens.queryTextSize, colors.textPrimary),
                maxLines = 1,
                modifier = Modifier.testTag("query"),
            )
            BasicText("▏", style = EnvType.style(SearchDimens.queryTextSize, colors.accent, FontWeight.Light))
            Spacer(Modifier.weight(1f))
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

@Composable
private fun SearchMenuPanel(menu: SearchMenu, actions: SearchActions) {
    AnchoredPanel(anchor = menu.anchor, onDismiss = actions::onDismissMenu) {
        PanelHeader(menu.label)
        PanelItem(
            stringResource(if (menu.inEnv) R.string.menu_remove_from_env else R.string.menu_add_to_env),
            tag = "menu_env",
        ) { actions.onToggleInEnv(menu.key, add = !menu.inEnv) }
        PanelItem(
            stringResource(if (menu.hidden) R.string.menu_unhide else R.string.menu_hide),
            tag = "menu_hide",
        ) { actions.onToggleHidden(menu.key, !menu.hidden) }
        PanelItem(
            (if (menu.shortcut) "✓  " else "     ") + stringResource(R.string.menu_system_shortcut),
            tag = "menu_shortcut",
        ) { actions.onToggleShortcut(menu.key, !menu.shortcut) }
        PanelItem(stringResource(R.string.menu_rename), tag = "menu_rename") { actions.onRename(menu.key) }
        PanelItem(stringResource(R.string.menu_app_info), tag = "menu_info") { actions.onAppInfo(menu.key) }
        if (menu.canUninstall) {
            PanelItem(stringResource(R.string.menu_uninstall), tag = "menu_uninstall") { actions.onUninstall(menu.key) }
        }
    }
}

private const val HIGHLIGHT_ALPHA = 0.12f
private const val PANE_ZEBRA_ALPHA = 0.05f
private const val RESIZE_SCRIM = 0.55f
private const val GRABBED_SCALE = 1.6f
private val BOUNDS_STROKE = 1.5.dp
private val DASH = 6.dp
