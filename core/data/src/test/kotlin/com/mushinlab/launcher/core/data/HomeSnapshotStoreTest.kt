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
}
