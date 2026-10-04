package com.bluecubelabs.env.core.model

enum class IconMode { NONE, LEFT, RIGHT }

enum class ClockFormat { SYSTEM, H24, H12 }

enum class HomeTextSize(val sp: Int) { S(22), M(26), L(30) }

enum class PressFeedback { FLASH, HOLD, ACCENT, DIM, SCALE }

enum class ClockPosition { START, CENTER, END }

enum class AppFont { SYSTEM, INTER, OPEN_SANS, MONTSERRAT, CONDENSED }

enum class AppFontWeight(val value: Int) { LIGHT(300), REGULAR(400), MEDIUM(500) }

data class LauncherSettings(
    val currentEnvId: Long? = null,
    val iconMode: IconMode = IconMode.NONE,
    val clockFormat: ClockFormat = ClockFormat.SYSTEM,
    val showDate: Boolean = true,
    val clockPosition: ClockPosition = ClockPosition.START,
    val font: AppFont = AppFont.SYSTEM,
    val fontWeight: AppFontWeight = AppFontWeight.REGULAR,
    val homeTextSize: HomeTextSize = HomeTextSize.M,
    val notificationCounts: Boolean = false,
    val pressFeedback: PressFeedback = PressFeedback.FLASH,
    val keyboardLayouts: List<String> = listOf("en", "ru"),
    val activeKeyboardLayout: String = "en",
    val keyboardHaptics: Boolean = true,
    val keyboardDigitHints: Boolean = true,
    val keyboardGestureTyping: Boolean = false,
    val keyboardGestureLetters: Boolean = false,
    val gestureShiftPenalty: Float = GestureTuning.SHIFT_PENALTY,
    val gestureSuggestMargin: Float = GestureTuning.SUGGEST_MARGIN,
    val searchFuzzy: Boolean = true,
    val searchLayoutSwap: Boolean = true,
    val searchTranslit: Boolean = true,
    val onboardingDone: Boolean = false,
)
