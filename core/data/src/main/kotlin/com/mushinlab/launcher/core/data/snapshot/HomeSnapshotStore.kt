package com.mushinlab.launcher.core.data.snapshot

import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.ClockFormat
import com.mushinlab.launcher.core.model.ClockPosition
import com.mushinlab.launcher.core.model.HomeTextSize
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.model.Palette
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

data class SnapshotRow(
    val key: AppKey,
    val label: String,
    val dimmed: Boolean,
    val work: Boolean,
)

data class HomeSnapshot(
    val envId: Long,
    val palette: Palette,
    val iconMode: IconMode,
    val textSize: HomeTextSize,
    val clockFormat: ClockFormat,
    val showDate: Boolean,
    val rows: List<SnapshotRow>,
    val clockPosition: ClockPosition = ClockPosition.START,
    val listOffset: Int = 0,
)

class HomeSnapshotStore(directory: File) {
    private val file = File(directory, FILE_NAME)
    private val temp = File(directory, "$FILE_NAME.tmp")

    fun read(): HomeSnapshot? = runCatching {
        if (!file.exists()) return null
        DataInputStream(file.inputStream().buffered()).use { input ->
            if (input.readInt() != VERSION) return null
            val envId = input.readLong()
            val palette = Palette(
                background = input.readInt(),
                zebraA = input.readInt(),
                zebraB = input.readInt(),
                textPrimary = input.readInt(),
                textSecondary = input.readInt(),
                accent = input.readInt(),
                iconTint = input.readInt(),
                divider = input.readInt(),
                surface = input.readInt(),
                keyBackground = input.readInt(),
                keyPressed = input.readInt(),
                isDark = input.readBoolean(),
            )
            val iconMode = IconMode.valueOf(input.readUTF())
            val textSize = HomeTextSize.valueOf(input.readUTF())
            val clockFormat = ClockFormat.valueOf(input.readUTF())
            val showDate = input.readBoolean()
            val clockPosition = ClockPosition.valueOf(input.readUTF())
            val listOffset = input.readInt()
            val rows = List(input.readInt()) {
                SnapshotRow(
                    key = AppKey(input.readUTF(), input.readUTF(), input.readLong()),
                    label = input.readUTF(),
                    dimmed = input.readBoolean(),
                    work = input.readBoolean(),
                )
            }
            HomeSnapshot(envId, palette, iconMode, textSize, clockFormat, showDate, rows, clockPosition, listOffset)
        }
    }.getOrNull()

    fun write(snapshot: HomeSnapshot) {
        runCatching {
            DataOutputStream(temp.outputStream().buffered()).use { out ->
                out.writeInt(VERSION)
                out.writeLong(snapshot.envId)
                with(snapshot.palette) {
                    listOf(
                        background, zebraA, zebraB, textPrimary, textSecondary, accent,
                        iconTint, divider, surface, keyBackground, keyPressed,
                    ).forEach(out::writeInt)
                    out.writeBoolean(isDark)
                }
                out.writeUTF(snapshot.iconMode.name)
                out.writeUTF(snapshot.textSize.name)
                out.writeUTF(snapshot.clockFormat.name)
                out.writeBoolean(snapshot.showDate)
                out.writeUTF(snapshot.clockPosition.name)
                out.writeInt(snapshot.listOffset)
                out.writeInt(snapshot.rows.size)
                snapshot.rows.forEach { row ->
                    out.writeUTF(row.key.packageName)
                    out.writeUTF(row.key.activityName)
                    out.writeLong(row.key.userSerial)
                    out.writeUTF(row.label)
                    out.writeBoolean(row.dimmed)
                    out.writeBoolean(row.work)
                }
            }
            temp.renameTo(file)
        }
    }

    private companion object {
        const val FILE_NAME = "home_snapshot.bin"
        const val VERSION = 3
    }
}
