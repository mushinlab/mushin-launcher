package com.mushinlab.launcher.feature.settings.tips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Stable
class OnboardingPager(val count: Int, initial: Int = 0) {
    var page by mutableIntStateOf(initial.coerceIn(0, count - 1))
        private set

    val isLast: Boolean get() = page == count - 1

    fun next(): Boolean {
        if (isLast) return false
        page += 1
        return true
    }

    fun previous(): Boolean {
        if (page == 0) return false
        page -= 1
        return true
    }

    fun swipe(dragX: Float, threshold: Float): Boolean = when {
        dragX <= -threshold -> next()
        dragX >= threshold -> previous()
        else -> false
    }

    companion object {
        const val SLIDES = 4

        fun saver(count: Int): Saver<OnboardingPager, Int> = Saver(save = {
            it.page
        }, restore = { OnboardingPager(count, it) })
    }
}

@Composable
fun rememberOnboardingPager(): OnboardingPager =
    rememberSaveable(saver = OnboardingPager.saver(OnboardingPager.SLIDES)) { OnboardingPager(OnboardingPager.SLIDES) }
