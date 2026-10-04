package com.bluecubelabs.env.core.model

enum class EnvType { NORMAL, WORK, CUSTOM }

data class Environment(
    val id: Long,
    val name: String,
    val type: EnvType,
    val sortOrder: Int,
    val preset: PalettePreset,
    val palette: Palette,
    val createdAt: Long,
    val coloredIcons: Boolean = false,
)

object EnvLimits {
    const val MAX_WORK = 1
    const val MAX_CUSTOM = 20
    const val NAME_MAX_LENGTH = 24
    const val LABEL_MAX_LENGTH = 40
    const val RECENTS_PER_ENV = 50
    const val ORPHAN_TTL_MS = 30L * 24 * 60 * 60 * 1000
}
