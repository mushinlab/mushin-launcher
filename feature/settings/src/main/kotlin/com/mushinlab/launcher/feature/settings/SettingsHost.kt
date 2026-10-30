package com.mushinlab.launcher.feature.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import com.mushinlab.launcher.core.ui.Motion
import com.mushinlab.launcher.feature.settings.tips.Tip
import com.mushinlab.launcher.feature.settings.tips.TipScreen
import com.mushinlab.launcher.feature.settings.tips.TipsScreen

@Immutable
private data class SettingsEntry(val route: SettingsRoute, val depth: Int)

@Composable
fun SettingsHost(stack: SettingsStack, state: SettingsUiState, actions: SettingsActions) {
    AnimatedContent(
        targetState = stack.top?.let { SettingsEntry(it, stack.size()) },
        transitionSpec = { settingsTransition() },
        label = "settings",
    ) { entry ->
        if (entry != null) {
            CompositionLocalProvider(
                LocalSpaceName provides state.spaces.firstOrNull { it.current }?.name,
                LocalSettingsTransition provides this,
            ) {
                SettingsRouteScreen(entry.route, stack, state, actions)
            }
        }
    }
}

private fun AnimatedContentTransitionScope<SettingsEntry?>.settingsTransition(): ContentTransform {
    val from = initialState
    val to = targetState
    return when {
        from == null -> fadeIn(tween(Motion.TRANSITION_MS)) togetherWith ExitTransition.None

        to == null -> EnterTransition.None togetherWith fadeOut(tween(Motion.TRANSITION_MS))

        to.depth > from.depth ->
            slideInHorizontally(tween(Motion.TRANSITION_MS)) { it } togetherWith
                ExitTransition.KeepUntilTransitionsFinished

        else -> (
            EnterTransition.None togetherWith
                slideOutHorizontally(tween(Motion.TRANSITION_MS)) { it }
            ).apply { targetContentZIndex = -1f }
    }.using(null)
}

@Composable
private fun SettingsRouteScreen(
    route: SettingsRoute,
    stack: SettingsStack,
    state: SettingsUiState,
    actions: SettingsActions,
) {
    val back: () -> Unit = { if (stack.top == route) stack.pop() }
    val push: (SettingsRoute) -> Unit = { if (stack.top == route) stack.push(it) }
    when (route) {
        SettingsRoute.Root -> RootScreen(state, actions, push, back)

        is SettingsRoute.SpaceEditor,
        is SettingsRoute.PickApps,
        is SettingsRoute.BackgroundEditor,
        is SettingsRoute.BackgroundPicker,
        is SettingsRoute.BackgroundFolder,
        ->
            SpaceRouteScreen(route, state, actions, push, back)

        SettingsRoute.ClockSheet -> ClockSheet(state, actions, back)

        SettingsRoute.TextSheet -> TextSheet(state, actions, back)

        SettingsRoute.ThemeSheet -> ThemeSheet(state, actions, back)

        SettingsRoute.Keyboard, SettingsRoute.Gestures, is SettingsRoute.PickGestureApp ->
            InputRouteScreen(route, state, actions, push, back)

        SettingsRoute.Apps, SettingsRoute.System, SettingsRoute.Privacy ->
            SystemRouteScreen(route, state, actions, push, back)

        is SettingsRoute.MemoryApp, SettingsRoute.MemoryPickApp ->
            MemoryRouteScreen(route, stack, state, actions, push, back)

        SettingsRoute.Tips, is SettingsRoute.Tip -> TipsRouteScreen(route, push, back)

        is SettingsRoute.HomeFolder, is SettingsRoute.PickFolderApps, is SettingsRoute.ReplaceApp ->
            HomeEditRouteScreen(route, state, actions, push, back)
    }
}

@Composable
private fun HomeEditRouteScreen(
    route: SettingsRoute,
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    when (route) {
        is SettingsRoute.HomeFolder -> FolderSheet(route.folderId, state, actions, push, back)
        is SettingsRoute.PickFolderApps -> FolderAppPicker(route.folderId, state, actions, back)
        is SettingsRoute.ReplaceApp -> ReplaceAppPicker(route, state, actions, back)
        else -> Unit
    }
}

@Composable
private fun SpaceRouteScreen(
    route: SettingsRoute,
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    when (route) {
        is SettingsRoute.PickApps -> SpaceAppPicker(route.spaceId, state, actions, back)
        is SettingsRoute.BackgroundEditor -> BackgroundEditorScreen(route.spaceId, state, actions, push, back)
        is SettingsRoute.BackgroundPicker -> BackgroundPickerScreen(route.spaceId, actions, push, back)
        is SettingsRoute.BackgroundFolder -> BackgroundFolderScreen(route.spaceId, route.folder, actions, back)
        is SettingsRoute.SpaceEditor -> SpaceEditorScreen(route.spaceId, state, actions, push, back)
        else -> Unit
    }
}

@Composable
private fun InputRouteScreen(
    route: SettingsRoute,
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    when (route) {
        is SettingsRoute.PickGestureApp -> GestureAppPicker(route.direction, state, actions, back)
        SettingsRoute.Gestures -> GesturesScreen(state, actions, push, back)
        else -> KeyboardSettingsScreen(state, actions, back)
    }
}

@Composable
private fun SystemRouteScreen(
    route: SettingsRoute,
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    when (route) {
        SettingsRoute.Apps -> AppsSettingsScreen(state, actions, back)
        SettingsRoute.Privacy -> PrivacyScreen(state, actions, back)
        else -> SystemScreen(state, actions, push, back)
    }
}

@Composable
private fun MemoryRouteScreen(
    route: SettingsRoute,
    stack: SettingsStack,
    state: SettingsUiState,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    when (route) {
        is SettingsRoute.MemoryApp -> MemoryAppScreen(route, state, actions, stack, back)
        else -> MemoryAppPicker(state, stack, push, back)
    }
}

@Composable
private fun TipsRouteScreen(route: SettingsRoute, push: (SettingsRoute) -> Unit, back: () -> Unit) {
    val tip = (route as? SettingsRoute.Tip)?.let { Tip.byId(it.id) }
    if (tip != null) TipScreen(tip, back) else TipsScreen(push, back)
}
