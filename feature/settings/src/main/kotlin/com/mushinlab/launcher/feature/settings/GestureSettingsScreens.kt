package com.mushinlab.launcher.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.SwipeDirection

@Composable
internal fun directionLabel(direction: SwipeDirection): String = stringResource(
    when (direction) {
        SwipeDirection.UP -> R.string.gesture_up
        SwipeDirection.DOWN -> R.string.gesture_down
        SwipeDirection.LEFT -> R.string.gesture_left
        SwipeDirection.RIGHT -> R.string.gesture_right
    },
)

@Composable
internal fun actionLabel(summary: GestureSummary?): String {
    val action = summary?.action ?: GestureAction.None
    return when (action) {
        GestureAction.None -> stringResource(R.string.action_none)

        GestureAction.Notifications -> stringResource(R.string.action_notifications)

        GestureAction.Search -> stringResource(R.string.action_search)

        GestureAction.AllApps -> stringResource(R.string.action_all_apps)

        is GestureAction.LaunchApp -> if (summary?.appMissing != false) {
            stringResource(R.string.action_app_missing)
        } else {
            summary.appLabel.orEmpty()
        }
    }
}

@Composable
internal fun GesturesScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    SettingsPage(stringResource(R.string.settings_gestures), back) {
        SectionHeader(stringResource(R.string.gestures_swipes))
        SwipeDirection.entries.forEach { direction ->
            SwipeRow(direction, state.gestures[direction], actions.preferences, push)
        }
        NoteText(stringResource(R.string.gesture_shade_zone))
        SectionHeader(stringResource(R.string.style_stroke))
        StrokeRows(state.settings, actions.preferences)
    }
}

@Composable
private fun SwipeRow(
    direction: SwipeDirection,
    summary: GestureSummary?,
    preferences: PreferenceActions,
    push: (SettingsRoute) -> Unit,
) {
    val current = summary?.action ?: GestureAction.None
    DropdownRow(
        directionLabel(direction),
        listOf(
            GestureChoice.NONE to stringResource(R.string.action_none),
            GestureChoice.NOTIFICATIONS to stringResource(R.string.action_notifications),
            GestureChoice.SEARCH to stringResource(R.string.action_search),
            GestureChoice.ALL_APPS to stringResource(R.string.action_all_apps),
            GestureChoice.APP to if (current is GestureAction.LaunchApp) {
                actionLabel(summary)
            } else {
                stringResource(R.string.action_launch_app)
            },
        ),
        GestureChoice.of(current),
        "gesture_$direction",
    ) { choice ->
        when (choice) {
            GestureChoice.APP -> push(SettingsRoute.PickGestureApp(direction))
            else -> preferences.setGesture(direction, choice.action)
        }
    }
}

internal enum class GestureChoice(val action: GestureAction) {
    NONE(GestureAction.None),
    NOTIFICATIONS(GestureAction.Notifications),
    SEARCH(GestureAction.Search),
    ALL_APPS(GestureAction.AllApps),
    APP(GestureAction.None),
    ;

    companion object {
        fun of(action: GestureAction): GestureChoice = when (action) {
            GestureAction.None -> NONE
            GestureAction.Notifications -> NOTIFICATIONS
            GestureAction.Search -> SEARCH
            GestureAction.AllApps -> ALL_APPS
            is GestureAction.LaunchApp -> APP
        }
    }
}
