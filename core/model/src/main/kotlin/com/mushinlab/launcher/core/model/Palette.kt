package com.mushinlab.launcher.core.model

enum class PalettePreset { GRAPHITE, NAVY, PAPER, MOSS, NIGHT, SAND, LAGOON, PLUM, EMBER, MIST, ROSE, CUSTOM }

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
        background = argb(0x121212),
        zebraA = argb(0x121212),
        zebraB = argb(0x1C1C1C),
        textPrimary = argb(0xDEDEDE),
        textSecondary = argb(0x9E9E9E),
        accent = argb(0xB5C7FF),
        iconTint = argb(0xC8C8C8),
        divider = argb(0x2A2A2A),
        surface = argb(0x1E1E1E),
        keyBackground = argb(0x2C2C2C),
        keyPressed = argb(0x3C3C3C),
        isDark = true,
    )

    val Navy = Palette(
        background = argb(0x0E1522),
        zebraA = argb(0x0E1522),
        zebraB = argb(0x16202F),
        textPrimary = argb(0xDCE3EE),
        textSecondary = argb(0x8F9DB5),
        accent = argb(0x8AB4F8),
        iconTint = argb(0xC3CFE3),
        divider = argb(0x22304A),
        surface = argb(0x16213A),
        keyBackground = argb(0x233149),
        keyPressed = argb(0x2F4060),
        isDark = true,
    )

    val Paper = Palette(
        background = argb(0xF1F0EC),
        zebraA = argb(0xF1F0EC),
        zebraB = argb(0xE6E5E0),
        textPrimary = argb(0x1F1F1F),
        textSecondary = argb(0x5E5E5E),
        accent = argb(0x2F5BD3),
        iconTint = argb(0x2A2A2A),
        divider = argb(0xD6D5D0),
        surface = argb(0xFAF9F6),
        keyBackground = argb(0xE3E2DD),
        keyPressed = argb(0xCFCEC8),
        isDark = false,
    )

    val Moss = Palette(
        background = argb(0x111611),
        zebraA = argb(0x111611),
        zebraB = argb(0x1A221A),
        textPrimary = argb(0xDAE2D8),
        textSecondary = argb(0x8F9C8C),
        accent = argb(0x9FD39A),
        iconTint = argb(0xC6D1C3),
        divider = argb(0x263226),
        surface = argb(0x1B241B),
        keyBackground = argb(0x263226),
        keyPressed = argb(0x334233),
        isDark = true,
    )

    val Night = Palette(
        background = argb(0x000000),
        zebraA = argb(0x000000),
        zebraB = argb(0x0D0D0D),
        textPrimary = argb(0xC7C7C7),
        textSecondary = argb(0x7A7A7A),
        accent = argb(0xE8B36B),
        iconTint = argb(0xA8A8A8),
        divider = argb(0x1F1F1F),
        surface = argb(0x121212),
        keyBackground = argb(0x222222),
        keyPressed = argb(0x333333),
        isDark = true,
    )

    val Sand = Palette(
        background = argb(0xEEE7DA),
        zebraA = argb(0xEEE7DA),
        zebraB = argb(0xE4DCCD),
        textPrimary = argb(0x2B2620),
        textSecondary = argb(0x675D51),
        accent = argb(0x8C5218),
        iconTint = argb(0x3A332B),
        divider = argb(0xD6CCBA),
        surface = argb(0xF6F1E8),
        keyBackground = argb(0xE2D9C9),
        keyPressed = argb(0xD0C5B2),
        isDark = false,
    )

    val Lagoon = Palette(
        background = argb(0x0D1717),
        zebraA = argb(0x0D1717),
        zebraB = argb(0x152222),
        textPrimary = argb(0xD6E8E6),
        textSecondary = argb(0x8CA8A5),
        accent = argb(0x6FD6CB),
        iconTint = argb(0xBFD6D3),
        divider = argb(0x1F3333),
        surface = argb(0x142222),
        keyBackground = argb(0x1E3030),
        keyPressed = argb(0x2A4140),
        isDark = true,
    )

    val Plum = Palette(
        background = argb(0x17121C),
        zebraA = argb(0x17121C),
        zebraB = argb(0x211A28),
        textPrimary = argb(0xE6DCEE),
        textSecondary = argb(0xA597B3),
        accent = argb(0xD7A8F0),
        iconTint = argb(0xD2C6DD),
        divider = argb(0x2C2236),
        surface = argb(0x221A2A),
        keyBackground = argb(0x2E2438),
        keyPressed = argb(0x3D3049),
        isDark = true,
    )

    val Ember = Palette(
        background = argb(0x1A1411),
        zebraA = argb(0x1A1411),
        zebraB = argb(0x251D18),
        textPrimary = argb(0xEDE0D6),
        textSecondary = argb(0xAE9A8B),
        accent = argb(0xF0A070),
        iconTint = argb(0xD8C9BD),
        divider = argb(0x33281F),
        surface = argb(0x241C17),
        keyBackground = argb(0x30261F),
        keyPressed = argb(0x40332A),
        isDark = true,
    )

    val Mist = Palette(
        background = argb(0xE9EEF2),
        zebraA = argb(0xE9EEF2),
        zebraB = argb(0xDDE4EA),
        textPrimary = argb(0x1B232B),
        textSecondary = argb(0x51606E),
        accent = argb(0x2A62A8),
        iconTint = argb(0x26313C),
        divider = argb(0xCDD6DE),
        surface = argb(0xF5F8FA),
        keyBackground = argb(0xDAE1E8),
        keyPressed = argb(0xC5CFD9),
        isDark = false,
    )

    val Rose = Palette(
        background = argb(0xF4E9EA),
        zebraA = argb(0xF4E9EA),
        zebraB = argb(0xEADCDE),
        textPrimary = argb(0x2A1E20),
        textSecondary = argb(0x6B5558),
        accent = argb(0xA83A55),
        iconTint = argb(0x3A2B2E),
        divider = argb(0xE0CDD0),
        surface = argb(0xFBF4F5),
        keyBackground = argb(0xE8D8DB),
        keyPressed = argb(0xD8C3C7),
        isDark = false,
    )

    val presets: Map<PalettePreset, Palette> = mapOf(
        PalettePreset.GRAPHITE to Graphite,
        PalettePreset.NIGHT to Night,
        PalettePreset.NAVY to Navy,
        PalettePreset.MOSS to Moss,
        PalettePreset.LAGOON to Lagoon,
        PalettePreset.PLUM to Plum,
        PalettePreset.EMBER to Ember,
        PalettePreset.PAPER to Paper,
        PalettePreset.SAND to Sand,
        PalettePreset.MIST to Mist,
        PalettePreset.ROSE to Rose,
    )

    fun of(preset: PalettePreset): Palette = presets[preset] ?: Graphite

    private fun argb(rgb: Long): Int = (0xFF000000 or rgb).toInt()
}
