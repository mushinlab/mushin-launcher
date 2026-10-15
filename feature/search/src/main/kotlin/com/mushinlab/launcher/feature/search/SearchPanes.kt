package com.mushinlab.launcher.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.ui.AppIcon
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.SwipeMarks
import com.mushinlab.launcher.core.ui.pressable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun ResultsPane(
    controller: SearchController,
    iconMode: IconMode,
    actions: SearchActions,
    onLongPress: (AppKey, String) -> Unit,
    modifier: Modifier,
) {
    val colors = LocalMushinColors.current
    val rows = controller.rows
    val listState = rememberLazyListState()
    LaunchedEffect(controller.query, controller.suggesting) { listState.scrollToItem(0) }
    if (rows.isEmpty() && controller.query.isNotEmpty()) {
        Box(modifier.padding(SearchDimens.padding).testTag("no_results"), contentAlignment = Alignment.BottomStart) {
            BasicText(
                stringResource(R.string.search_nothing_found),
                style = MushinType.style(MushinType.secondarySize, colors.textSecondary),
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
                textSize = MushinType.resultSize.value,
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
internal fun RecentsPane(
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
                    textSize = MushinType.recentSize.value,
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
    val colors = LocalMushinColors.current
    val background = when {
        highlighted -> colors.accent.copy(alpha = HIGHLIGHT_ALPHA)
        index % 2 == 1 -> colors.textPrimary.copy(alpha = PANE_ZEBRA_ALPHA)
        else -> Color.Transparent
    }
    val textStyle = MushinType.style(textSize.sp, colors.textPrimary)
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
                    MushinType.style(MushinType.recentSize, colors.textSecondary),
                )
            }
        }
        if (iconMode == IconMode.RIGHT) {
            Spacer(Modifier.width(12.dp))
            AppIcon(key, label, iconSize)
        }
    }
}
