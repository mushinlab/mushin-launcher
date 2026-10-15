package com.mushinlab.launcher

import android.app.Activity
import android.app.StatusBarManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
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

    fun openAssistantSettings() {
        try {
            activity.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            activity.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        }
    }

    fun requestSearchTile() {
        val manager = activity.getSystemService(StatusBarManager::class.java) ?: return
        manager.requestAddTileService(
            ComponentName(activity, SearchTileService::class.java),
            activity.getString(R.string.tile_search),
            Icon.createWithResource(activity, R.drawable.ic_tile_search),
            activity.mainExecutor,
        ) { result ->
            if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                notify(Notice(R.string.notice_tile_added))
            }
        }
    }
}
