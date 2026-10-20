package com.mushinlab.launcher.core.data.snapshot

import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.ClockFormat
import com.mushinlab.launcher.core.model.ClockPosition
import com.mushinlab.launcher.core.model.HomeListOffset
import com.mushinlab.launcher.core.model.HomeTextAlign
import com.mushinlab.launcher.core.model.HomeTextSize
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.model.Palette
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

/** A home row as shown at the last snapshot; [work] marks apps of the work profile. */
data class SnapshotRow(
    val key: AppKey,
    val label: String,
    val dimmed: Boolean,
    val work: Boolean,
)

/**
 * What the home screen showed last time, enough to draw it before the database and app list load.
 * [listOffset] null means the automatic list position.
 */
data class HomeSnapshot(
    val spaceId: Long,
    val palette: Palette,
    val iconMode: IconMode,
    val textSize: HomeTextSize,
    val clockFormat: ClockFormat,
    val showDate: Boolean,
    val rows: List<SnapshotRow>,
    val clockPosition: ClockPosition = ClockPosition.START,
    val listOffset: Int? = null,
    val textAlign: HomeTextAlign = HomeTextAlign.START,
)

/**
 * Stores one [HomeSnapshot] as a small versioned binary file for an instant first frame after process start.
 *
 * Writes go to a temporary file and are renamed into place. [read] returns null for a missing or corrupt file
 * and for versions before 3; a version 3 file has no text alignment and reads as [HomeTextAlign.START]. Both
 * methods block on file I/O.
 */
class HomeSnapshotStore(directory: File) {
    private val file = File(directory, FILE_NAME)
    private val temp = File(directory, "$FILE_NAME.tmp")

    fun read(): HomeSnapshot? = runCatching {
        if (!file.exists()) return null
        DataInputStream(file.inputStream().buffered()).use { input ->
            val version = input.readInt()
            if (version != VERSION && version != NO_ALIGN_VERSION) return null
            val spaceId = input.readLong()
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
            val listOffset = input.readInt().takeIf { it in 0..HomeListOffset.MAX_DP }
            val textAlign = if (version == VERSION) HomeTextAlign.valueOf(input.readUTF()) else HomeTextAlign.START
            val count = input.readInt()
            require(count in 0..MAX_ROWS)
            val rows = List(count) {
                SnapshotRow(
                    key = AppKey(input.readUTF(), input.readUTF(), input.readLong()),
                    label = input.readUTF(),
                    dimmed = input.readBoolean(),
                    work = input.readBoolean(),
                )
            }
            HomeSnapshot(
                spaceId,
                palette,
                iconMode,
                textSize,
                clockFormat,
                showDate,
                rows,
                clockPosition,
                listOffset,
                textAlign,
            )
        }
    }.getOrNull()

    fun write(snapshot: HomeSnapshot) {
        runCatching {
            DataOutputStream(temp.outputStream().buffered()).use { out ->
                out.writeInt(VERSION)
                out.writeLong(snapshot.spaceId)
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
                out.writeInt(snapshot.listOffset ?: AUTO_LIST_OFFSET)
                out.writeUTF(snapshot.textAlign.name)
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
        const val MAX_ROWS = 512
        const val FILE_NAME = "home_snapshot.bin"
        const val VERSION = 4
        const val NO_ALIGN_VERSION = 3
        const val AUTO_LIST_OFFSET = -1
    }
}
