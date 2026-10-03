package com.bluecubelabs.env.feature.settings

import androidx.compose.runtime.Composable

@Composable
fun SettingsHost(stack: SettingsStack, state: SettingsUiState, actions: SettingsActions) {
    val route = stack.top ?: return
    val back: () -> Unit = { stack.pop() }
    when (route) {
        SettingsRoute.Root -> RootScreen(stack::push, back)

        SettingsRoute.Environments -> EnvironmentsScreen(state, actions, stack::push, back)

        is SettingsRoute.EnvEditor -> EnvEditorScreen(route.envId, state, actions, stack::push, back)

        is SettingsRoute.PickApps -> EnvAppPicker(route.envId, state, actions, back)

        is SettingsRoute.PickGestureApp -> GestureAppPicker(route.direction, state, actions) {
            stack.pop()
            stack.pop()
        }

        SettingsRoute.Home -> HomeSettingsScreen(state, actions, back)

        SettingsRoute.Search -> SearchSettingsScreen(state, actions, back)

        SettingsRoute.Keyboard -> KeyboardSettingsScreen(state, actions, back)

        SettingsRoute.Gestures -> GesturesScreen(state, stack::push, back)

        is SettingsRoute.GestureAction -> GestureActionScreen(route.direction, state, actions, stack::push, back)

        SettingsRoute.Apps -> AppsSettingsScreen(state, actions, back)

        SettingsRoute.System -> SystemScreen(state, actions, back)
    }
}
