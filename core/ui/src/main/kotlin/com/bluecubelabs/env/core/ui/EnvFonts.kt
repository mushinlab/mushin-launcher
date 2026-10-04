package com.bluecubelabs.env.core.ui

import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.bluecubelabs.env.core.model.AppFont

object EnvFonts {
    private val weights = listOf(300, 400, 500, 600, 700)

    private val inter by lazy { bundled(R.font.inter) }
    private val openSans by lazy { bundled(R.font.open_sans) }
    private val montserrat by lazy { bundled(R.font.montserrat) }
    private val condensed by lazy {
        FontFamily(weights.map { Font(DeviceFontFamilyName("sans-serif-condensed"), FontWeight(it)) })
    }

    fun family(font: AppFont): FontFamily = when (font) {
        AppFont.SYSTEM -> FontFamily.Default
        AppFont.INTER -> inter
        AppFont.OPEN_SANS -> openSans
        AppFont.MONTSERRAT -> montserrat
        AppFont.CONDENSED -> condensed
    }

    private fun bundled(res: Int) = FontFamily(
        weights.map { weight ->
            Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))
        },
    )
}
