package com.mushinlab.launcher.core.data.widgets

import com.mushinlab.launcher.core.model.HostedWidget
import com.mushinlab.launcher.core.model.PendingWidget
import com.mushinlab.launcher.core.model.WidgetLayout
import com.mushinlab.launcher.core.model.WidgetPanelPosition
import com.mushinlab.launcher.core.model.WidgetPanelSize
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
                out.writeUTF(layout.clockPanel?.name.orEmpty())
                out.writeUTF(layout.topSize.name)
                out.writeUTF(layout.bottomSize.name)
                out.writeInt(layout.widgets.size)
                layout.widgets.forEach {
                    out.writeInt(it.id)
                    out.writeUTF(it.panel.name)
                    out.writeUTF(it.label)
                }
            }
            out.writeBoolean(state.pending != null)
            state.pending?.let {
                out.writeLong(it.spaceId)
                out.writeUTF(it.panel.name)
                out.writeInt(it.id)
            }
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray())
    }

    fun decode(raw: String?): WidgetState = runCatching {
        if (raw == null) return WidgetState()
        DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(raw))).use { input ->
            require(input.readInt() == VERSION)
            val layouts = buildMap {
                repeat(input.count()) {
                    val id = input.readLong()
                    val clock = input.readUTF().takeIf { it.isNotEmpty() }?.let(WidgetPanelPosition::valueOf)
                    val top = WidgetPanelSize.valueOf(input.readUTF())
                    val bottom = WidgetPanelSize.valueOf(input.readUTF())
                    val widgets = List(input.count()) {
                        HostedWidget(input.readInt().also { require(it > 0) }, input.panel(), input.readUTF())
                    }
                    put(id, WidgetLayout(clock, top, bottom, widgets))
                }
            }
            val pending = if (input.readBoolean()) {
                PendingWidget(input.readLong(), input.panel(), input.readInt().also { require(it > 0) })
            } else {
                null
            }
            WidgetState(layouts, pending)
        }
    }.getOrDefault(WidgetState())

    private fun DataInputStream.panel(): WidgetPanelPosition = WidgetPanelPosition.valueOf(readUTF())

    private fun DataInputStream.count(): Int = readInt().also { require(it in 0..MAX_ENTRIES) }

    private const val VERSION = 1
    private const val MAX_ENTRIES = 512
}
