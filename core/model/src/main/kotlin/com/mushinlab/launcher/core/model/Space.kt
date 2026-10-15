package com.mushinlab.launcher.core.model

enum class SpaceType { NORMAL, WORK, CUSTOM }

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

object SpaceLimits {
    const val MAX_WORK = 1
    const val MAX_CUSTOM = 20
    const val NAME_MAX_LENGTH = 24
    const val RECENTS_PER_SPACE = 50
}
