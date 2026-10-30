package com.mushinlab.launcher.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.ClockPosition
import com.mushinlab.launcher.core.model.HomeListOffset
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

interface HomeEditActions {
    fun onEdit()

    fun onEditClock()

    fun onEditBackground()

    fun onEditText()

    fun onEditTheme()

    fun onEditRow(row: HomeRow)

    fun onRemoveRow(row: HomeRow)
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
    editing: Boolean = false,
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
        HomeColumn(state, minute, registry, gestures, currentActions, editing, hasBackground = background != null)
        EditToggle(editing, registry, actions, Modifier.align(Alignment.BottomEnd))
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
    editing: Boolean,
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
            HomeHeader(state, minute, registry, gestures, actions, editing)
            Column(Modifier.fillMaxWidth()) {
                ListHandle(active = gestures.handle.active, visible = editing, onBounds = registry.handle::set)
                FavoritesList(
                    state = state,
                    registry = registry,
                    rowStyle = RowStyle(textStyle, rowHeight, rowColor),
                    pressedRow = { gestures.pressedRow },
                    editing = ListEditing(editing, gestures.handle.active),
                    actions = actions,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) { capacity ->
                    actions.value.onCapacityChanged(capacity)
                }
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

@Composable
private fun HomeHeader(
    state: HomeUiState,
    minute: State<Long>,
    registry: RowRegistry,
    gestures: HomeGestureHandler,
    actions: State<HomeActions>,
    editing: Boolean,
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
            editing = editing,
            pressed = gestures.pressedClock,
            onBounds = registry.clock::set,
            onTap = { actions.value.onClockTap() },
            onMeasured = gestures.clock::measured,
            onEdit = remember(registry, actions) {
                ClockEdit(onClick = { actions.value.onEditClock() }, onBounds = { registry.block(CLOCK_EDIT, it) })
            },
        )
        Spacer(Modifier.height(HomeDimens.listGap - HANDLE_HEIGHT))
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

@Immutable
private data class ListEditing(val editing: Boolean, val dragging: Boolean) {
    fun shown(index: Int): Boolean = EditList.shown(index, editing, dragging)
}

@Composable
private fun FavoritesList(
    state: HomeUiState,
    registry: RowRegistry,
    rowStyle: RowStyle,
    pressedRow: () -> Int?,
    editing: ListEditing,
    actions: State<HomeActions>,
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
                val shown = editing.shown(index)
                Reveal(shown) {
                    FavoriteRow(
                        index,
                        row,
                        RowLayout(state.iconMode, state.textAlign),
                        rowStyle,
                        registry,
                        pressedRow() == index,
                        edit = if (editing.editing && shown) {
                            RowEdit({ actions.value.onEditRow(row) }, { actions.value.onRemoveRow(row) })
                        } else {
                            null
                        },
                    )
                }
            }
        }
        val slot = EditList.addSlot(visible.size, editing.dragging)
        AnimatedVisibility(
            visible = editing.editing && state.rows.size < capacity,
            enter = fadeIn(tween(Motion.TRANSITION_MS)),
            exit = fadeOut(tween(Motion.TRANSITION_MS)),
            modifier = Modifier.offset { IntOffset(0, (rowStyle.height * slot).roundToPx()) },
        ) {
            AddRow(
                rowStyle,
                rowStyle.color(slot),
                registry,
                onAddFolder = { actions.value.onAddFolder() },
                onAddApp = { actions.value.onAddApps() },
            )
        }
    }
}

@Immutable
private class RowEdit(val onEdit: () -> Unit, val onRemove: () -> Unit)

@Composable
private fun FavoriteRow(
    index: Int,
    row: HomeRow,
    layout: RowLayout,
    rowStyle: RowStyle,
    registry: RowRegistry,
    pressed: Boolean,
    edit: RowEdit?,
) {
    val colors = LocalMushinColors.current
    val textStyle = rowStyle.text
    Row(
        Modifier
            .fillMaxWidth()
            .height(rowStyle.height)
            .background(rowStyle.color(index))
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
        if (edit != null) {
            Spacer(Modifier.width(8.dp))
            RowEditButtons(index, row.label, edit, registry)
        }
    }
}

@Composable
private fun RowEditButtons(index: Int, label: String, edit: RowEdit, registry: RowRegistry) {
    val tint = LocalMushinColors.current.textPrimary
    val id = "row_edit_$index"
    DisposableEffect(id) { onDispose { registry.block(id, null) } }
    Row(
        Modifier.onGloballyPositioned { registry.block(id, it.boundsInRoot()) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphButton(
            Glyph.EDIT,
            stringResource(R.string.home_row_edit, label),
            tint,
            tag = "row_edit_$index",
            size = EDIT_BUTTON,
            glyphSize = EDIT_GLYPH,
            onClick = edit.onEdit,
        )
        GlyphButton(
            Glyph.DELETE,
            stringResource(R.string.home_row_remove, label),
            tint,
            tag = "row_remove_$index",
            size = EDIT_BUTTON,
            glyphSize = EDIT_GLYPH,
            onClick = edit.onRemove,
        )
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
private const val CLOCK_EDIT = "clock_edit"
private const val SHADE_ZONE = 0.2f
private const val DIMMED_ALPHA = 0.5f
private val EDIT_BUTTON = 40.dp
private val EDIT_GLYPH = 20.dp
