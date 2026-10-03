package com.bluecubelabs.env.core.model

data class AppKey(
    val packageName: String,
    val activityName: String,
    val userSerial: Long,
) {
    val id: String get() = "$packageName/$activityName/$userSerial"
}
