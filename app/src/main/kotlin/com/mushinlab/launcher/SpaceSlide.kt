package com.mushinlab.launcher

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import com.mushinlab.launcher.core.model.Palette
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.feature.home.HomeUiState

@Immutable
data class SpacePage(
    val id: Long,
    val state: HomeUiState,
    val palette: Palette,
    val background: Bitmap?,
    val coloredIcons: Boolean,
)

@Immutable
data class SpaceSlide(val from: SpacePage, val to: SpacePage, val direction: SwipeDirection)
