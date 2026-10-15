package com.mushinlab.launcher.feature.home

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
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
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.SpaceType
import com.mushinlab.launcher.core.ui.SwipeMarks
import com.mushinlab.launcher.core.ui.pressable
import com.mushinlab.launcher.feature.home.gesture.GestureArbiter
import com.mushinlab.launcher.feature.home.gesture.GestureConfig
import com.mushinlab.launcher.feature.home.gesture.HomeGestureCallbacks
import com.mushinlab.launcher.feature.home.gesture.RowRegistry
import com.mushinlab.launcher.feature.home.gesture.StartZone
import com.mushinlab.launcher.feature.home.gesture.detectHomeGestures
import kotlin.math.max

interface HomeActions {
    fun onRowTap(row: HomeRow, index: Int)

    fun onLongPress(position: Offset, row: HomeRow?)

    fun onDoubleTap()

    fun onSwipe(direction: SwipeDirection, fromShadeZone: Boolean)

    fun onRequestRole()

    fun onCapacityChanged(capacity: Int)

    fun onClockTap()

    fun onClockMoved(position: ClockPosition)

    fun onEdit()

    fun onSuggestion(anchor: Offset)

    fun onListOffset(dp: Int)

    fun onDraw(points: List<Offset>, width: Float)
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
    val currentState by rememberUpdatedState(state)
    val currentActions by rememberUpdatedState(actions)
    val view = LocalView.current
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val arbiter = remember(density, viewConfiguration) {
        with(density) {
            GestureArbiter(
                GestureConfig(
                    doubleTapTimeoutMs = viewConfiguration.doubleTapTimeoutMillis,
                    doubleTapSlopPx = 100.dp.toPx(),
                    swipeDistancePx = 96.dp.toPx(),
                    flingDistancePx = 48.dp.toPx(),
                    flingVelocityPxPerSec = 1000.dp.toPx(),
                    drawMinPx = 144.dp.toPx(),
                ),
            )
        }
    }
    val zone = rememberStartZone()
    var clockDrag by remember { mutableStateOf<ClockDrag?>(null) }
    var clockShown by remember { mutableStateOf(state.clockPosition) }
    LaunchedEffect(state.clockPosition) { if (clockDrag == null) clockShown = state.clockPosition }
    val clockMetrics = remember { IntArray(2) }
    val inset = with(density) { HomeDimens.rowPadding.roundToPx() }
    val trace = remember { Trace() }
    var handleDrag by remember { mutableStateOf<Float?>(null) }
    var listOffsetPx by remember { mutableFloatStateOf(with(density) { state.homeListOffset.dp.toPx() }) }
    LaunchedEffect(state.homeListOffset, density) {
        if (handleDrag == null) listOffsetPx = with(density) { state.homeListOffset.dp.toPx() }
    }
    val maxOffsetPx = with(density) { HomeListOffset.MAX_DP.dp.toPx() }
    var homeWidth by remember { mutableFloatStateOf(0f) }
    val callbacks = remember(view) {
        object : HomeGestureCallbacks {
            override fun onRowTap(index: Int) {
                currentState.rows.getOrNull(index)?.let { currentActions.onRowTap(it, index) }
            }

            override fun onLongPress(position: Offset, rowIndex: Int?) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                currentActions.onLongPress(position, rowIndex?.let { currentState.rows.getOrNull(it) })
            }

            override fun onDoubleTap() {
                currentActions.onDoubleTap()
            }

            override fun onSwipe(direction: SwipeDirection, fromShadeZone: Boolean) {
                view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                currentActions.onSwipe(direction, fromShadeZone)
            }

            override fun onClockTap() {
                currentActions.onClockTap()
            }

            override fun onClockHold(x: Float) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                clockDrag = ClockDrag(x, x)
            }

            override fun onClockDrag(x: Float) {
                clockDrag = clockDrag?.copy(x = x)
            }

            override fun onClockRelease() {
                val drag = clockDrag ?: return
                val (area, block) = clockMetrics.let { it[0] to it[1] }
                val from = ClockSlots.left(clockShown, area, block, inset)
                val left = (from + drag.x - drag.startX).toInt().coerceIn(0, maxOf(0, area - block))
                val target = ClockSlots.nearest(left, area, block, inset)
                clockDrag = null
                if (target != clockShown) {
                    view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                    clockShown = target
                    currentActions.onClockMoved(target)
                }
            }

            override fun onHandleHold(y: Float) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                handleDrag = y
            }

            override fun onHandleDrag(y: Float) {
                val last = handleDrag ?: return
                listOffsetPx = (listOffsetPx + y - last).coerceIn(0f, maxOffsetPx)
                handleDrag = y
            }

            override fun onHandleRelease() {
                handleDrag = null
                currentActions.onListOffset(with(density) { listOffsetPx.toDp().value.toInt() })
            }

            override fun onTrace(position: Offset, timeMs: Long) {
                trace.add(position, timeMs)
            }

            override fun onDraw(points: List<Offset>) {
                currentActions.onDraw(points, homeWidth)
            }
        }
    }
    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .onSizeChanged { homeWidth = it.width.toFloat() }
            .testTag("home")
            .pointerInput(arbiter) {
                detectHomeGestures(arbiter, registry, { zone.value }, gesturesEnabled, callbacks, Motion.CLOCK_HOLD_MS)
            },
    ) {
        if (background != null) {
            Image(
                background,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().testTag("home_background"),
                contentScale = ContentScale.Crop,
            )
            Box(Modifier.fillMaxSize().background(colors.background.copy(alpha = 1f - state.backgroundOpacity)))
        }
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + HomeDimens.topGap))
            if (state.roleMissing) {
                RoleCard(registry, onClick = { currentActions.onRequestRole() })
            }
            ClockArea(
                minute = minute,
                format = state.clockFormat,
                showDate = state.showDate,
                position = clockShown,
                drag = clockDrag,
                onBounds = registry::clock,
                onTap = { currentActions.onClockTap() },
                onMeasured = { area, block ->
                    clockMetrics[0] = area
                    clockMetrics[1] = block
                },
            )
            Spacer(Modifier.height(HomeDimens.listGap - HANDLE_HEIGHT))
            Spacer(Modifier.height(with(density) { listOffsetPx.toDp() }))
            ListHandle(active = handleDrag != null, onBounds = registry::handle)
            val rowColor: (Int) -> Color = if (background == null) {
                colors::zebra
            } else {
                { index ->
                    if (index % 2 ==
                        0
                    ) {
                        Color.Transparent
                    } else {
                        colors.zebraB.copy(alpha = 1f - state.backgroundOpacity)
                    }
                }
            }
            FavoritesList(state, registry, rowColor, Modifier.weight(1f).fillMaxWidth()) { capacity ->
                currentActions.onCapacityChanged(capacity)
            }
            HomeBar(state.suggestion, registry, currentActions)
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
        TraceLayer(trace)
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
            style = SpaceType.style(SpaceType.secondarySize, colors.background),
            modifier = Modifier.padding(horizontal = HomeDimens.rowPadding),
        )
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun stringResourceRoleCard(): String = androidx.compose.ui.res.stringResource(R.string.home_make_default)

@Composable
private fun FavoritesList(
    state: HomeUiState,
    registry: RowRegistry,
    rowColor: (Int) -> Color,
    modifier: Modifier,
    onCapacity: (Int) -> Unit,
) {
    val colors = LocalMushinColors.current
    val textStyle = SpaceType.style(state.textSize.sp.sp, colors.textPrimary)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val rowHeight = remember(textStyle, density) {
        val textHeight = with(density) { measurer.measure("Ag", textStyle).size.height.toDp() }
        max(HomeDimens.rowHeight.value, (textHeight + 16.dp).value).dp
    }
    BoxWithConstraints(modifier) {
        val capacity = (maxHeight / rowHeight).toInt().coerceAtLeast(0)
        LaunchedEffect(capacity) { onCapacity(capacity) }
        val visible = state.rows.take(capacity)
        LaunchedEffect(visible.size) { registry.retainBelow(visible.size) }
        if (state.rows.isEmpty()) {
            BasicText(
                text = androidx.compose.ui.res.stringResource(R.string.home_empty_space),
                style = SpaceType.style(SpaceType.secondarySize, colors.textSecondary),
                modifier = Modifier.padding(horizontal = HomeDimens.rowPadding).testTag("empty_env"),
            )
        } else {
            Column(verticalArrangement = Arrangement.Top) {
                visible.forEachIndexed { index, row ->
                    FavoriteRow(index, row, state.iconMode, textStyle, rowHeight, registry, rowColor(index))
                }
            }
        }
    }
}

@Composable
private fun FavoriteRow(
    index: Int,
    row: HomeRow,
    iconMode: IconMode,
    textStyle: androidx.compose.ui.text.TextStyle,
    height: Dp,
    registry: RowRegistry,
    background: Color,
) {
    val colors = LocalMushinColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(background)
            .onGloballyPositioned { registry.update(index, it.boundsInRoot()) }
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
            if (row.swipes.isNotEmpty() || row.shortcut) {
                Spacer(Modifier.width(8.dp))
                SwipeMarks(
                    row.swipes,
                    textStyle.fontSize,
                    colors.textSecondary,
                    Modifier.testTag("swipe_$index"),
                    shortcut = row.shortcut,
                )
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
