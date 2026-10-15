package com.mushinlab.launcher.core.model

/**
 * Moves data stored for an app (space items, renames, history, gestures) when the app renames its launcher
 * activity.
 */
object KeyRemap {
    /**
     * Finds stored keys whose activity no longer exists while their package is still installed in the same profile.
     *
     * Returns old key to new key only when the package now has exactly one launchable activity, so stored data can
     * follow an app that renamed its launcher activity without guessing.
     */
    fun moves(stored: Collection<AppKey>, available: Collection<AppKey>): Map<AppKey, AppKey> {
        val present = available.toHashSet()
        val byPackage = available.groupBy { it.packageName to it.userSerial }
        return stored.asSequence()
            .distinct()
            .filterNot { it in present }
            .mapNotNull { key -> byPackage[key.packageName to key.userSerial]?.singleOrNull()?.let { key to it } }
            .toMap()
    }
}
