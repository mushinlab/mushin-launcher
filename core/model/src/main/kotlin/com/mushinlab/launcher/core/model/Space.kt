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
 * overrides the global home list offset in dp.
 */
data class Space(
    val id: Long,
    val name: String,
    val type: SpaceType,
    val sortOrder: Int,
    val preset: PalettePreset,
    val palette: Palette,
    val createdAt: Long,
    val coloredIcons: Boolean = false,
    val listOffset: Int? = null,
)

/** Limits on spaces: count per type, name length in characters and search recents kept per space. */
object SpaceLimits {
    const val MAX_WORK = 1
    const val MAX_CUSTOM = 20
    const val NAME_MAX_LENGTH = 24
    const val RECENTS_PER_SPACE = 50
}
