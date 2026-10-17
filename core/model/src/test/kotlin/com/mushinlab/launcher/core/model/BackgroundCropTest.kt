package com.mushinlab.launcher.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackgroundCropTest {
    private val wide = CropFrame(sourceWidth = 4000, sourceHeight = 3000, frameWidth = 1080f, frameHeight = 2400f)

    @Test
    fun defaultCropShowsTheMiddleOfTheImageAtCoverScale() {
        val rect = wide.region(BackgroundCrop())

        assertThat(rect.height).isWithin(0.01f).of(3000f)
        assertThat(rect.width).isWithin(0.01f).of(1350f)
        assertThat(rect.left).isWithin(0.01f).of(1325f)
        assertThat(rect.top).isWithin(0.01f).of(0f)
    }

    @Test
    fun tallImageInAWideFrameCoversByWidth() {
        val frame = CropFrame(sourceWidth = 1000, sourceHeight = 4000, frameWidth = 1000f, frameHeight = 2000f)

        val rect = frame.region(BackgroundCrop())

        assertThat(rect.width).isWithin(0.01f).of(1000f)
        assertThat(rect.height).isWithin(0.01f).of(2000f)
        assertThat(rect.top).isWithin(0.01f).of(1000f)
    }

    @Test
    fun zoomNeverGoesBelowCoverOrAboveTheMaximum() {
        val shrunk = wide.zoom(BackgroundCrop(), 540f, 1200f, factor = 0.25f)
        val grown = wide.zoom(BackgroundCrop(), 540f, 1200f, factor = 100f)

        assertThat(shrunk.zoom).isEqualTo(1f)
        assertThat(wide.region(shrunk).height).isWithin(0.01f).of(3000f)
        assertThat(grown.zoom).isEqualTo(BackgroundCrop.MAX_ZOOM)
        assertThat(wide.region(grown).height).isWithin(0.01f).of(1000f)
    }

    @Test
    fun panStopsAtTheImageEdges() {
        val dragged = wide.pan(BackgroundCrop(), dx = 100_000f, dy = 100_000f)

        val rect = wide.region(dragged)

        assertThat(rect.left).isWithin(0.01f).of(0f)
        assertThat(rect.top).isWithin(0.01f).of(0f)
        assertThat(wide.region(wide.pan(BackgroundCrop(), dx = -100_000f, dy = 0f)).left)
            .isWithin(0.01f).of(4000f - 1350f)
    }

    @Test
    fun panMovesTheImageWithTheFinger() {
        val scale = wide.scale(BackgroundCrop())

        val rect = wide.region(wide.pan(BackgroundCrop(), dx = 80f, dy = 0f))

        assertThat(rect.left).isWithin(0.01f).of(1325f - 80f / scale)
    }

    @Test
    fun zoomKeepsTheImagePointUnderTheFingers() {
        val crop = BackgroundCrop()
        val before = wide.region(crop)
        val scale = wide.scale(crop)
        val pointX = before.left + 300f / scale
        val pointY = before.top + 900f / scale

        val zoomed = wide.zoom(crop, focusX = 300f, focusY = 900f, factor = 2f)
        val after = wide.region(zoomed)
        val nextScale = wide.scale(zoomed)

        assertThat(zoomed.zoom).isEqualTo(2f)
        assertThat(after.left + 300f / nextScale).isWithin(0.5f).of(pointX)
        assertThat(after.top + 900f / nextScale).isWithin(0.5f).of(pointY)
    }

    @Test
    fun clampPullsAnOutOfRangeCropBackInside() {
        val clamped = wide.clamp(BackgroundCrop(zoom = 0f, centerX = -3f, centerY = 9f))

        assertThat(clamped.zoom).isEqualTo(1f)
        val rect = wide.region(clamped)
        assertThat(rect.left).isWithin(0.01f).of(0f)
        assertThat(rect.top + rect.height).isWithin(0.01f).of(3000f)
    }

    @Test
    fun backgroundLookIsClampedAndNanIsNeutral() {
        assertThat(SpaceBackground.brightness(-4f)).isEqualTo(-1f)
        assertThat(SpaceBackground.brightness(Float.NaN)).isEqualTo(0f)
        assertThat(SpaceBackground.blur(2f)).isEqualTo(1f)
        assertThat(SpaceBackground.blur(-0.3f)).isEqualTo(0f)
    }
}
