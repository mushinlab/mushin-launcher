package com.mushinlab.launcher.feature.search

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.search.KeyboardLayout

@Immutable
data class SearchChrome(
    val iconMode: IconMode,
    val layout: KeyboardLayout,
    val nextLayoutLabel: String,
    val digitHints: Boolean,
    val haptics: Boolean,
    val gestureShiftPenalty: Float,
    val gestureSuggestMargin: Float,
    val keyboardWidth: Float = 1f,
    val keyboardShift: Float = 0f,
    val fuzzy: Boolean = true,
    val layoutSwap: Boolean = true,
    val translit: Boolean = true,
    val coloredIcons: Boolean = false,
)

interface SearchActions : KeyboardActions, SearchMenuActions, SearchOptionsActions {
    fun onLaunch(key: AppKey)

    fun onShowKeyboard()

    fun onLongPress(key: AppKey, label: String, anchor: Offset)

    fun onKeyboardFit(width: Float, shift: Float)
}

object SearchDimens {
    val resultHeight: Dp = 56.dp
    val queryHeight: Dp = 36.dp
    val queryTextSize: TextUnit = 18.sp
    val padding: Dp = 16.dp
    val resultIcon: Dp = 24.dp
    val recentDivider: Dp = 1.dp
    val deleteWidth: Dp = 48.dp
    val deleteHeight: Dp = 32.dp
    val deleteTouchWidth: Dp = 56.dp
    val paneTop: Dp = KeyboardDimens.verticalPadding + KeyboardDimens.gap / 2
    val anchorLength: Dp = 48.dp
    val anchorThickness: Dp = 6.dp
    val anchorInset: Dp = 10.dp
    val anchorTouch: Dp = 32.dp
    val suggestionIcon: Dp = 28.dp
    val suggestionGap: Dp = 12.dp
    val headerHeight: Dp = 48.dp
    val headerGap: Dp = 6.dp
    val paneGap: Dp = 8.dp
    val paneRadius: Dp = 16.dp
    val closeSwipe: Dp = 72.dp
}
