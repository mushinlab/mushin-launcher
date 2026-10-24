package com.mushinlab.launcher.feature.settings

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

enum class PresetFolder { PLAIN, JAPANESE, COSMOS }

@Immutable
sealed interface BackgroundPreset {
    data class Plain(val argb: Int) : BackgroundPreset

    data class Picture(@param:DrawableRes val image: Int, @param:DrawableRes val thumbnail: Int) : BackgroundPreset
}

object BackgroundPresets {
    fun of(folder: PresetFolder): List<BackgroundPreset> = when (folder) {
        PresetFolder.PLAIN -> PlainColors.map { BackgroundPreset.Plain(it.toInt()) }
        PresetFolder.JAPANESE -> Japanese
        PresetFolder.COSMOS -> emptyList()
    }
}

private val PlainColors = listOf(
    0xFF1C1C1E,
    0xFF2B2D42,
    0xFF2F5D62,
    0xFF6B705C,
    0xFFA3B18A,
    0xFFA9C6DE,
    0xFFD8A7B1,
    0xFFF2EDE4,
)

private val Japanese = listOf(
    BackgroundPreset.Picture(R.drawable.bg_jp_fuji_sun, R.drawable.bg_jp_fuji_sun_thumb),
    BackgroundPreset.Picture(R.drawable.bg_jp_sakura, R.drawable.bg_jp_sakura_thumb),
    BackgroundPreset.Picture(R.drawable.bg_jp_mushin, R.drawable.bg_jp_mushin_thumb),
    BackgroundPreset.Picture(R.drawable.bg_jp_torii, R.drawable.bg_jp_torii_thumb),
    BackgroundPreset.Picture(R.drawable.bg_jp_reflection, R.drawable.bg_jp_reflection_thumb),
    BackgroundPreset.Picture(R.drawable.bg_jp_fuji_sun_dark, R.drawable.bg_jp_fuji_sun_dark_thumb),
    BackgroundPreset.Picture(R.drawable.bg_jp_sakura_dark, R.drawable.bg_jp_sakura_dark_thumb),
    BackgroundPreset.Picture(R.drawable.bg_jp_mushin_dark, R.drawable.bg_jp_mushin_dark_thumb),
    BackgroundPreset.Picture(R.drawable.bg_jp_torii_dark, R.drawable.bg_jp_torii_dark_thumb),
    BackgroundPreset.Picture(R.drawable.bg_jp_reflection_dark, R.drawable.bg_jp_reflection_dark_thumb),
)
