package com.bluecubelabs.env.feature.settings

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
import com.bluecubelabs.env.core.ui.Motion

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
            CompositionLocalProvider(LocalSpaceName provides state.envs.firstOrNull { it.current }?.name) {
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
        SettingsRoute.Environments -> EnvironmentsScreen(state, actions, back)
        is SettingsRoute.EnvEditor -> EnvEditorScreen(route.envId, state, actions, push, back)
        is SettingsRoute.PickApps -> EnvAppPicker(route.envId, state, actions, back)
        is SettingsRoute.PickGestureApp -> GestureAppPicker(route.direction, state, actions, back)
        SettingsRoute.Home -> HomeSettingsScreen(state, actions, back)
        SettingsRoute.Style -> StyleScreen(state, actions, back)
        SettingsRoute.Advanced -> AdvancedScreen(state, push, back)
        SettingsRoute.Keyboard -> KeyboardSettingsScreen(state, actions, back)
        SettingsRoute.Gestures -> GesturesScreen(state, actions, push, back)
        SettingsRoute.Press -> PressSettingsScreen(state, actions, back)
        SettingsRoute.Apps -> AppsSettingsScreen(state, actions, back)
        SettingsRoute.System -> SystemScreen(state, actions, push, back)
        SettingsRoute.Memory -> MemoryScreen(state, actions, push, back)
        is SettingsRoute.MemoryApp -> MemoryAppScreen(route, state, actions, stack, back)
        SettingsRoute.MemoryPickApp -> MemoryAppPicker(state, stack, push, back)
    }
}
