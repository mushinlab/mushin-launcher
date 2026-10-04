package com.bluecubelabs.env.feature.home

import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
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
import com.bluecubelabs.env.core.model.IconMode
import com.bluecubelabs.env.core.model.SwipeDirection
import com.bluecubelabs.env.core.ui.AppIcon
import com.bluecubelabs.env.core.ui.EnvType
import com.bluecubelabs.env.core.ui.LocalEnvColors
import com.bluecubelabs.env.core.ui.SingleLineText
import com.bluecubelabs.env.core.ui.SwipeMarks
import com.bluecubelabs.env.core.ui.pressable
import com.bluecubelabs.env.feature.home.gesture.GestureArbiter
import com.bluecubelabs.env.feature.home.gesture.GestureConfig
import com.bluecubelabs.env.feature.home.gesture.HomeGestureCallbacks
import com.bluecubelabs.env.feature.home.gesture.RowRegistry
import com.bluecubelabs.env.feature.home.gesture.StartZone
import com.bluecubelabs.env.feature.home.gesture.detectHomeGestures
import kotlin.math.max

interface HomeActions {
    fun onRowTap(row: HomeRow, index: Int)

    fun onLongPress(position: Offset, row: HomeRow?)

    fun onDoubleTap()

    fun onSwipe(direction: SwipeDirection, fromShadeZone: Boolean)

    fun onRequestRole()

    fun onCapacityChanged(capacity: Int)
}

object HomeDimens {
    val rowHeight: Dp = 64.dp
    val rowPadding: Dp = 24.dp
    val iconSize: Dp = 28.dp
    val iconGap: Dp = 16.dp
    val topGap: Dp = 40.dp
    val listGap: Dp = 32.dp
    val roleCardHeight: Dp = 56.dp
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    minute: State<Long>,
    actions: HomeActions,
    gesturesEnabled: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEnvColors.current
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
                ),
            )
        }
    }
    val zone = rememberStartZone()
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
        }
    }
    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("home")
            .pointerInput(arbiter) {
                detectHomeGestures(arbiter, registry, { zone.value }, gesturesEnabled, callbacks)
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + HomeDimens.topGap))
            if (state.roleMissing) {
                RoleCard(registry, onClick = { currentActions.onRequestRole() })
            }
            ClockBlock(minute, state.clockFormat, state.showDate)
            Spacer(Modifier.height(HomeDimens.listGap))
            FavoritesList(state, registry, Modifier.weight(1f).fillMaxWidth()) { capacity ->
                currentActions.onCapacityChanged(capacity)
            }
        }
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
    val colors = LocalEnvColors.current
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
            style = EnvType.style(EnvType.secondarySize, colors.background),
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
    modifier: Modifier,
    onCapacity: (Int) -> Unit,
) {
    val colors = LocalEnvColors.current
    val textStyle = EnvType.style(state.textSize.sp.sp, colors.textPrimary)
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
                text = androidx.compose.ui.res.stringResource(R.string.home_empty_env),
                style = EnvType.style(EnvType.secondarySize, colors.textSecondary),
                modifier = Modifier.padding(horizontal = HomeDimens.rowPadding).testTag("empty_env"),
            )
        } else {
            Column(verticalArrangement = Arrangement.Top) {
                visible.forEachIndexed { index, row ->
                    FavoriteRow(index, row, state.iconMode, textStyle, rowHeight, registry)
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
) {
    val colors = LocalEnvColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(colors.zebra(index))
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
        if (iconMode == IconMode.LEFT) {
            AppIcon(row.key, row.label, HomeDimens.iconSize, Modifier.alpha(rowAlpha))
            Spacer(Modifier.width(HomeDimens.iconGap))
        }
        Row(Modifier.weight(1f).alpha(rowAlpha), verticalAlignment = Alignment.CenterVertically) {
            SingleLineText(row.label, textStyle, Modifier.weight(1f, fill = false))
            if (row.work) {
                Spacer(Modifier.width(6.dp))
                Briefcase(colors.textSecondary)
            }
            if (row.swipes.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                SwipeMarks(
                    row.swipes,
                    EnvType.style(EnvType.recentSize, colors.textSecondary),
                    Modifier.testTag("swipe_$index"),
                )
            }
            if (row.notifications > 0) {
                Spacer(Modifier.width(4.dp))
                BasicText(
                    if (row.notifications > MAX_BADGE) "$MAX_BADGE+" else row.notifications.toString(),
                    style = EnvType.style(EnvType.badgeSize, colors.accent),
                    modifier = Modifier.align(Alignment.Top).testTag("badge_$index"),
                )
            }
        }
        if (iconMode == IconMode.RIGHT) {
            Spacer(Modifier.width(HomeDimens.iconGap))
            AppIcon(row.key, row.label, HomeDimens.iconSize, Modifier.alpha(rowAlpha))
        }
    }
}

private const val ROLE_CARD = "role_card"
private const val SHADE_ZONE = 0.2f
private const val DIMMED_ALPHA = 0.5f
private const val MAX_BADGE = 99
