package com.mushinlab.launcher

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import com.mushinlab.launcher.core.model.AppKey

sealed interface Overlay {
    data object None : Overlay

    @Immutable
    data class Popup(val anchor: Offset, val app: AppKey?) : Overlay

    data object Search : Overlay

    @Immutable
    data class Suggestion(val anchor: Offset) : Overlay

    @Immutable
    data class Folder(val id: Long, val anchor: Offset) : Overlay
}

@Immutable
data class Notice(
    @param:StringRes val text: Int,
    val arg: String? = null,
    @param:StringRes val actionLabel: Int? = null,
    val action: (() -> Unit)? = null,
    val id: Long = System.nanoTime(),
)

sealed interface LauncherEffect {
    data object Reject : LauncherEffect

    data object OpenClock : LauncherEffect

    data object RequestHomeRole : LauncherEffect

    data object OpenNotificationAccess : LauncherEffect

    data class PickBackground(val spaceId: Long) : LauncherEffect
}
