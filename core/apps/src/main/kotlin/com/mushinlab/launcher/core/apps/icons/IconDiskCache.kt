package com.mushinlab.launcher.core.apps.icons

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest

class IconDiskCache(private val dir: File) {
    fun read(key: IconKey): LoadedIcon? {
        val file = file(key)
        if (!file.exists()) return null
        return runCatching {
            DataInputStream(file.inputStream().buffered()).use { input ->
                when (val size = input.readInt()) {
                    LETTER_MARK -> LoadedIcon.Letter
                    key.sizePx -> LoadedIcon.Mask(alphaMask(ByteArray(size * size).also(input::readFully), size))
                    else -> null
                }
            }
        }.getOrNull()
    }

    fun write(key: IconKey, icon: LoadedIcon) {
        if (icon is LoadedIcon.Full) return
        runCatching {
            dir.mkdirs()
            DataOutputStream(file(key).outputStream().buffered()).use { out ->
                if (icon is LoadedIcon.Mask) out.writeMask(icon) else out.writeInt(LETTER_MARK)
            }
        }
    }

    fun invalidate(packageName: String, userSerial: Long) {
        val prefix = packagePrefix(packageName, userSerial)
        dir.listFiles { file -> file.name.startsWith(prefix) }?.forEach { it.delete() }
    }

    private fun DataOutputStream.writeMask(icon: LoadedIcon.Mask) {
        val size = icon.bitmap.width
        val buffer = ByteBuffer.allocate(icon.bitmap.byteCount)
        icon.bitmap.copyPixelsToBuffer(buffer)
        writeInt(size)
        val raw = buffer.array()
        for (row in 0 until size) write(raw, row * icon.bitmap.rowBytes, size)
    }

    private fun file(key: IconKey): File {
        val rest = sha1("${key.app.activityName}|${key.lastUpdateTime}|${key.sizePx}|$FORMAT_VERSION")
        return File(dir, packagePrefix(key.app.packageName, key.app.userSerial) + rest)
    }

    private fun packagePrefix(packageName: String, userSerial: Long) =
        sha1("$packageName|$userSerial").take(PREFIX_LENGTH) + "_"

    private fun sha1(text: String): String = MessageDigest.getInstance("SHA-1")
        .digest(text.toByteArray())
        .joinToString("") { "%02x".format(it) }

    private companion object {
        const val LETTER_MARK = 0
        const val FORMAT_VERSION = 1
        const val PREFIX_LENGTH = 16
    }
}
