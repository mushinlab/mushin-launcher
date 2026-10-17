package com.mushinlab.launcher.feature.settings

import com.mushinlab.launcher.core.model.LauncherSettings

internal fun LauncherSettings.withLayout(id: String): LauncherSettings =
    if (id in keyboardLayouts) this else copy(keyboardLayouts = keyboardLayouts + id)

internal fun LauncherSettings.withoutLayout(id: String): LauncherSettings {
    val next = keyboardLayouts - id
    if (next.isEmpty()) return this
    return copy(
        keyboardLayouts = next,
        activeKeyboardLayout = activeKeyboardLayout.takeIf { it in next } ?: next.first(),
    )
}
