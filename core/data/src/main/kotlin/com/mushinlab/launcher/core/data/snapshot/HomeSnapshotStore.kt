package com.mushinlab.launcher.core.data.snapshot

import com.mushinlab.launcher.core.data.widgets.WidgetStateCodec
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.ClockFormat
import com.mushinlab.launcher.core.model.ClockPosition
import com.mushinlab.launcher.core.model.ClockSize
import com.mushinlab.launcher.core.model.HomeListOffset
import com.mushinlab.launcher.core.model.HomeTextAlign
import com.mushinlab.launcher.core.model.HomeTextSize
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.model.Palette
import com.mushinlab.launcher.core.model.WidgetLayout
import com.mushinlab.launcher.core.model.WidgetState
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
    val showClock: Boolean = true,
    val clockSize: ClockSize = ClockSize.M,
    val widgetLayout: WidgetLayout = WidgetLayout(),
)

/**
 * Stores one [HomeSnapshot] as a small versioned binary file for an instant first frame after process start.
 *
 * Writes go to a temporary file and are renamed into place. [read] returns null for a missing or corrupt file
 * and for versions before 3; a version 3 file has no text alignment and reads as [HomeTextAlign.START], and
 * files before version 5 have no clock visibility and size and read as a shown medium clock. Version 6 adds widget
 * panel layouts; older snapshots use the built-in time in the top panel. Both methods block on
 * file I/O.
 */
class HomeSnapshotStore(directory: File) {
    private val file = File(directory, FILE_NAME)
    private val temp = File(directory, "$FILE_NAME.tmp")

    fun read(): HomeSnapshot? = runCatching {
        if (!file.exists()) return null
        DataInputStream(file.inputStream().buffered()).use { input ->
            val version = input.readInt()
            if (version !in NO_ALIGN_VERSION..VERSION) return null
            val spaceId = input.readLong()
            val palette = input.readPalette()
            val iconMode = IconMode.valueOf(input.readUTF())
            val textSize = HomeTextSize.valueOf(input.readUTF())
            val clockFormat = ClockFormat.valueOf(input.readUTF())
            val showDate = input.readBoolean()
            val clockPosition = ClockPosition.valueOf(input.readUTF())
            val listOffset = input.readInt().takeIf { it in 0..HomeListOffset.MAX_DP }
            val textAlign = if (version >
                NO_ALIGN_VERSION
            ) {
                HomeTextAlign.valueOf(input.readUTF())
            } else {
                HomeTextAlign.START
            }
            val showClock = if (version >= CLOCK_VERSION) input.readBoolean() else true
            val clockSize = if (version >= CLOCK_VERSION) ClockSize.valueOf(input.readUTF()) else ClockSize.M
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
            val widgetLayout = if (version >= WIDGET_VERSION) {
                WidgetStateCodec.decode(input.readUTF()).layouts[spaceId] ?: WidgetLayout()
            } else {
                WidgetLayout()
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
                showClock,
                clockSize,
                widgetLayout,
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
                out.writeBoolean(snapshot.showClock)
                out.writeUTF(snapshot.clockSize.name)
                out.writeInt(snapshot.rows.size)
                snapshot.rows.forEach { row ->
                    out.writeUTF(row.key.packageName)
                    out.writeUTF(row.key.activityName)
                    out.writeLong(row.key.userSerial)
                    out.writeUTF(row.label)
                    out.writeBoolean(row.dimmed)
                    out.writeBoolean(row.work)
                }
                out.writeUTF(WidgetStateCodec.encode(WidgetState(mapOf(snapshot.spaceId to snapshot.widgetLayout))))
            }
            temp.renameTo(file)
        }
    }

    private fun DataInputStream.readPalette() = Palette(
        background = readInt(),
        zebraA = readInt(),
        zebraB = readInt(),
        textPrimary = readInt(),
        textSecondary = readInt(),
        accent = readInt(),
        iconTint = readInt(),
        divider = readInt(),
        surface = readInt(),
        keyBackground = readInt(),
        keyPressed = readInt(),
        isDark = readBoolean(),
    )

    private companion object {
        const val MAX_ROWS = 512
        const val FILE_NAME = "home_snapshot.bin"
        const val VERSION = 6
        const val WIDGET_VERSION = 6
        const val CLOCK_VERSION = 5
        const val NO_ALIGN_VERSION = 3
        const val AUTO_LIST_OFFSET = -1
    }
}
