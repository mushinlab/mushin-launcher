package com.bluecubelabs.env

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import com.bluecubelabs.env.core.model.AppKey

sealed interface Overlay {
    data object None : Overlay

    @Immutable
    data class Popup(val anchor: Offset, val app: AppKey?) : Overlay

    data object Search : Overlay

    @Immutable
    data class Suggestion(val anchor: Offset) : Overlay
}

@Immutable
data class Notice(
    val text: String,
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null,
    val id: Long = System.nanoTime(),
)
