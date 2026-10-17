package com.mushinlab.launcher.core.model

/** Whether app icons are shown next to labels, and on which side. */
enum class IconMode { NONE, LEFT, RIGHT }

/** Clock format; [SYSTEM] follows the device 12/24-hour setting. */
enum class ClockFormat { SYSTEM, H24, H12 }

/** Size of home-screen app labels; [sp] is the font size in sp. */
enum class HomeTextSize(val sp: Int) { S(sp = 22), M(sp = 26), L(sp = 30) }

/**
 * The five allowed press styles (ADR-0004): fading highlight, briefly held highlight, accent tint, dimming
 * and scale-down. Ripple is never used.
 */
enum class PressFeedback { FLASH, HOLD, ACCENT, DIM, SCALE }

/**
 * Allowed on-screen keyboard width (fraction of the screen width) and horizontal position
 * (-1 at the start edge, 1 at the end edge).
 */
object KeyboardFit {
    const val WIDTH_MIN = 0.6f
    const val WIDTH_MAX = 1f
    const val SHIFT_MIN = -1f
    const val SHIFT_MAX = 1f
    const val STEP = 0.05f
}

/** Upper bound of the vertical offset of the home app list, in dp. */
object HomeListOffset {
    const val MAX_DP = 480
}

enum class ClockPosition { START, CENTER, END }

/** Interface font: system default, a bundled variable font, or the device sans-serif-condensed. */
enum class AppFont { SYSTEM, INTER, OPEN_SANS, MONTSERRAT, CONDENSED }

/** Font weight; [value] is the numeric weight (100..900 scale). */
enum class AppFontWeight(val value: Int) { LIGHT(value = 300), REGULAR(value = 400), MEDIUM(value = 500) }

/** Color of the gesture trail; [argb] is null for [ACCENT] and [TEXT], which take the palette color. */
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

/** Allowed gesture trail width, in dp. */
object StrokeWidth {
    const val MIN_DP = 2
    const val MAX_DP = 12
    const val DEFAULT_DP = 5
}

/**
 * Global preferences, persisted by `SettingsRepository`.
 *
 * [currentSpaceId] null means the normal space. Per-space options (palette, colored icons, list offset,
 * background) live in [Space]. [homeListOffset] null means the home list position is computed automatically.
 */
data class LauncherSettings(
    val currentSpaceId: Long? = null,
    val iconMode: IconMode = IconMode.NONE,
    val clockFormat: ClockFormat = ClockFormat.SYSTEM,
    val showDate: Boolean = true,
    val clockPosition: ClockPosition = ClockPosition.START,
    val homeListOffset: Int? = null,
    val font: AppFont = AppFont.SYSTEM,
    val fontWeight: AppFontWeight = AppFontWeight.REGULAR,
    val homeTextSize: HomeTextSize = HomeTextSize.M,
    val strokeWidth: Int = StrokeWidth.DEFAULT_DP,
    val strokeColor: StrokeColor = StrokeColor.ACCENT,
    val strokeDots: Boolean = true,
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
