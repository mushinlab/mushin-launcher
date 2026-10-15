package com.mushinlab.launcher

import com.mushinlab.launcher.core.apps.LaunchResult
import com.mushinlab.launcher.core.model.AppEntry
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.AppOverride
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.LearnedGesture
import com.mushinlab.launcher.core.model.Space
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.search.GestureSample

interface LauncherModel {
    val settings: LauncherSettings
    val gestureBindings: Map<SwipeDirection, GestureAction>
    val learnedGestures: List<LearnedGesture>
    val currentSpace: Space?

    fun entry(key: AppKey): AppEntry?

    fun override(key: AppKey): AppOverride?

    fun isInCurrentSpace(key: AppKey): Boolean

    fun launch(key: AppKey, fromSearch: Boolean, animate: Boolean, gestures: List<GestureSample>): LaunchResult

    fun expandShade(): Boolean

    fun addToCurrentSpace(key: AppKey): Boolean

    fun removeFromCurrentSpace(key: AppKey)

    fun setHidden(key: AppKey, hidden: Boolean)

    fun setActiveLayout(id: String)
}
