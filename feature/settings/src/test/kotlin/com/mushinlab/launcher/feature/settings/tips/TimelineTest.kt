package com.mushinlab.launcher.feature.settings.tips

import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TimelineTest {
    @Test
    fun phaseIsClampedOutsideItsWindow() {
        assertThat(phase(50f, 100, 200)).isEqualTo(0f)
        assertThat(phase(150f, 100, 200)).isEqualTo(0.5f)
        assertThat(phase(250f, 100, 200)).isEqualTo(1f)
        assertThat(ease(100f, 100, 200)).isEqualTo(0f)
        assertThat(ease(200f, 100, 200)).isEqualTo(1f)
    }

    @Test
    fun trackHoldsItsEndsAndPassesThroughKeys() {
        val track = Track(listOf(Key(100, Offset(0f, 0f)), Key(200, Offset(10f, 20f)), Key(300, Offset(10f, 20f))))

        assertThat(track.at(0f)).isEqualTo(Offset(0f, 0f))
        assertThat(track.at(200f)).isEqualTo(Offset(10f, 20f))
        assertThat(track.at(250f)).isEqualTo(Offset(10f, 20f))
        assertThat(track.at(1000f)).isEqualTo(Offset(10f, 20f))
        assertThat(track.at(150f).x).isGreaterThan(0f)
        assertThat(track.at(150f).x).isLessThan(10f)
    }

    @Test
    fun tapPulseRisesHoldsAndFallsBack() {
        assertThat(pulse(90f, 100)).isEqualTo(0f)
        assertThat(pulse(250f, 100)).isEqualTo(1f)
        assertThat(pulse(1000f, 100)).isEqualTo(0f)
        assertThat(pulse(1250f, 100, 1100)).isEqualTo(1f)
    }

    @Test
    fun loopFadesInAndOut() {
        assertThat(loopFade(0f, 4000)).isEqualTo(0f)
        assertThat(loopFade(2000f, 4000)).isEqualTo(1f)
        assertThat(loopFade(4000f, 4000)).isEqualTo(0f)
    }

    @Test
    fun polylineWalksByLength() {
        val line = Polyline(listOf(Offset(0f, 0f), Offset(10f, 0f), Offset(10f, 30f)))

        assertThat(line.pointAt(0.5f).y).isWithin(TOLERANCE).of(10f)
        assertThat(line.pointAt(1f)).isEqualTo(Offset(10f, 30f))
        assertThat(line.reached(0.5f)).hasSize(3)
        assertThat(line.reached(0.5f)[1]).isEqualTo(Offset(10f, 0f))
        assertThat(line.moved(Offset(100f, 100f)).first.x).isWithin(TOLERANCE).of(100f - 20f / 3f)
        assertThat(line.moved(Offset(100f, 100f)).first.y).isWithin(TOLERANCE).of(90f)
    }

    @Test
    fun sheetRisesFollowsTheDragAndHidesWhenClosed() {
        val top = Scene.HEIGHT - 100f

        assertThat(sheetOffset(top, open = 0f, dragged = 0f, closed = 0f)).isEqualTo(100f)
        assertThat(sheetOffset(top, open = 1f, dragged = 0f, closed = 0f)).isEqualTo(0f)
        assertThat(sheetOffset(top, open = 1f, dragged = 30f, closed = 0f)).isEqualTo(30f)
        assertThat(sheetOffset(top, open = 1f, dragged = 30f, closed = 0.5f)).isWithin(TOLERANCE).of(65f)
        assertThat(sheetOffset(top, open = 1f, dragged = 30f, closed = 1f)).isEqualTo(100f)
    }

    private companion object {
        const val TOLERANCE = 0.001f
    }
}
