package com.mushinlab.launcher.core.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset

/** A raw gesture sample with a monotonic event time; used only for rendering. */
@Immutable
data class BrushPoint(val position: Offset, val timeMs: Long)
