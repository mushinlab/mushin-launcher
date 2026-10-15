package com.mushinlab.launcher.core.apps

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log

/** Opens the notification shade; [expand] returns false when the system refused. */
fun interface NotificationShade {
    fun expand(): Boolean
}

/**
 * Calls the hidden StatusBarManager.expandNotificationsPanel by reflection, the single exception to the
 * no-reflection rule (tech spec D.13). Call it only while the activity is resumed.
 */
class ReflectionNotificationShade(private val context: Context) : NotificationShade {
    @SuppressLint("WrongConstant")
    override fun expand(): Boolean = runCatching {
        val manager = context.getSystemService(STATUS_BAR_SERVICE) ?: error("no status bar service")
        manager.javaClass.getMethod("expandNotificationsPanel").invoke(manager)
        true
    }.getOrElse {
        Log.w(TAG, "expandNotificationsPanel failed", it)
        false
    }

    private companion object {
        const val TAG = "NotificationShade"
        const val STATUS_BAR_SERVICE = "statusbar"
    }
}
