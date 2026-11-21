package com.mushinlab.launcher

import androidx.compose.runtime.Immutable

@Immutable
data class PrivateGate(
    val mode: Mode,
    val digits: List<Int> = emptyList(),
    val first: String? = null,
    val error: Boolean = false,
) {
    val pin: String get() = digits.joinToString("")

    enum class Mode { SET, CONFIRM, UNLOCK }

    companion object {
        const val LENGTH = 4
    }
}
