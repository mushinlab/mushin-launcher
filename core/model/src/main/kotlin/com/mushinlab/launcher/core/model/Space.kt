package com.mushinlab.launcher.core.model

/**
 * Kind of space. Exactly one [NORMAL] space exists and cannot be deleted; [SpaceLimits] caps [WORK] and
 * [CUSTOM] spaces.
 */
enum class SpaceType { NORMAL, WORK, CUSTOM }

/**
 * A space: its own home list, palette and search recents.
 *
 * [palette] comes from [preset] unless the preset is [PalettePreset.CUSTOM]. [listOffset], when not null,
 * overrides the global home list offset in dp. [backgroundBrightness] and [backgroundBlur] adjust the space's
 * background image within [SpaceBackground] limits; the image itself is a file, not part of the model. [createdAt]
 * also starts the space's hint statistics, and [hintAt] is when a usefulness hint was last accepted or dismissed
 * (ADR-0025).
 */
data class Space(
    val id: Long,
    val name: String,
    val type: SpaceType,
    val sortOrder: Int,
    val preset: PalettePreset,
    val palette: Palette,
    val createdAt: Long,
    val coloredIcons: Boolean = true,
    val listOffset: Int? = null,
    val backgroundBrightness: Float = 0f,
    val backgroundBlur: Float = 0f,
    val hintAt: Long? = null,
)

/**
 * Limits of the background look of a space (ADR-0020): brightness from [BRIGHTNESS_MIN] (darkest) through 0
 * (unchanged) to [BRIGHTNESS_MAX] (lightest), blur from 0 to [BLUR_MAX]. Sliders move in [STEP].
 */
object SpaceBackground {
    const val BRIGHTNESS_MIN = -1f
    const val BRIGHTNESS_MAX = 1f
    const val BLUR_MAX = 1f
    const val STEP = 0.05f

    /** [value] limited to the brightness range; NaN becomes 0. */
    fun brightness(value: Float): Float = if (value.isNaN()) 0f else value.coerceIn(BRIGHTNESS_MIN, BRIGHTNESS_MAX)

    /** [value] limited to the blur range; NaN becomes 0. */
    fun blur(value: Float): Float = if (value.isNaN()) 0f else value.coerceIn(0f, BLUR_MAX)
}

/** Limits on spaces: count per type, name length in characters and search recents kept per space. */
object SpaceLimits {
    const val MAX_WORK = 1
    const val MAX_CUSTOM = 20
    const val NAME_MAX_LENGTH = 24
    const val RECENTS_PER_SPACE = 50
}
