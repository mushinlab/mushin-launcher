package com.mushinlab.launcher.core.data.widgets

import com.mushinlab.launcher.core.model.HostedWidget
import com.mushinlab.launcher.core.model.PendingWidget
import com.mushinlab.launcher.core.model.WidgetArrangement
import com.mushinlab.launcher.core.model.WidgetLayout
import com.mushinlab.launcher.core.model.WidgetPanelMode
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
                out.writeUTF(layout.mode.name)
                out.writeUTF(layout.arrangement.name)
                out.writeInt(layout.widgets.size)
                layout.widgets.forEach {
                    out.writeInt(it.id)
                    out.writeUTF(it.label)
                }
            }
            out.writeBoolean(state.pending != null)
            state.pending?.let {
                out.writeLong(it.spaceId)
                out.writeInt(it.id)
            }
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray())
    }

    fun decode(raw: String?): WidgetState = runCatching {
        if (raw == null) return WidgetState()
        DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(raw))).use { input ->
            val version = input.readInt()
            require(version in 1..VERSION)
            val legacy = version < VERSION
            val layouts = buildMap {
                repeat(input.count()) {
                    val id = input.readLong()
                    put(id, if (legacy) input.legacyLayout(version) else input.layout())
                }
            }
            val pending = if (input.readBoolean()) {
                val space = input.readLong()
                if (legacy) input.readUTF()
                PendingWidget(space, input.readInt().also { require(it > HostedWidget.CLOCK_ID) })
            } else {
                null
            }
            WidgetState(layouts, pending)
        }
    }.getOrDefault(WidgetState())

    private fun DataInputStream.layout(): WidgetLayout {
        val mode = WidgetPanelMode.valueOf(readUTF())
        val arrangement = WidgetArrangement.valueOf(readUTF())
        val widgets = List(count()) { HostedWidget(readInt().also { require(it >= 0) }, readUTF()) }
        return WidgetLayout(mode, arrangement, widgets)
    }

    private fun DataInputStream.legacyLayout(version: Int): WidgetLayout {
        val clock = readUTF().isNotEmpty()
        val mode = readUTF()
        val arrangement = readUTF()
        val hosted = List(count()) {
            val id = readInt().also { require(it > HostedWidget.CLOCK_ID) }
            readUTF()
            HostedWidget(id, readUTF())
        }
        val widgets = if (clock) listOf(HostedWidget.clock) + hosted else hosted
        val stored = if (version == 1) WidgetPanelMode.STANDARD else WidgetPanelMode.valueOf(mode)
        return WidgetLayout(
            mode = if (widgets.size > stored.capacity) WidgetPanelMode.EXPANDED else stored,
            arrangement = if (version == 1) WidgetArrangement.ROW else WidgetArrangement.valueOf(arrangement),
            widgets = widgets,
        )
    }

    private fun DataInputStream.count(): Int = readInt().also { require(it in 0..MAX_ENTRIES) }

    private const val VERSION = 3
    private const val MAX_ENTRIES = 512
}
