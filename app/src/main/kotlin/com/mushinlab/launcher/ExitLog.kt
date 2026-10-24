package com.mushinlab.launcher

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import java.io.File

class ExitLog(private val context: Context) {
    private val file = File(context.filesDir, FILE_NAME)
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun recordNewExits() {
        val manager = context.getSystemService(ActivityManager::class.java) ?: return
        val lastSeen = prefs.getLong(KEY_LAST_SEEN, 0L)
        val exits = runCatching { manager.getHistoricalProcessExitReasons(context.packageName, 0, MAX_READ) }
            .getOrDefault(emptyList())
            .filter { it.timestamp > lastSeen }
            .sortedBy { it.timestamp }
        if (exits.isEmpty()) return
        val lines = exits.map(::format)
        val kept = (readLines() + lines).takeLast(MAX_LINES)
        file.writeText(kept.joinToString("\n", postfix = "\n"))
        prefs.edit().putLong(KEY_LAST_SEEN, exits.last().timestamp).apply()
    }

    fun readLines(): List<String> = if (file.exists()) file.readLines().filter { it.isNotBlank() } else emptyList()

    private fun format(info: ApplicationExitInfo): String {
        val memoryLimiter = info.description?.contains(MEMORY_LIMITER) == true
        return listOf(
            info.timestamp.toString(),
            "reason=${info.reason}",
            "importance=${info.importance}",
            "pss=${info.pss}",
            "rss=${info.rss}",
            if (memoryLimiter) "MEMORY_LIMITER" else "-",
        ).joinToString("\t")
    }

    private companion object {
        const val FILE_NAME = "exit_log.tsv"
        const val PREFS = "exit_log"
        const val KEY_LAST_SEEN = "last_seen"
        const val MEMORY_LIMITER = "MemoryLimiter"
        const val MAX_READ = 16
        const val MAX_LINES = 200
    }
}
