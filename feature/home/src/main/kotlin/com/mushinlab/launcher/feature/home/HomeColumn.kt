package com.mushinlab.launcher.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.ClockPosition
import com.mushinlab.launcher.core.model.HomeListOffset
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.model.WidgetPanelPosition
import com.mushinlab.launcher.core.ui.AppIcon
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.GlyphButton
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.Motion
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.SpaceBackdrop
import com.mushinlab.launcher.core.ui.SwipeMarks
import com.mushinlab.launcher.core.ui.pressFeedback
import com.mushinlab.launcher.core.ui.pressable
import com.mushinlab.launcher.feature.home.gesture.HomeGestureDetector
import com.mushinlab.launcher.feature.home.gesture.RowRegistry
import com.mushinlab.launcher.feature.home.gesture.StartZone

@Composable
internal fun HomeColumn(
    state: HomeUiState,
    minute: State<Long>,
    registry: RowRegistry,
    gestures: HomeGestureHandler,
    actions: State<HomeActions>,
    editing: Boolean,
    hasBackground: Boolean,
    widgets: WidgetContent,
) {
    val colors = LocalMushinColors.current
    val textStyle = MushinType.style(state.textSize.sp.sp, colors.textPrimary)
    val rowHeight = rememberRowHeight(textStyle)
    val rowAlpha = state.backgroundRowAlpha
    val rowColor: (Int) -> Color = remember(colors, hasBackground, rowAlpha) {
        { index -> colors.row(index, overImage = hasBackground, alpha = rowAlpha) }
    }
    val placement = rememberListPlacement(state, rowHeight)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val topPanel = state.widgetPanels.top
        val bottomPanel = state.widgetPanels.bottom
        val top = if (topPanel.hasContent(state.showClock)) topPanel.size.heightDp.toFloat() else 0f
        val bottom = if (bottomPanel.hasContent(state.showClock)) bottomPanel.size.heightDp.toFloat() else 0f
        val chrome = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + HomeDimens.topGap +
            HomeDimens.listGap + HANDLE_LIST_GAP + HomeDimens.barHeight +
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
            (if (state.roleMissing) HomeDimens.roleCardHeight + 24.dp else 0.dp) +
            (if (editing) ADD_WIDGET_HEIGHT * 2 + rowHeight else 0.dp)
        val heights = panelHeights(top, bottom, (maxHeight - chrome - rowHeight * 2).value)
        val topHeight = heights.first.dp
        val bottomHeight = heights.second.dp
        val panelWidth = maxWidth
        val listCapacity = ((maxHeight - HomeDimens.barHeight) / rowHeight).toInt().coerceAtLeast(state.rows.size)
        Layout(
            content = {
                HomeHeader(state, registry, actions) {
                    HomeWidgetPanel(
                        topPanel, WidgetPanelPosition.TOP, topHeight, panelWidth, state.showClock, editing,
                        registry, { actions.value.onAddWidget(WidgetPanelPosition.TOP) }, widgets,
                    ) {
                        HomeClock(state, minute, registry, gestures, actions, editing)
                    }
                }
                HomeFavorites(
                    state,
                    RowStyle(textStyle, rowHeight, rowColor),
                    registry,
                    gestures,
                    actions,
                    ListEditing(editing, gestures.handle.active),
                    listCapacity,
                )
                HomeWidgetPanel(
                    bottomPanel, WidgetPanelPosition.BOTTOM, bottomHeight, panelWidth, state.showClock, editing,
                    registry, { actions.value.onAddWidget(WidgetPanelPosition.BOTTOM) }, widgets,
                ) {
                    HomeClock(state, minute, registry, gestures, actions, editing)
                }
                Column(Modifier.fillMaxWidth()) {
                    HomeBar(state.suggestion, editing, registry, actions.value)
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            },
            modifier = Modifier.fillMaxSize(),
            measurePolicy = remember(placement, gestures.handle) { homeColumnPolicy(placement, gestures.handle) },
        )
    }
}

@Composable
private fun HomeFavorites(
    state: HomeUiState,
    style: RowStyle,
    registry: RowRegistry,
    gestures: HomeGestureHandler,
    actions: State<HomeActions>,
    editing: ListEditing,
    capacity: Int,
) {
    Column(Modifier.fillMaxWidth()) {
        ListHandle(active = gestures.handle.active, visible = editing.editing, onBounds = registry.handle::set)
        FavoritesList(
            state,
            registry,
            style,
            { gestures.pressedRow },
            editing,
            actions,
            Modifier.weight(1f).fillMaxWidth(),
            capacity,
        ) { actions.value.onCapacityChanged(it) }
    }
}

@Composable
private fun HomeHeader(
    state: HomeUiState,
    registry: RowRegistry,
    actions: State<HomeActions>,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + HomeDimens.topGap))
        if (state.roleMissing) {
            RoleCard(registry, onClick = { actions.value.onRequestRole() })
        }
        content()
        Spacer(Modifier.height(HomeDimens.listGap - HANDLE_HEIGHT))
    }
}

@Composable
private fun HomeClock(
    state: HomeUiState,
    minute: State<Long>,
    registry: RowRegistry,
    gestures: HomeGestureHandler,
    actions: State<HomeActions>,
    editing: Boolean,
) {
    if (state.showClock) {
        DisposableEffect(Unit) { onDispose { registry.clock.set(null) } }
        ClockArea(
            minute = minute,
            format = state.clockFormat,
            size = state.clockSize,
            showDate = state.showDate,
            position = gestures.clock.shown,
            drag = gestures.clock.drag,
            editing = editing,
            pressed = gestures.pressedClock,
            onBounds = registry.clock::set,
            onTap = { actions.value.onClockTap() },
            onMeasured = gestures.clock::measured,
            onEdit = remember(registry, actions) {
                ClockEdit(onClick = { actions.value.onEditClock() }, onBounds = { registry.block(CLOCK_EDIT, it) })
            },
        )
    }
}

@Composable
private fun rememberListPlacement(state: HomeUiState, rowHeight: Dp): ListPlacement = with(LocalDensity.current) {
    ListPlacement(
        savedOffsetPx = state.homeListOffset?.dp?.toPx(),
        clockGapPx = HomeDimens.listGap.toPx(),
        rowHeightPx = rowHeight.toPx(),
        maxOffsetPx = HomeListOffset.MAX_DP.dp.toPx(),
        handleHeightPx = (HANDLE_HEIGHT + HANDLE_LIST_GAP).roundToPx(),
    )
}
