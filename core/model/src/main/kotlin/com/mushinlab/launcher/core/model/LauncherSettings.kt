package com.mushinlab.launcher.core.model

enum class IconMode { NONE, LEFT, RIGHT }

enum class ClockFormat { SYSTEM, H24, H12 }

enum class HomeTextSize(val sp: Int) { S(sp = 22), M(sp = 26), L(sp = 30) }

enum class PressFeedback { FLASH, HOLD, ACCENT, DIM, SCALE }

object KeyboardFit {
    const val WIDTH_MIN = 0.6f
    const val WIDTH_MAX = 1f
    const val SHIFT_MIN = -1f
    const val SHIFT_MAX = 1f
    const val STEP = 0.05f
}

object BackgroundOpacity {
    const val DEFAULT = 0.5f
    const val STEP = 0.05f
}

object HomeListOffset {
    const val MAX_DP = 480
}

enum class ClockPosition { START, CENTER, END }

enum class AppFont { SYSTEM, INTER, OPEN_SANS, MONTSERRAT, CONDENSED }

enum class AppFontWeight(val value: Int) { LIGHT(value = 300), REGULAR(value = 400), MEDIUM(value = 500) }

enum class StrokeColor(val argb: Long?) {
    ACCENT(null),
    TEXT(null),
    CORAL(argb = 0xFFFF6B6B),
    ORANGE(argb = 0xFFFF9F43),
    AMBER(argb = 0xFFFFC93C),
    LIME(argb = 0xFFA3E635),
    GREEN(argb = 0xFF34D399),
    TEAL(argb = 0xFF2DD4BF),
    SKY(argb = 0xFF38BDF8),
    BLUE(argb = 0xFF6C8CFF),
    VIOLET(argb = 0xFFA78BFA),
    PINK(argb = 0xFFF472B6),
}

object StrokeWidth {
    const val MIN_DP = 2
    const val MAX_DP = 12
    const val DEFAULT_DP = 5
}

data class LauncherSettings(
    val currentSpaceId: Long? = null,
    val iconMode: IconMode = IconMode.NONE,
    val clockFormat: ClockFormat = ClockFormat.SYSTEM,
    val showDate: Boolean = true,
    val clockPosition: ClockPosition = ClockPosition.START,
    val homeListOffset: Int = 0,
    val font: AppFont = AppFont.SYSTEM,
    val fontWeight: AppFontWeight = AppFontWeight.REGULAR,
    val homeTextSize: HomeTextSize = HomeTextSize.M,
    val strokeWidth: Int = StrokeWidth.DEFAULT_DP,
    val strokeColor: StrokeColor = StrokeColor.ACCENT,
    val strokeDots: Boolean = true,
    val backgroundOpacity: Float = BackgroundOpacity.DEFAULT,
    val notificationCounts: Boolean = false,
    val pressFeedback: PressFeedback = PressFeedback.FLASH,
    val keyboardLayouts: List<String> = listOf("en", "ru"),
    val activeKeyboardLayout: String = "en",
    val keyboardHaptics: Boolean = true,
    val keyboardDigitHints: Boolean = true,
    val keyboardWidth: Float = KeyboardFit.WIDTH_MAX,
    val keyboardShift: Float = 0f,
    val gestureShiftPenalty: Float = GestureTuning.SHIFT_PENALTY,
    val gestureSuggestMargin: Float = GestureTuning.SUGGEST_MARGIN,
    val searchFuzzy: Boolean = true,
    val searchLayoutSwap: Boolean = true,
    val searchTranslit: Boolean = true,
    val searchColoredIcons: Boolean = false,
    val onboardingDone: Boolean = false,
)
