package com.mushinlab.launcher.core.data.widgets

import com.mushinlab.launcher.core.model.HostedWidget
import com.mushinlab.launcher.core.model.PendingWidget
import com.mushinlab.launcher.core.model.WidgetCell
import com.mushinlab.launcher.core.model.WidgetLayout
import com.mushinlab.launcher.core.model.WidgetState
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Base64

internal object WidgetStateCodec {
    fun encode(state: WidgetState): String {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(VERSION)
            out.writeInt(state.layouts.size)
            state.layouts.forEach { (id, layout) ->
                out.writeLong(id)
                out.writeInt(layout.widgets.size)
                layout.widgets.forEach {
                    out.writeInt(it.id)
                    out.writeUTF(it.label)
                    out.writeCell(it.cell)
                }
            }
            out.writeBoolean(state.pending != null)
            state.pending?.let {
                out.writeLong(it.spaceId)
                out.writeInt(it.id)
                out.writeBoolean(it.cell != null)
                it.cell?.let { cell -> out.writeCell(cell) }
            }
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray())
    }

    fun decode(raw: String?): WidgetState = runCatching {
        if (raw == null) return WidgetState()
        DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(raw))).use { input ->
            val version = input.readInt()
            require(version in 1..VERSION)
            val layouts = buildMap {
                repeat(input.count()) {
                    val id = input.readLong()
                    put(id, if (version == VERSION) input.layout() else input.legacyLayout(version))
                }
            }
            val pending = if (input.readBoolean()) {
                val space = input.readLong()
                if (version < LIST_VERSION) input.readUTF()
                val id = input.readInt().also { require(it > HostedWidget.CLOCK_ID) }
                val cell = if (version == VERSION && input.readBoolean()) input.readCell() else null
                PendingWidget(space, id, cell)
            } else {
                null
            }
            WidgetState(layouts, pending)
        }
    }.getOrDefault(WidgetState())

    private fun DataInputStream.layout(): WidgetLayout {
        val widgets = List(count()) {
            HostedWidget(readInt().also { require(it >= 0) }, readUTF(), readCell())
        }
        require(widgets.map { it.id }.distinct().size == widgets.size)
        widgets.forEachIndexed { index, widget ->
            require(widgets.drop(index + 1).none { it.cell.overlaps(widget.cell) })
        }
        return WidgetLayout(widgets)
    }

    private fun DataInputStream.legacyLayout(version: Int): WidgetLayout {
        val legacyClock = version < LIST_VERSION && readUTF().isNotEmpty()
        val mode = readUTF()
        val arrangement = readUTF()
        val listed = List(count()) {
            val id = readInt().also { require(it >= if (version < LIST_VERSION) 1 else 0) }
            if (version < LIST_VERSION) readUTF()
            HostedWidget(id, readUTF())
        }
        val all = if (legacyClock) listOf(HostedWidget.clock) + listed else listed
        require(all.map { it.id }.distinct().size == all.size)
        val widgets = if (all.size > WidgetCell.singles.size) all.filterNot { it.isClock } else all
        val column = version > 1 && mode == STANDARD && arrangement == COLUMN && widgets.size <= 2
        val cells = if (column) listOf(WidgetCell(0, 0), WidgetCell(0, 1)) else WidgetCell.singles
        return WidgetLayout(widgets.zip(cells) { widget, cell -> widget.copy(cell = cell) })
    }

    private fun DataOutputStream.writeCell(cell: WidgetCell) {
        writeByte(cell.column)
        writeByte(cell.row)
        writeByte(cell.columnSpan)
        writeByte(cell.rowSpan)
    }

    private fun DataInputStream.readCell(): WidgetCell =
        WidgetCell(readByte().toInt(), readByte().toInt(), readByte().toInt(), readByte().toInt())
            .also { require(it.fits) }

    private fun DataInputStream.count(): Int = readInt().also { require(it in 0..MAX_ENTRIES) }

    private const val VERSION = 4
    private const val LIST_VERSION = 3
    private const val STANDARD = "STANDARD"
    private const val COLUMN = "COLUMN"
    private const val MAX_ENTRIES = 512
}
