package com.mushinlab.launcher

import android.appwidget.AppWidgetManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.HostedWidget
import com.mushinlab.launcher.core.model.WidgetArrangement
import com.mushinlab.launcher.core.model.WidgetLayout
import com.mushinlab.launcher.core.model.WidgetPanelMode
import com.mushinlab.launcher.core.model.WidgetState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class WidgetsUiTest : LauncherUiTestBase() {
    @Before
    fun resetWidgets() = runBlocking {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "appwidget grantbind --package com.mushinlab.launcher --user 0",
        ).use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
        container.widgets.update { WidgetState() }
    }

    @After
    fun clearWidgets() = runBlocking {
        if (InstrumentationRegistry.getArguments().getString("keepWidgets") == "true") return@runBlocking
        container.widgetHost.appWidgetIds.forEach(container.widgetHost::deleteAppWidgetId)
        container.widgets.update { WidgetState() }
    }

    @Test
    fun AC_39_1_clockIsARemovableSlotAddedBackFromThePicker() {
        rule.onNodeWithTag("widget_${HostedWidget.CLOCK_ID}").assertIsDisplayed()
        rule.onNodeWithTag("add_widget_TOP").assertDoesNotExist()
        screenshot("default")
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("add_widget_TOP").assertIsDisplayed()
        rule.onNodeWithTag("tile_remove_${HostedWidget.CLOCK_ID}").performClick()
        rule.waitUntil(5_000) { layout().widgets.isEmpty() }
        rule.onNodeWithTag("clock_block").assertDoesNotExist()
        rule.onNodeWithTag("add_widget_TOP").assertIsDisplayed().performClick()
        assertThat(rule.activity.launcher.widgets.choices?.first()).isEqualTo(WidgetChoice.Clock)
        rule.onNodeWithTag("widget_choice_0").performClick()
        rule.waitUntil(5_000) { layout().widgets == listOf(HostedWidget.clock) }
        rule.onNodeWithTag("clock_sheet").assertIsDisplayed()
    }

    @Test
    fun AC_39_2_threeWidgetsInTheExpandedPanelLeaveTheFourthCellForAdding() {
        seedWidgets(3)
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("add_widget_TOP").assertIsDisplayed()
        screenshot("expanded-three")
    }

    @Test
    fun AC_39_3_pickerFiltersSmallWidgetsAndCancellationAllocatesNothing() {
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("add_widget_TOP").performClick()
        rule.onNodeWithTag("widget_picker").assertIsDisplayed()
        val choices = rule.activity.launcher.widgets.choices.orEmpty()
        assertThat(choices).isNotEmpty()
        assertThat(choices).doesNotContain(WidgetChoice.Clock)
        assertThat(choices.all { it is WidgetChoice.Hosted && fitsCompactWidget(rule.activity, it.info) }).isTrue()
        assertThat(choices.size).isLessThan(AppWidgetManager.getInstance(rule.activity).installedProviders.size)
        screenshot("picker")
        androidx.test.espresso.Espresso.pressBack()
        rule.waitForIdle()
        rule.onNodeWithTag("widget_picker").assertDoesNotExist()
        assertThat(rule.activity.launcher.navigator.homeEditing).isTrue()
        assertThat(container.widgets.loaded.value?.pending).isNull()
        assertThat(container.widgetHost.appWidgetIds).isEmpty()
    }

    @Test
    fun AC_39_3_addRemoveAndRecreateRealWidget() {
        val index = compactProviders(rule.activity).indexOfFirst { it.configure == null }
        assertThat(index).isAtLeast(0)
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("add_widget_TOP").performClick()
        rule.onNodeWithTag("widget_choice_$index").performScrollTo().performClick()
        rule.waitUntil(10_000) { layout().widgets.any { !it.isClock } }
        val widget = layout().widgets.first { !it.isClock }
        rule.onNodeWithTag("widget_${widget.id}").assertIsDisplayed()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("widget_${widget.id}").assertIsDisplayed()
        rule.onNodeWithTag("home_widgets").performClick()
        rule.onNodeWithTag("widget_remove_${widget.id}").performScrollTo().performClick()
        rule.waitUntil(5_000) {
            layout().widgets == listOf(HostedWidget.clock) && container.widgetHost.appWidgetIds.isEmpty()
        }
    }

    @Test
    fun AC_39_2_standardRowColumnAndDragPersist() {
        val widgets = seedWidgets(2)
        rule.onNodeWithTag("home_edit").performClick()
        val first = rule.onNodeWithTag("widget_${widgets[0].id}")
        val second = rule.onNodeWithTag("widget_${widgets[1].id}")
        val a = first.fetchSemanticsNode().boundsInRoot
        val b = second.fetchSemanticsNode().boundsInRoot
        assertThat(a.top).isEqualTo(b.top)
        screenshot("standard-row")
        val timeout = android.view.ViewConfiguration.getLongPressTimeout().toLong()
        first.performTouchInput {
            down(center)
            advanceEventTime(timeout + 100)
            moveBy(b.center - a.center, 300)
            up()
        }
        rule.waitUntil(5_000) { layout().widgets.first().id == widgets[1].id }
        rule.runOnIdle {
            rule.activity.launcher.widgets.panels.resize(WidgetPanelMode.STANDARD, WidgetArrangement.COLUMN)
        }
        rule.waitUntil(5_000) {
            rule.activity.launcher.home.value.widgetPanels.top.arrangement ==
                WidgetArrangement.COLUMN
        }
        val upper = second.fetchSemanticsNode().boundsInRoot
        val lower = first.fetchSemanticsNode().boundsInRoot
        assertThat(upper.left).isEqualTo(lower.left)
        assertThat(lower.top).isGreaterThan(upper.bottom)
        screenshot("standard-column")
        rule.activityRule.scenario.recreate()
        rule.waitUntil(5_000) { layout().arrangement == WidgetArrangement.COLUMN }
        assertThat(layout().widgets.first().id).isEqualTo(widgets[1].id)
    }

    @Test
    fun AC_39_2_expandedGridHasFourCellsAndCannotOverflow() {
        val widgets = seedWidgets(4)
        rule.onNodeWithTag("home_edit").performClick()
        val cells = widgets.map {
            rule.onNodeWithTag("widget_${it.id}").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        }
        assertThat(cells[0].top).isEqualTo(cells[1].top)
        assertThat(cells[2].top).isEqualTo(cells[3].top)
        assertThat(cells[2].top).isGreaterThan(cells[0].bottom)
        rule.onNodeWithTag("add_widget_TOP").assertDoesNotExist()
        screenshot("expanded-edit")
        rule.runOnIdle {
            rule.activity.launcher.widgets.panels.resize(WidgetPanelMode.STANDARD, WidgetArrangement.ROW)
            rule.activity.launcher.widgets.add()
        }
        rule.waitForIdle()
        assertThat(layout().mode).isEqualTo(WidgetPanelMode.EXPANDED)
        assertThat(rule.activity.launcher.widgets.choices).isNull()
        rule.onNodeWithTag("home_widgets").performClick()
        screenshot("panel-settings")
        androidx.test.espresso.Espresso.pressBack()
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("row_2").assertIsDisplayed()
        screenshot("expanded-home")
        val row = rule.onNodeWithTag("row_0").fetchSemanticsNode().boundsInRoot
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val pixels = IntArray(row.width.toInt() * row.height.toInt())
        bitmap.getPixels(
            pixels,
            0,
            row.width.toInt(),
            row.left.toInt(),
            row.top.toInt(),
            row.width.toInt(),
            row.height.toInt(),
        )
        assertThat(pixels.toSet().size).isGreaterThan(2)
    }

    @Test
    fun AC_39_3_cancelProviderConfigurationReleasesId() {
        val providers = compactProviders(rule.activity)
        val index = providers.indexOfFirst { it.configure != null }
        assertThat(index).isAtLeast(0)
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("add_widget_TOP").performClick()
        rule.onNodeWithTag("widget_choice_$index").performScrollTo().performClick()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        rule.waitUntil(10_000) {
            automation.rootInActiveWindow?.packageName?.toString() == providers[index].configure.packageName
        }
        automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        rule.waitUntil(5_000) { container.widgets.loaded.value?.pending == null }
        assertThat(layout().widgets).containsExactly(HostedWidget.clock)
        assertThat(container.widgetHost.appWidgetIds).isEmpty()
    }

    private fun seedWidgets(count: Int): List<HostedWidget> {
        val manager = AppWidgetManager.getInstance(rule.activity)
        val providers = compactProviders(rule.activity).filter { it.configure == null }
        assertThat(providers).isNotEmpty()
        val widgets = (0 until count).map { index ->
            val info = providers[index % providers.size]
            val id = container.widgetHost.allocateAppWidgetId()
            assertThat(manager.bindAppWidgetIdIfAllowed(id, info.provider)).isTrue()
            HostedWidget(id, info.loadLabel(rule.activity.packageManager).toString())
        }
        runBlocking {
            container.widgets.update {
                WidgetState(
                    mapOf(
                        normalId to WidgetLayout(
                            mode = if (count > 2) WidgetPanelMode.EXPANDED else WidgetPanelMode.STANDARD,
                            widgets = widgets,
                        ),
                    ),
                )
            }
        }
        rule.waitUntil(5_000) { rule.activity.launcher.home.value.widgetPanels.top.widgets.size == count }
        return widgets
    }

    private fun layout(): WidgetLayout = container.widgets.loaded.value?.layouts?.get(normalId) ?: WidgetLayout()

    private fun screenshot(name: String) {
        rule.mainClock.advanceTimeBy(400)
        rule.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.waitForIdle(300, 3_000)
        val file = java.io.File(rule.activity.getExternalFilesDir(null), "widgets-$name.png")
        file.outputStream().use {
            automation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
