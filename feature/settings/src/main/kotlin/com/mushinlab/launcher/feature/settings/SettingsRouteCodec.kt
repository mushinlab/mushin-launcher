package com.mushinlab.launcher.feature.settings

import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.feature.settings.tips.Tip

object SettingsRouteCodec {
    private const val SEPARATOR = "|"

    private val objects: Map<String, SettingsRoute> = listOf(
        SettingsRoute.Root,
        SettingsRoute.Style,
        SettingsRoute.TextSheet,
        SettingsRoute.ThemeSheet,
        SettingsRoute.Advanced,
        SettingsRoute.Keyboard,
        SettingsRoute.Gestures,
        SettingsRoute.Press,
        SettingsRoute.Apps,
        SettingsRoute.System,
        SettingsRoute.Memory,
        SettingsRoute.MemoryPickApp,
        SettingsRoute.Tips,
    ).associateBy { it.toString() }

    fun encode(route: SettingsRoute): String = when (route) {
        is SettingsRoute.SpaceEditor -> join("SpaceEditor", route.spaceId)

        is SettingsRoute.PickApps -> join("PickApps", route.spaceId)

        is SettingsRoute.BackgroundEditor -> join("BackgroundEditor", route.spaceId)

        is SettingsRoute.PickGestureApp -> join("PickGestureApp", route.direction.name)

        is SettingsRoute.MemoryApp -> with(route.key) {
            join("MemoryApp", packageName, activityName, userSerial, route.sketch)
        }

        is SettingsRoute.Tip -> join("Tip", route.id)

        else -> route.toString()
    }

    fun decode(text: String): SettingsRoute? {
        val parts = text.split(SEPARATOR)
        return objects[text] ?: runCatching { decodeWithArguments(parts) }.getOrNull()
    }

    private fun decodeWithArguments(parts: List<String>): SettingsRoute? = when (parts.first()) {
        "SpaceEditor" -> SettingsRoute.SpaceEditor(parts[1].toLong())

        "PickApps" -> SettingsRoute.PickApps(parts[1].toLong())

        "BackgroundEditor" -> SettingsRoute.BackgroundEditor(parts[1].toLong())

        "PickGestureApp" -> SettingsRoute.PickGestureApp(SwipeDirection.valueOf(parts[1]))

        "MemoryApp" -> SettingsRoute.MemoryApp(
            AppKey(parts[PACKAGE_PART], parts[ACTIVITY_PART], parts[USER_PART].toLong()),
            parts[SKETCH_PART].toBooleanStrict(),
        )

        "Tip" -> Tip.byId(parts[1])?.let { SettingsRoute.Tip(it.id) }

        else -> null
    }

    private fun join(vararg parts: Any): String = parts.joinToString(SEPARATOR)

    private const val PACKAGE_PART = 1
    private const val ACTIVITY_PART = 2
    private const val USER_PART = 3
    private const val SKETCH_PART = 4
}
