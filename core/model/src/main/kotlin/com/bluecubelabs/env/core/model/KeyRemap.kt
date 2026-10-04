package com.bluecubelabs.env.core.model

object KeyRemap {
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
