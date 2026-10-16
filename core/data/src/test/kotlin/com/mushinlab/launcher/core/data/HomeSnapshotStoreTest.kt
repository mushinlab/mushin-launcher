package com.mushinlab.launcher.core.data

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.data.snapshot.HomeSnapshot
import com.mushinlab.launcher.core.data.snapshot.HomeSnapshotStore
import com.mushinlab.launcher.core.data.snapshot.SnapshotRow
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.ClockFormat
import com.mushinlab.launcher.core.model.ClockPosition
import com.mushinlab.launcher.core.model.HomeTextSize
import com.mushinlab.launcher.core.model.IconMode
import com.mushinlab.launcher.core.model.Palettes
import java.io.DataOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HomeSnapshotStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun roundTripsSnapshot() {
        val store = HomeSnapshotStore(tmp.root)
        assertThat(store.read()).isNull()
        val snapshot = HomeSnapshot(
            spaceId = 3,
            palette = Palettes.Paper,
            iconMode = IconMode.LEFT,
            textSize = HomeTextSize.L,
            clockFormat = ClockFormat.H12,
            showDate = false,
            rows = listOf(SnapshotRow(AppKey("com.slack", "Main", 10), "Слак", dimmed = true, work = true)),
            clockPosition = ClockPosition.END,
            listOffset = 64,
        )
        store.write(snapshot)
        assertThat(store.read()).isEqualTo(snapshot)
    }

    @Test
    fun corruptFileIsIgnored() {
        tmp.root.resolve("home_snapshot.bin").writeText("garbage")
        assertThat(HomeSnapshotStore(tmp.root).read()).isNull()
    }

    @Test
    fun automaticListOffsetRoundTripsAsNull() {
        val store = HomeSnapshotStore(tmp.root)
        val snapshot = HomeSnapshot(
            spaceId = 1,
            palette = Palettes.Paper,
            iconMode = IconMode.NONE,
            textSize = HomeTextSize.M,
            clockFormat = ClockFormat.SYSTEM,
            showDate = true,
            rows = emptyList(),
        )
        store.write(snapshot)
        assertThat(store.read()).isEqualTo(snapshot)
        assertThat(store.read()?.listOffset).isNull()
    }

    @Test
    fun fileFromThePreviousReleaseKeepsItsListOffset() {
        writeVersion3(listOffset = 0)
        assertThat(HomeSnapshotStore(tmp.root).read()?.listOffset).isEqualTo(0)
        writeVersion3(listOffset = 120)
        val snapshot = HomeSnapshotStore(tmp.root).read()
        assertThat(snapshot?.listOffset).isEqualTo(120)
        assertThat(snapshot?.rows?.single()?.label).isEqualTo("Maps")
    }

    @Test
    fun damagedListOffsetMeansAutomatic() {
        writeVersion3(listOffset = -7)
        assertThat(HomeSnapshotStore(tmp.root).read()?.listOffset).isNull()
        writeVersion3(listOffset = 100_000)
        assertThat(HomeSnapshotStore(tmp.root).read()?.listOffset).isNull()
    }

    private fun writeVersion3(listOffset: Int) {
        DataOutputStream(tmp.root.resolve("home_snapshot.bin").outputStream()).use { out ->
            out.writeInt(3)
            out.writeLong(5)
            repeat(11) { out.writeInt(0xFF000000.toInt()) }
            out.writeBoolean(true)
            out.writeUTF("NONE")
            out.writeUTF("M")
            out.writeUTF("SYSTEM")
            out.writeBoolean(true)
            out.writeUTF("START")
            out.writeInt(listOffset)
            out.writeInt(1)
            out.writeUTF("com.maps")
            out.writeUTF("Main")
            out.writeLong(0)
            out.writeUTF("Maps")
            out.writeBoolean(false)
            out.writeBoolean(false)
        }
    }
}
