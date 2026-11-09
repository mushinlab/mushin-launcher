package com.mushinlab.launcher.feature.settings

import androidx.compose.runtime.Immutable

@Immutable
data class WidgetLabel(val id: Int, val label: String, val app: String? = null, val clock: Boolean = false)
