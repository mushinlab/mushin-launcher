package com.mushinlab.launcher

import android.app.Notification
import android.os.UserManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.mushinlab.launcher.core.apps.NotificationCounts
import com.mushinlab.launcher.core.apps.PostedNotification

class NotificationCountService : NotificationListenerService() {
    private val container get() = (application as EnvApp).container

    override fun onListenerConnected() = publish()

    override fun onNotificationPosted(sbn: StatusBarNotification?) = publish()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = publish()

    override fun onListenerDisconnected() {
        container.notificationCounts.value = emptyMap()
    }

    private fun publish() {
        if (container.settings.loaded.value?.notificationCounts == false) {
            container.notificationCounts.value = emptyMap()
            requestUnbind()
            return
        }
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
