package com.mushinlab.launcher.core.apps

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.apps.icons.IconKey
import com.mushinlab.launcher.core.apps.icons.IconRenderer
import com.mushinlab.launcher.core.apps.icons.IconSource
import com.mushinlab.launcher.core.apps.icons.LoadedIcon
import com.mushinlab.launcher.core.model.AppKey
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IconRendererTest {
    private val key = IconKey(AppKey("app", "Main", 0), lastUpdateTime = 1, sizePx = 48)

    private fun renderer(drawable: Drawable?) = IconRenderer(IconSource { _, _ -> drawable }, densityDpi = 420)

    @Test
    fun missingIconBecomesALetter() {
        assertThat(renderer(null).mask(key)).isEqualTo(LoadedIcon.Letter)
        assertThat(renderer(null).full(key)).isEqualTo(LoadedIcon.Letter)
    }

    @Test
    fun aSmallGlyphBecomesAMask() {
        val dot = ShapeDrawable(OvalShape()).apply { paint.color = Color.WHITE }
        val padded = android.graphics.drawable.InsetDrawable(dot, 0.3f)

        val icon = renderer(padded).mask(key)

        assertThat(icon).isInstanceOf(LoadedIcon.Mask::class.java)
        assertThat((icon as LoadedIcon.Mask).bitmap.width).isEqualTo(48)
    }

    @Test
    fun aFullBleedIconIsTooHeavyForAMaskAndFallsBackToALetter() {
        assertThat(renderer(ColorDrawable(Color.WHITE)).mask(key)).isEqualTo(LoadedIcon.Letter)
    }

    @Test
    fun colouredIconKeepsItsSize() {
        val icon = renderer(ColorDrawable(Color.RED)).full(key)

        assertThat((icon as LoadedIcon.Full).bitmap.getPixel(10, 10)).isEqualTo(Color.RED)
    }
}
