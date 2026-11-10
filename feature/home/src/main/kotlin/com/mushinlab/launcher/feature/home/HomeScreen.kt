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
import com.mushinlab.launcher.core.model.WidgetCell
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

interface HomeActions : HomeGestureActions, HomeControlActions, HomeEditActions

interface HomeGestureActions {
    fun onRowTap(row: HomeRow, index: Int, position: Offset)

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

    fun onAddApps()

    fun onAddFolder()

    fun onOpenSettings()

    fun onSuggestion(anchor: Offset)

    fun onListOffset(dp: Int)
}

interface HomeWidgetActions {
    fun onPlaceWidget(id: Int, cell: WidgetCell) {}

    fun onRemoveWidget(id: Int) {}

    fun onAddWidget(cell: WidgetCell)
}

interface HomeEditActions : HomeWidgetActions {
    fun onEdit()

    fun onEditClock()

    fun onEditBackground()

    fun onEditText()

    fun onEditTheme()

    fun onEditWidgets()

    fun onEditTips()

    fun onEditRow(row: HomeRow)
}

object HomeDimens {
    val rowPadding: Dp = 24.dp
    val iconSize: Dp = 28.dp
    val iconGap: Dp = 16.dp
    val topGap: Dp = 40.dp
    val listGap: Dp = 32.dp
    val roleCardHeight: Dp = 56.dp
    val barHeight: Dp = 56.dp
    val barGap: Dp = 16.dp
    val minRowHeight: Dp = 48.dp
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
    editing: Boolean = false,
    background: ImageBitmap? = null,
    widgets: WidgetContent = { _, _, _ -> },
) {
    val colors = LocalMushinColors.current
    val registry = remember { RowRegistry() }
    val currentState = rememberUpdatedState(state)
    val currentActions = rememberUpdatedState(actions)
    val arbiter = rememberGestureArbiter()
    val zone = rememberStartZone()
    val trace = remember { Trace() }
    val gestures = rememberHomeGestures(currentState, currentActions, trace)
    val currentEditing = rememberUpdatedState(editing)
    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .onSizeChanged { gestures.width = it.width.toFloat() }
            .testTag("home")
            .pointerInput(arbiter) {
                HomeGestureDetector(arbiter, registry, { zone.value }, gesturesEnabled, { currentEditing.value })
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
        HomeColumn(
            state,
            minute,
            registry,
            gestures,
            currentActions,
            editing,
            hasBackground = background != null,
            widgets,
        )
        EditToggle(editing, registry, actions, Modifier.align(Alignment.BottomEnd))
        TraceLayer(trace)
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
internal fun RoleCard(registry: RowRegistry, onClick: () -> Unit) {
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
internal fun FavoritesList(
    state: HomeUiState,
    registry: RowRegistry,
    rowStyle: RowStyle,
    pressedRow: () -> Int?,
    editing: ListEditing,
    actions: State<HomeActions>,
    modifier: Modifier,
    listCapacity: Int,
    onCapacity: (Int) -> Unit,
) {
    BoxWithConstraints(modifier) {
        val capacity = listCapacity
        LaunchedEffect(capacity) { onCapacity(capacity) }
        val visible = state.rows
        val availableHeight = maxHeight
        val viewport = availableHeight - if (editing.editing) rowStyle.height else 0.dp
        val scrolls =
            rowStyle.height * visible.size > minOf(rowStyle.height * editing.rowsInView(visible.size), viewport)
        LaunchedEffect(visible.size) { registry.retainBelow(visible.size) }
        Column(verticalArrangement = Arrangement.Top) {
            ScrollingRows(
                visible.size,
                rowStyle,
                editing,
                registry,
                availableHeight - if (editing.editing) rowStyle.height else 0.dp,
            ) {
                visible.forEachIndexed { index, row ->
                    FavoriteRow(
                        index,
                        row,
                        RowLayout(state.iconMode, state.textAlign),
                        rowStyle,
                        registry,
                        pressedRow() == index,
                        onClick = if (editing.editing) {
                            { actions.value.onEditRow(row) }
                        } else {
                            { actions.value.onRowTap(row, index, registry.boundsOf(index)?.center ?: Offset.Zero) }
                        },
                        clickable = editing.editing || scrolls,
                        onHold = if (editing.editing) {
                            null
                        } else {
                            { actions.value.onLongPress(registry.boundsOf(index)?.center ?: Offset.Zero, row) }
                        },
                    )
                }
            }
            AnimatedVisibility(
                visible = editing.editing && state.rows.size < capacity,
                enter = fadeIn(tween(Motion.TRANSITION_MS)),
                exit = fadeOut(tween(Motion.TRANSITION_MS)),
            ) {
                AddRow(
                    rowStyle,
                    rowStyle.color(visible.size),
                    registry,
                    onAddFolder = { actions.value.onAddFolder() },
                    onAddApp = { actions.value.onAddApps() },
                )
            }
        }
    }
}

@Composable
private fun FavoriteRow(
    index: Int,
    row: HomeRow,
    layout: RowLayout,
    rowStyle: RowStyle,
    registry: RowRegistry,
    pressed: Boolean,
    onClick: () -> Unit,
    clickable: Boolean,
    onHold: (() -> Unit)?,
) {
    val colors = LocalMushinColors.current
    val textStyle = rowStyle.text
    Row(
        Modifier
            .fillMaxWidth()
            .height(rowStyle.height)
            .background(rowStyle.color(index))
            .onGloballyPositioned { registry.update(index, it.boundsInRoot()) }
            .then(
                if (clickable) {
                    Modifier.pressable(
                        onLongClick = onHold,
                        onClick = onClick,
                    )
                } else {
                    Modifier.pressFeedback(pressed)
                },
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = row.label
                onClick {
                    onClick()
                    true
                }
                onLongClick {
                    onHold?.invoke()
                    onHold != null
                }
            }
            .testTag("row_$index")
            .padding(horizontal = HomeDimens.rowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val badgeOnIcon = layout.icons && row.notifications > 0
        val icon: (@Composable () -> Unit)? = if (layout.icons) {
            { BadgedIcon(row, index, badgeOnIcon, Modifier.testTag("icon_$index")) }
        } else {
            null
        }
        LabelWithIcon(layout.align, Modifier.weight(1f).alpha(if (row.dimmed) DIMMED_ALPHA else 1f), icon) {
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
    }
}

@Composable
private fun BadgedIcon(row: HomeRow, index: Int, badge: Boolean, modifier: Modifier) {
    Box(modifier) {
        if (row.folderId != null) {
            GlyphBadge(Glyph.FOLDER, HomeDimens.iconSize, Modifier.testTag("folder_icon_$index"))
        } else {
            AppIcon(row.key, row.label, HomeDimens.iconSize)
        }
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
private const val SHADE_ZONE = 0.2f
private const val DIMMED_ALPHA = 0.5f
