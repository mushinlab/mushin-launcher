package com.bluecubelabs.env.feature.home

import androidx.compose.runtime.Immutable
import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.ClockFormat
import com.bluecubelabs.env.core.model.ClockPosition
import com.bluecubelabs.env.core.model.HomeTextSize
import com.bluecubelabs.env.core.model.IconMode
import com.bluecubelabs.env.core.model.SwipeDirection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class HomeRow(
    val key: AppKey,
    val label: String,
    val dimmed: Boolean = false,
    val work: Boolean = false,
    val notifications: Int = 0,
    val swipes: ImmutableList<SwipeDirection> = persistentListOf(),
)

@Immutable
data class HomeSuggestion(
    val add: AppKey,
    val addLabel: String,
    val replace: AppKey,
    val replaceLabel: String,
    val canAdd: Boolean,
)

@Immutable
data class HomeUiState(
    val rows: ImmutableList<HomeRow> = persistentListOf(),
    val iconMode: IconMode = IconMode.NONE,
    val textSize: HomeTextSize = HomeTextSize.M,
    val clockFormat: ClockFormat = ClockFormat.SYSTEM,
    val showDate: Boolean = true,
    val clockPosition: ClockPosition = ClockPosition.START,
    val roleMissing: Boolean = false,
    val suggestion: HomeSuggestion? = null,
)
