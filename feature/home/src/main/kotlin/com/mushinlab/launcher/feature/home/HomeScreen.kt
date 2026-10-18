package com.mushinlab.launcher.feature.home

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
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
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.model.SwipeDirection
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

interface HomeActions : HomeGestureActions, HomeControlActions

interface HomeGestureActions {
    fun onRowTap(row: HomeRow, index: Int)

    fun onLongPress(position: Offset, row: HomeRow?)

    fun onDoubleTap()

    fun onSwipe(direction: SwipeDirection, fromShadeZone: Boolean)

    fun onDraw(points: List<Offset>, width: Float)
}

interface HomeControlActions {
    fun onRequestRole()

    fun onCapacityChanged(capacity: Int)

    fun onClockTap()

    fun onClockMoved(position: ClockPosition)

    fun onEdit()

    fun onAddApps()

    fun onSuggestion(anchor: Offset)

    fun onListOffset(dp: Int)
}

object HomeDimens {
    val rowHeight: Dp = 64.dp
    val rowPadding: Dp = 24.dp
    val iconSize: Dp = 28.dp
    val iconGap: Dp = 16.dp
    val topGap: Dp = 40.dp
    val listGap: Dp = 32.dp
    val roleCardHeight: Dp = 56.dp
    val barHeight: Dp = 56.dp
    const val BADGE_TO_ICON = 0.55f
    const val BADGE_TO_TEXT = 0.75f
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    minute: State<Long>,
    actions: HomeActions,
    gesturesEnabled: () -> Boolean,
    modifier: Modifier = Modifier,
    background: ImageBitmap? = null,
) {
    val colors = LocalMushinColors.current
    val registry = remember { RowRegistry() }
    val currentState = rememberUpdatedState(state)
    val currentActions = rememberUpdatedState(actions)
    val arbiter = rememberGestureArbiter()
    val zone = rememberStartZone()
    val trace = remember { Trace() }
    val gestures = rememberHomeGestures(currentState, currentActions, trace)
    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .onSizeChanged { gestures.width = it.width.toFloat() }
            .testTag("home")
            .pointerInput(arbiter) {
                HomeGestureDetector(arbiter, registry, { zone.value }, gesturesEnabled, Motion.CLOCK_HOLD_MS)
                    .detect(this, gestures)
            },
    ) {
        if (background != null) {
            SpaceBackdrop(
                background,
                state.backgroundBrightness,
                state.backgroundBlur,
                Modifier.fillMaxSize().testTag("home_background"),
            )
        }
        HomeColumn(state, minute, registry, gestures, currentActions, hasBackground = background != null)
        TraceLayer(trace)
    }
}

@Composable
private fun HomeColumn(
    state: HomeUiState,
    minute: State<Long>,
    registry: RowRegistry,
    gestures: HomeGestureHandler,
    actions: State<HomeActions>,
    hasBackground: Boolean,
) {
    val colors = LocalMushinColors.current
    val density = LocalDensity.current
    val textStyle = MushinType.style(state.textSize.sp.sp, colors.textPrimary)
    val rowHeight = rememberRowHeight(textStyle)
    val rowAlpha = state.backgroundRowAlpha
    val rowColor: (Int) -> Color = remember(colors, hasBackground, rowAlpha) {
        { index -> colors.row(index, overImage = hasBackground, alpha = rowAlpha) }
    }
    val placement = with(density) {
        ListPlacement(
            savedOffsetPx = state.homeListOffset?.dp?.toPx(),
            clockGapPx = HomeDimens.listGap.toPx(),
            rowHeightPx = rowHeight.toPx(),
            maxOffsetPx = HomeListOffset.MAX_DP.dp.toPx(),
            handleHeightPx = (HANDLE_HEIGHT + HANDLE_LIST_GAP).roundToPx(),
        )
    }
    Layout(
        content = {
            HomeHeader(state, minute, registry, gestures, actions)
            Column(Modifier.fillMaxWidth()) {
                ListHandle(active = gestures.handle.active, onBounds = registry.handle::set)
                FavoritesList(
                    state = state,
                    registry = registry,
                    rowStyle = RowStyle(textStyle, rowHeight, rowColor),
                    pressedRow = { gestures.pressedRow },
                    onAddApps = { actions.value.onAddApps() },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) { capacity ->
                    actions.value.onCapacityChanged(capacity)
                }
            }
            Column(Modifier.fillMaxWidth()) {
                HomeBar(state.suggestion, registry, actions.value)
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        },
        modifier = Modifier.fillMaxSize(),
        measurePolicy = remember(placement, gestures.handle) { homeColumnPolicy(placement, gestures.handle) },
    )
}

@Composable
private fun HomeHeader(
    state: HomeUiState,
    minute: State<Long>,
    registry: RowRegistry,
    gestures: HomeGestureHandler,
    actions: State<HomeActions>,
) {
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + HomeDimens.topGap))
        if (state.roleMissing) {
            RoleCard(registry, onClick = { actions.value.onRequestRole() })
        }
        ClockArea(
            minute = minute,
            format = state.clockFormat,
            showDate = state.showDate,
            position = gestures.clock.shown,
            drag = gestures.clock.drag,
            onBounds = registry.clock::set,
            onTap = { actions.value.onClockTap() },
            onMeasured = gestures.clock::measured,
        )
        Spacer(Modifier.height(HomeDimens.listGap - HANDLE_HEIGHT))
    }
}

@Composable
private fun HomeBar(suggestion: HomeSuggestion?, registry: RowRegistry, actions: HomeActions) {
    val colors = LocalMushinColors.current
    val tint = colors.textSecondary.copy(alpha = BAR_ALPHA)
    Row(
        Modifier.fillMaxWidth().height(HomeDimens.barHeight).padding(end = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (suggestion != null) {
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
        GlyphButton(
            Glyph.EDIT,
            stringResource(R.string.home_edit),
            tint,
            tag = "home_edit",
            modifier = Modifier.onGloballyPositioned { registry.block(BAR_EDIT, it.boundsInRoot()) },
            onClick = actions::onEdit,
        )
    }
}

@Composable
private fun rememberStartZone(): State<StartZone> {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val status = WindowInsets.statusBars
    val mandatory = WindowInsets.mandatorySystemGestures
    val gestures = WindowInsets.systemGestures
    val heightPx = LocalView.current.height
    val widthPx = LocalView.current.width
    return rememberUpdatedState(
        with(density) {
            StartZone(
                topLimitPx = status.getTop(this).toFloat(),
                shadeLimitPx = heightPx * SHADE_ZONE,
                bottomLimitPx = heightPx - mandatory.getBottom(this) - 16.dp.toPx(),
                leftLimitPx = gestures.getLeft(this, direction) + 8.dp.toPx(),
                rightLimitPx = widthPx - gestures.getRight(this, direction) - 8.dp.toPx(),
            )
        },
    )
}

@Composable
private fun RoleCard(registry: RowRegistry, onClick: () -> Unit) {
    val colors = LocalMushinColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(HomeDimens.roleCardHeight)
            .background(colors.accent)
            .onGloballyPositioned { registry.block(ROLE_CARD, it.boundsInRoot()) }
            .pressable(onClick = onClick)
            .testTag("role_card"),
        contentAlignment = Alignment.CenterStart,
    ) {
        SingleLineText(
            text = stringResourceRoleCard(),
            style = MushinType.style(MushinType.secondarySize, colors.background),
            modifier = Modifier.padding(horizontal = HomeDimens.rowPadding),
        )
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun stringResourceRoleCard(): String = androidx.compose.ui.res.stringResource(R.string.home_make_default)

@Immutable
internal class RowStyle(val text: TextStyle, val height: Dp, val color: (Int) -> Color)

@Composable
private fun FavoritesList(
    state: HomeUiState,
    registry: RowRegistry,
    rowStyle: RowStyle,
    pressedRow: () -> Int?,
    onAddApps: () -> Unit,
    modifier: Modifier,
    onCapacity: (Int) -> Unit,
) {
    BoxWithConstraints(modifier) {
        val capacity = (maxHeight / rowStyle.height).toInt().coerceAtLeast(0)
        LaunchedEffect(capacity) { onCapacity(capacity) }
        val visible = state.rows.take(capacity)
        LaunchedEffect(visible.size) { registry.retainBelow(visible.size) }
        Column(verticalArrangement = Arrangement.Top) {
            visible.forEachIndexed { index, row ->
                val pressed = pressedRow() == index
                FavoriteRow(
                    index,
                    row,
                    state.iconMode,
                    rowStyle.text,
                    rowStyle.height,
                    registry,
                    rowStyle.color(index),
                    pressed,
                )
            }
            if (state.rows.size < capacity) {
                AddAppsRow(state.iconMode, rowStyle, rowStyle.color(visible.size), registry, onAddApps)
            }
        }
    }
}

@Composable
private fun FavoriteRow(
    index: Int,
    row: HomeRow,
    iconMode: IconMode,
    textStyle: TextStyle,
    height: Dp,
    registry: RowRegistry,
    background: Color,
    pressed: Boolean,
) {
    val colors = LocalMushinColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(background)
            .onGloballyPositioned { registry.update(index, it.boundsInRoot()) }
            .pressFeedback(pressed)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = row.label
                onClick { false }
                onLongClick { false }
            }
            .testTag("row_$index")
            .padding(horizontal = HomeDimens.rowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val rowAlpha = if (row.dimmed) DIMMED_ALPHA else 1f
        val badgeOnIcon = iconMode != IconMode.NONE && row.notifications > 0
        if (iconMode == IconMode.LEFT) {
            BadgedIcon(row, index, badgeOnIcon, Modifier.alpha(rowAlpha))
            Spacer(Modifier.width(HomeDimens.iconGap))
        }
        Row(Modifier.weight(1f).alpha(rowAlpha), verticalAlignment = Alignment.CenterVertically) {
            SingleLineText(row.label, textStyle, Modifier.weight(1f, fill = false))
            if (row.work) {
                Spacer(Modifier.width(6.dp))
                Briefcase(colors.textSecondary)
            }
            if (row.notifications > 0 && !badgeOnIcon) {
                Spacer(Modifier.width(8.dp))
                val diameter = with(LocalDensity.current) { textStyle.fontSize.toDp() } * HomeDimens.BADGE_TO_TEXT
                CountBadge(row.notifications, diameter, Modifier.testTag("badge_$index"))
            }
            if (row.swipes.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                SwipeMarks(row.swipes, textStyle.fontSize, colors.textSecondary, Modifier.testTag("swipe_$index"))
            }
        }
        if (iconMode == IconMode.RIGHT) {
            Spacer(Modifier.width(HomeDimens.iconGap))
            BadgedIcon(row, index, badgeOnIcon, Modifier.alpha(rowAlpha))
        }
    }
}

@Composable
private fun BadgedIcon(row: HomeRow, index: Int, badge: Boolean, modifier: Modifier) {
    Box(modifier) {
        AppIcon(row.key, row.label, HomeDimens.iconSize)
        if (badge) {
            val diameter = HomeDimens.iconSize * HomeDimens.BADGE_TO_ICON
            CountBadge(
                row.notifications,
                diameter,
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = diameter / 4, y = diameter / 4)
                    .testTag("badge_$index"),
            )
        }
    }
}

private const val ROLE_CARD = "role_card"
private const val BAR_EDIT = "bar_edit"
private const val BAR_SUGGEST = "bar_suggest"
private const val BAR_ALPHA = 0.7f
private const val SHADE_ZONE = 0.2f
private const val DIMMED_ALPHA = 0.5f
