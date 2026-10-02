package com.bluecubelabs.env.core.model

enum class IconMode { NONE, LEFT, RIGHT }

enum class ClockFormat { SYSTEM, H24, H12 }

enum class HomeTextSize(val sp: Int) { S(22), M(26), L(30) }

data class LauncherSettings(
    val currentEnvId: Long? = null,
    val iconMode: IconMode = IconMode.NONE,
    val clockFormat: ClockFormat = ClockFormat.SYSTEM,
    val showDate: Boolean = true,
    val homeTextSize: HomeTextSize = HomeTextSize.M,
    val keyboardLayouts: List<String> = listOf("en", "ru"),
    val activeKeyboardLayout: String = "en",
    val keyboardHaptics: Boolean = true,
    val keyboardDigitHints: Boolean = true,
    val searchFuzzy: Boolean = true,
    val searchLayoutSwap: Boolean = true,
    val searchTranslit: Boolean = true,
    val onboardingDone: Boolean = false,
)
