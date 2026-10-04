package com.bluecubelabs.env.feature.search

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
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
    val gestureTyping: Boolean,
    val gestureLetters: Boolean,
    val gestureShiftPenalty: Float,
    val gestureSuggestMargin: Float,
)

interface SearchActions : KeyboardActions {
    fun onLaunch(key: AppKey)

    fun onShowKeyboard()

    fun onToggleInput()

    fun onLongPress(key: AppKey, label: String, anchor: Offset)

    fun onToggleInEnv(key: AppKey, add: Boolean)

    fun onAppInfo(key: AppKey)

    fun onUninstall(key: AppKey)

    fun onToggleHidden(key: AppKey, hidden: Boolean)

    fun onToggleShowHidden()

    fun onDismissMenu()
}

object SearchDimens {
    val resultHeight: Dp = 56.dp
    val recentHeight: Dp = 52.dp
    val queryHeight: Dp = 64.dp
    val padding: Dp = 16.dp
    val resultIcon: Dp = 24.dp
    val recentIcon: Dp = 20.dp
    val enterInset: Dp = 6.dp
    val toggleWidth: Dp = 72.dp
    val deleteWidth: Dp = 64.dp
    val suggestionIcon: Dp = 28.dp
    val suggestionGap: Dp = 12.dp
    val eyeSize: Dp = 48.dp
    val paneGap: Dp = 8.dp
    val paneRadius: Dp = 16.dp
    val eyeGlyph: Dp = 22.dp
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
            Spacer(Modifier.height(gap))
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
                    controller.showHidden,
                    actions,
                    onLongPress,
                    Modifier.width(recentsWidth).fillMaxHeight().then(pane),
                )
            }
            Spacer(Modifier.height(gap))
            val typing = mode == SearchMode.TYPING
            Column(Modifier.fillMaxWidth().then(pane).testTag("input_pane")) {
                QueryRow(
                    controller,
                    actions,
                    enterWidth = recentsWidth.takeIf { typing },
                    inputToggle = if (!typing) {
                        null
                    } else {
                        stringResource(
                            if (chrome.gestureTyping) R.string.search_input_keys else R.string.search_input_gesture,
                        )
                    },
                    gestureDelete = typing && chrome.gestureTyping,
                )
                if (typing) {
                    if (chrome.gestureTyping) {
                        GesturePanel(
                            controller,
                            chrome.haptics,
                            chrome.gestureLetters,
                            chrome.gestureShiftPenalty,
                            chrome.gestureSuggestMargin,
                        )
                    } else {
                        Keyboard(chrome.layout, chrome.nextLayoutLabel, chrome.digitHints, chrome.haptics, actions)
                    }
                }
            }
            Spacer(Modifier.height(if (typing) KeyboardDimens.bottomLift else gap))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
        controller.menu?.let { SearchMenuPanel(it, actions) }
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
    showHidden: Boolean,
    actions: SearchActions,
    onLongPress: (AppKey, String) -> Unit,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier.testTag("recents")) {
        val fit = ((maxHeight - SearchDimens.eyeSize) / SearchDimens.recentHeight).toInt()
            .coerceIn(0, SearchDimens.MAX_RECENTS)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            HiddenToggle(showHidden, actions::onToggleShowHidden, Modifier.align(Alignment.End))
            Spacer(Modifier.weight(1f))
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
    val textStyle = remember(colors, textSize) { EnvType.style(textSize.sp, colors.textPrimary) }
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
            if (swipes.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                SwipeMarks(
                    swipes,
                    EnvType.style(EnvType.recentSize, colors.textSecondary),
                    Modifier.testTag("${tag}_swipe"),
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
private fun QueryRow(
    controller: SearchController,
    actions: SearchActions,
    enterWidth: Dp?,
    inputToggle: String?,
    gestureDelete: Boolean,
) {
    val colors = LocalEnvColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(SearchDimens.queryHeight)
            .background(Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (inputToggle != null) {
            QueryKey(
                inputToggle,
                SearchDimens.toggleWidth,
                "input_toggle",
                EnvType.secondarySize,
                actions::onToggleInput,
            )
        }
        Row(
            Modifier
                .weight(1f)
                .fillMaxHeight()
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
                    style = EnvType.style(EnvType.querySize, colors.textPrimary),
                    maxLines = 1,
                    modifier = Modifier.testTag("query"),
                )
                BasicText("▏", style = EnvType.style(EnvType.querySize, colors.accent, FontWeight.Light))
                Spacer(Modifier.weight(1f))
            }
            if (!gestureDelete) {
                BasicText(
                    text = controller.rows.size.toString(),
                    style = EnvType.style(14.sp, colors.textSecondary),
                    modifier = Modifier.testTag("result_count"),
                )
            }
        }
        if (gestureDelete) {
            QueryKey("⌫", SearchDimens.deleteWidth, "gesture_delete", EnvType.secondarySize, actions::onClear)
        }
        if (enterWidth != null) {
            QueryKey(stringResource(R.string.search_enter), enterWidth, "enter", EnvType.keySize, actions::onEnter)
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
private fun QueryKey(label: String, width: Dp, tag: String, textSize: TextUnit, onClick: () -> Unit) {
    val colors = LocalEnvColors.current
    Box(
        Modifier
            .width(width)
            .fillMaxHeight()
            .padding(SearchDimens.enterInset)
            .clip(RoundedCornerShape(KeyboardDimens.radius))
            .background(colors.keyBackground)
            .pressable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = EnvType.style(textSize, colors.textPrimary, FontWeight.Medium))
    }
}

@Composable
private fun HiddenToggle(showHidden: Boolean, onToggle: () -> Unit, modifier: Modifier) {
    val colors = LocalEnvColors.current
    val description = stringResource(R.string.search_show_hidden)
    val tint = if (showHidden) colors.accent else colors.textSecondary
    Box(
        modifier
            .size(SearchDimens.eyeSize)
            .pressable(role = Role.Switch, onClick = onToggle)
            .semantics {
                contentDescription = description
                selected = showHidden
            }
            .testTag("toggle_hidden"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(SearchDimens.eyeGlyph)) {
            val stroke = Stroke(width = size.width * EYE_STROKE, cap = StrokeCap.Round)
            val w = size.width
            val h = size.height
            val outline = Path().apply {
                moveTo(w * 0.04f, h * 0.5f)
                quadraticTo(w * 0.5f, h * 0.06f, w * 0.96f, h * 0.5f)
                quadraticTo(w * 0.5f, h * 0.94f, w * 0.04f, h * 0.5f)
                close()
            }
            drawPath(outline, tint, style = stroke)
            drawCircle(tint, radius = w * 0.14f, center = center)
            if (!showHidden) {
                drawLine(
                    tint,
                    Offset(w * 0.12f, h * 0.88f),
                    Offset(w * 0.88f, h * 0.12f),
                    stroke.width,
                    StrokeCap.Round,
                )
            }
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
        PanelItem(stringResource(R.string.menu_app_info), tag = "menu_info") { actions.onAppInfo(menu.key) }
        if (menu.canUninstall) {
            PanelItem(stringResource(R.string.menu_uninstall), tag = "menu_uninstall") { actions.onUninstall(menu.key) }
        }
    }
}

private const val HIGHLIGHT_ALPHA = 0.12f
private const val EYE_STROKE = 0.09f
private const val PANE_ZEBRA_ALPHA = 0.05f
