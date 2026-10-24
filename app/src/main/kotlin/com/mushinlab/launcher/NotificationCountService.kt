package com.mushinlab.launcher

import android.app.Notification
import android.os.UserManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.mushinlab.launcher.core.apps.NotificationCounts
import com.mushinlab.launcher.core.apps.PostedNotification
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class NotificationCountService : NotificationListenerService() {
    private val container get() = (application as MushinApp).container
    private var watch: Job? = null

    override fun onListenerConnected() {
        watch?.cancel()
        watch = container.scope.launch {
            container.settings.loaded.filterNotNull().map { it.notificationCounts }.distinctUntilChanged()
                .collect { on -> if (on) publish() else stop() }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = publishIfOn()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = publishIfOn()

    override fun onListenerDisconnected() {
        watch?.cancel()
        watch = null
        container.notificationCounts.value = emptyMap()
    }

    private fun publishIfOn() {
        if (container.settings.loaded.value?.notificationCounts == true) publish()
    }

    private fun stop() {
        container.notificationCounts.value = emptyMap()
        requestUnbind()
    }

    private fun publish() {
        val users = getSystemService(UserManager::class.java)
        container.notificationCounts.value = NotificationCounts.count(
            activeNotifications.orEmpty().map {
                PostedNotification(
                    packageName = it.packageName,
                    userSerial = users.getSerialNumberForUser(it.user),
                    clearable = it.isClearable,
                    groupSummary = it.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
                )
            },
            packageName,
        )
    }
}
