package com.bluecubelabs.env.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
class LauncherBenchmarks {
    @get:Rule
    val rule = MacrobenchmarkRule()

    private val compilation = CompilationMode.Partial(BaselineProfileMode.Require)

    @Test
    fun coldStartup() = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilation,
        startupMode = StartupMode.COLD,
        iterations = ITERATIONS,
    ) {
        startLauncher()
    }

    @Test
    fun searchOpen() = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(TraceSectionMetric("search_open", TraceSectionMetric.Mode.Average), FrameTimingMetric()),
        compilationMode = compilation,
        iterations = ITERATIONS,
        setupBlock = {
            startLauncher()
            waitForHome()
        },
    ) {
        doubleTapBackground()
        closeOverlay()
    }

    @Test
    fun typingInSearch() = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = compilation,
        iterations = ITERATIONS,
        setupBlock = {
            startLauncher()
            waitForHome()
            doubleTapBackground()
        },
    ) {
        typeKeys("slackmapsx")
        closeOverlay()
    }

    private companion object {
        const val ITERATIONS = 10
    }
}
