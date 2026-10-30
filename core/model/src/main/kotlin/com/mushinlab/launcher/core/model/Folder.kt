package com.mushinlab.launcher.core.model

/**
 * A named group of apps that takes one row of a space's home list (ADR-0035). [name] may be blank: the interface
 * then shows a default name. [apps] are in the user's order.
 */
data class Folder(val id: Long, val spaceId: Long, val name: String, val apps: List<AppKey>)

/**
 * Home-list keys of folders. A folder keeps its place in the home list as an [AppKey] with the package [PACKAGE],
 * the folder id as the activity and the user serial [USER_SERIAL], so ordering and capacity treat it as any row.
 * No installed app can have such a key.
 */
object FolderKeys {
    const val PACKAGE = "mushin.folder"
    const val USER_SERIAL = -1L

    /** The home-list key of the folder [id]. */
    fun of(id: Long): AppKey = AppKey(PACKAGE, id.toString(), USER_SERIAL)

    /** The folder id behind [key], or null when [key] is an app. */
    fun idOf(key: AppKey): Long? =
        if (key.packageName == PACKAGE && key.userSerial == USER_SERIAL) key.activityName.toLongOrNull() else null
}
