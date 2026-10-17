package com.mushinlab.launcher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.provider.AlarmClock
import android.provider.Settings

class SystemScreens(private val activity: Activity, private val notify: (Notice) -> Unit) {
    fun openClock() {
        try {
            activity.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            notify(Notice(R.string.notice_no_clock))
        }
    }

    fun openNotificationAccess(listener: ComponentName) {
        val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, listener.flattenToString())
        try {
            activity.startActivity(detail)
        } catch (_: ActivityNotFoundException) {
            activity.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }
}
