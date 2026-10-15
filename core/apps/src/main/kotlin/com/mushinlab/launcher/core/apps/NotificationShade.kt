package com.mushinlab.launcher.core.apps

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log

fun interface NotificationShade {
    fun expand(): Boolean
}

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
