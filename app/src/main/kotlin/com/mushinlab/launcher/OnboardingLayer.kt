package com.mushinlab.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mushinlab.launcher.feature.settings.tips.Onboarding
import com.mushinlab.launcher.feature.settings.tips.rememberOnboardingPager

@Composable
internal fun OnboardingLayer(vm: LauncherViewModel) {
    val done by vm.onboardingDone.collectAsStateWithLifecycle()
    var closed by rememberSaveable { mutableStateOf(false) }
    if (done != false || closed) return
    val pager = rememberOnboardingPager()
    val finish = {
        closed = true
        vm.preferences.update { it.copy(onboardingDone = true) }
    }
    BackHandler { if (!pager.previous()) finish() }
    Box(Modifier.zIndex(6f).fillMaxSize()) {
        Onboarding(onFinish = finish, pager = pager)
    }
}
