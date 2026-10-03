package com.bluecubelabs.env.feature.search

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.IconMode
import com.bluecubelabs.env.core.search.KeyboardLayout
import com.bluecubelabs.env.core.ui.AppIcon
import com.bluecubelabs.env.core.ui.EnvType
import com.bluecubelabs.env.core.ui.LocalEnvColors
import com.bluecubelabs.env.core.ui.SingleLineText
import com.bluecubelabs.env.core.ui.VerticalDivider
import com.bluecubelabs.env.core.ui.pressable
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
)

interface SearchActions : KeyboardActions {
    fun onLaunch(key: AppKey)

    fun onShowKeyboard()
}

object SearchDimens {
    val resultHeight: Dp = 56.dp
    val recentHeight: Dp = 52.dp
    val queryHeight: Dp = 56.dp
    val padding: Dp = 16.dp
    val resultIcon: Dp = 24.dp
    val recentIcon: Dp = 20.dp
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
    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("search"),
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val recentsWidth = (maxWidth.value / 3).toInt().dp
            Row(Modifier.fillMaxSize()) {
                ResultsPane(controller, chrome.iconMode, actions, Modifier.weight(1f).fillMaxHeight())
                VerticalDivider(Modifier.fillMaxHeight())
                RecentsPane(recents, chrome.iconMode, actions, Modifier.width(recentsWidth).fillMaxHeight())
            }
        }
        QueryRow(controller, actions)
        if (mode == SearchMode.TYPING) {
            Keyboard(chrome.layout, chrome.nextLayoutLabel, chrome.digitHints, chrome.haptics, actions)
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun ResultsPane(controller: SearchController, iconMode: IconMode, actions: SearchActions, modifier: Modifier) {
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
    modifier: Modifier,
) {
    BoxWithConstraints(modifier.testTag("recents")) {
        val fit = (maxHeight / SearchDimens.recentHeight).toInt().coerceAtMost(SearchDimens.MAX_RECENTS)
        Column(Modifier.fillMaxSize(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom) {
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
            .pressable(onClick = onClick)
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
private fun QueryRow(controller: SearchController, actions: SearchActions) {
    val colors = LocalEnvColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(SearchDimens.queryHeight)
            .background(colors.background)
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
        BasicText(
            text = controller.rows.size.toString(),
            style = EnvType.style(14.sp, colors.textSecondary),
            modifier = Modifier.testTag("result_count"),
        )
    }
}

private const val HIGHLIGHT_ALPHA = 0.12f
