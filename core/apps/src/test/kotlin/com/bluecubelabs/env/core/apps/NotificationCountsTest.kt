package com.bluecubelabs.env.core.apps

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationCountsTest {
    private fun posted(pkg: String, user: Long = 0, clearable: Boolean = true, summary: Boolean = false) =
        PostedNotification(pkg, user, clearable, summary)

    @Test
    fun countsClearableNotificationsPerPackageAndUser() {
        val counts = NotificationCounts.count(
            listOf(
                posted("chat"),
                posted("chat"),
                posted("chat", summary = true),
                posted("chat", user = 10),
                posted("music", clearable = false),
                posted("env"),
            ),
            ownPackage = "env",
        )
        assertThat(counts).containsExactly(
            NotificationSource("chat", 0),
            2,
            NotificationSource("chat", 10),
            1,
        )
    }
}
