package com.bluecubelabs.env.core.model

enum class PalettePreset { GRAPHITE, NAVY, PAPER, MOSS, CUSTOM }

data class Palette(
    val background: Int,
    val zebraA: Int,
    val zebraB: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val accent: Int,
    val iconTint: Int,
    val divider: Int,
    val surface: Int,
    val keyBackground: Int,
    val keyPressed: Int,
    val isDark: Boolean,
)

object Palettes {
    val Graphite = Palette(
        background = argb(0x0E0E0E),
        zebraA = argb(0x0E0E0E),
        zebraB = argb(0x171717),
        textPrimary = argb(0xEDEDED),
        textSecondary = argb(0x8A8A8A),
        accent = argb(0xB5C7FF),
        iconTint = argb(0xD0D0D0),
        divider = argb(0x262626),
        surface = argb(0x1A1A1A),
        keyBackground = argb(0x1C1C1C),
        keyPressed = argb(0x2E2E2E),
        isDark = true,
    )

    val Navy = Palette(
        background = argb(0x0B1220),
        zebraA = argb(0x0B1220),
        zebraB = argb(0x111A2C),
        textPrimary = argb(0xE6ECF7),
        textSecondary = argb(0x8C9AB3),
        accent = argb(0x6EA8FE),
        iconTint = argb(0xC9D6EE),
        divider = argb(0x1E2A40),
        surface = argb(0x131D30),
        keyBackground = argb(0x152036),
        keyPressed = argb(0x22324F),
        isDark = true,
    )

    val Paper = Palette(
        background = argb(0xF5F5F2),
        zebraA = argb(0xF5F5F2),
        zebraB = argb(0xEBEBE7),
        textPrimary = argb(0x141414),
        textSecondary = argb(0x5F5F5F),
        accent = argb(0x2F5BD3),
        iconTint = argb(0x2A2A2A),
        divider = argb(0xD9D9D4),
        surface = argb(0xFFFFFF),
        keyBackground = argb(0xE6E6E1),
        keyPressed = argb(0xD2D2CC),
        isDark = false,
    )

    val Moss = Palette(
        background = argb(0x0F130F),
        zebraA = argb(0x0F130F),
        zebraB = argb(0x161C16),
        textPrimary = argb(0xE4EBE2),
        textSecondary = argb(0x8C9989),
        accent = argb(0x9FD39A),
        iconTint = argb(0xCBD6C8),
        divider = argb(0x243024),
        surface = argb(0x182018),
        keyBackground = argb(0x1A221A),
        keyPressed = argb(0x283428),
        isDark = true,
    )

    val presets: Map<PalettePreset, Palette> = mapOf(
        PalettePreset.GRAPHITE to Graphite,
        PalettePreset.NAVY to Navy,
        PalettePreset.PAPER to Paper,
        PalettePreset.MOSS to Moss,
    )

    fun of(preset: PalettePreset): Palette = presets[preset] ?: Graphite

    private fun argb(rgb: Long): Int = (0xFF000000 or rgb).toInt()
}
