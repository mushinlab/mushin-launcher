package com.mushinlab.launcher

import com.mushinlab.launcher.core.apps.LaunchResult
import com.mushinlab.launcher.core.model.AppKey

class AppLaunches(private val model: LauncherModel, private val navigator: LauncherNavigator) {
    fun launch(key: AppKey, fromSearch: Boolean, animate: Boolean = true): Boolean {
        val known = model.entry(key) != null
        val gestures = if (fromSearch) navigator.search.gestures.toLearn(key) else emptyList()
        val result = model.launch(key, fromSearch, animate, gestures)
        when (result) {
            LaunchResult.Started -> if (fromSearch) navigator.closeOverlays()
            LaunchResult.UnlockRequested -> Unit
            LaunchResult.Suspended -> navigator.notice = Notice(R.string.notice_suspended)
            LaunchResult.Failed -> navigator.notice = failureNotice(key)
        }
        if (!known && !fromSearch) navigator.notice = Notice(R.string.notice_launch_failed)
        return result == LaunchResult.Started
    }

    private fun failureNotice(key: AppKey): Notice = if (model.isInCurrentSpace(key)) {
        Notice(
            text = R.string.notice_launch_failed,
            actionLabel = R.string.notice_remove_from_space,
            action = { model.removeFromCurrentSpace(key) },
        )
    } else {
        Notice(R.string.notice_launch_failed)
    }
}
