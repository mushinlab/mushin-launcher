package com.bluecubelabs.env.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.IconMode
import com.bluecubelabs.env.core.search.KeyboardLayout
import com.bluecubelabs.env.core.ui.AnchoredPanel
import com.bluecubelabs.env.core.ui.AppIcon
import com.bluecubelabs.env.core.ui.EnvType
import com.bluecubelabs.env.core.ui.LocalEnvColors
import com.bluecubelabs.env.core.ui.PanelHeader
import com.bluecubelabs.env.core.ui.PanelItem
import com.bluecubelabs.env.core.ui.SingleLineText
import com.bluecubelabs.env.core.ui.VerticalDivider
import com.bluecubelabs.env.core.ui.pressable
import kotlin.math.abs
import kotlinx.collections.immutable.ImmutableList

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
)

interface SearchActions : KeyboardActions {
    fun onLaunch(key: AppKey)

    fun onShowKeyboard()

    fun onToggleInput()

    fun onLongPress(key: AppKey, label: String, anchor: Offset)

    fun onToggleInEnv(key: AppKey, add: Boolean)

    fun onAppInfo(key: AppKey)

    fun onUninstall(key: AppKey)

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
        val recentsWidth = (maxWidth.value / 3).toInt().dp
        Column(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        lastDown = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial).position
                    }
                }
                .swipeDownToClose(closePx, actions::onClose),
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(Modifier.weight(1f).fillMaxWidth()) {
                ResultsPane(controller, chrome.iconMode, actions, onLongPress, Modifier.weight(1f).fillMaxHeight())
                VerticalDivider(Modifier.fillMaxHeight())
                RecentsPane(
                    recents,
                    chrome.iconMode,
                    actions,
                    onLongPress,
                    Modifier.width(recentsWidth).fillMaxHeight(),
                )
            }
            val typing = mode == SearchMode.TYPING
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
                    GesturePanel(controller, chrome.haptics, chrome.gestureLetters)
                } else {
                    Keyboard(chrome.layout, chrome.nextLayoutLabel, chrome.digitHints, chrome.haptics, actions)
                }
                Spacer(Modifier.height(KeyboardDimens.bottomLift))
            }
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
    LaunchedEffect(controller.query) { listState.scrollToItem(0) }
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
        val fit = (maxHeight / SearchDimens.recentHeight).toInt().coerceAtMost(SearchDimens.MAX_RECENTS)
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
    highlighted: Boolean,
    tag: String,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
) {
    val colors = LocalEnvColors.current
    val background = if (highlighted) colors.accent.copy(alpha = HIGHLIGHT_ALPHA) else colors.zebra(index)
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
            .background(colors.background),
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
            BasicText(
                text = controller.query,
                style = EnvType.style(EnvType.querySize, colors.textPrimary),
                maxLines = 1,
                modifier = Modifier.testTag("query"),
            )
            BasicText("▏", style = EnvType.style(EnvType.querySize, colors.accent, FontWeight.Light))
            Spacer(Modifier.weight(1f))
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
private fun SearchMenuPanel(menu: SearchMenu, actions: SearchActions) {
    AnchoredPanel(anchor = menu.anchor, onDismiss = actions::onDismissMenu) {
        PanelHeader(menu.label)
        PanelItem(
            stringResource(if (menu.inEnv) R.string.menu_remove_from_env else R.string.menu_add_to_env),
            tag = "menu_env",
        ) { actions.onToggleInEnv(menu.key, add = !menu.inEnv) }
        PanelItem(stringResource(R.string.menu_app_info), tag = "menu_info") { actions.onAppInfo(menu.key) }
        if (menu.canUninstall) {
            PanelItem(stringResource(R.string.menu_uninstall), tag = "menu_uninstall") { actions.onUninstall(menu.key) }
        }
    }
}

private const val HIGHLIGHT_ALPHA = 0.12f
