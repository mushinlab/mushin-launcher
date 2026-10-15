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
    const val LABEL_MAX_LENGTH = 40
    const val RECENTS_PER_SPACE = 50
    const val ORPHAN_TTL_MS = 30L * 24 * 60 * 60 * 1000
}
