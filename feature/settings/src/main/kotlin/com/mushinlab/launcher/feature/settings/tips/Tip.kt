package com.mushinlab.launcher.feature.settings.tips

import androidx.annotation.StringRes
import com.mushinlab.launcher.feature.settings.R

enum class Tip(val id: String, @param:StringRes val title: Int, @param:StringRes val description: Int) {
    SPACE("space", R.string.tip_space_title, R.string.tip_space_text),
    STYLE("style", R.string.tip_style_title, R.string.tip_style_text),
    MEMORY("memory", R.string.tip_memory_title, R.string.tip_memory_text),
    ;

    companion object {
        fun byId(id: String): Tip? = entries.firstOrNull { it.id == id }
    }
}
