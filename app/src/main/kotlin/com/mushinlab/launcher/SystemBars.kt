package com.mushinlab.launcher

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import com.mushinlab.launcher.core.model.Palette

class SystemBars(private val activity: ComponentActivity) {
    private var dark: Boolean? = null

    fun apply(palette: Palette) {
        if (dark == palette.isDark) return
        dark = palette.isDark
        val style = if (palette.isDark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
