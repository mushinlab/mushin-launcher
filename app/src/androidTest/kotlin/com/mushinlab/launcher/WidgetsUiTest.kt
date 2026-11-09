package com.mushinlab.launcher

import android.appwidget.AppWidgetManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.model.HostedWidget
import com.mushinlab.launcher.core.model.WidgetCell
import com.mushinlab.launcher.core.model.WidgetLayout
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
        rule.onNodeWithTag("add_widget_1_0").assertDoesNotExist()
        screenshot("default")
        rule.onNodeWithTag("home_edit").performClick()
        WidgetCell.singles.drop(1).forEach {
            rule.onNodeWithTag("add_widget_${it.column}_${it.row}").assertIsDisplayed()
        }
        rule.onNodeWithTag("tile_remove_${HostedWidget.CLOCK_ID}").performClick()
        rule.waitUntil(5_000) { layout().widgets.isEmpty() }
        rule.onNodeWithTag("clock_block").assertDoesNotExist()
        rule.onNodeWithTag("add_widget_0_0").assertIsDisplayed().performClick()
        assertThat(rule.activity.launcher.widgets.choices?.first()).isEqualTo(WidgetChoice.Clock)
        rule.onNodeWithTag("widget_choice_0").performClick()
        rule.waitUntil(5_000) { layout().widgets == listOf(HostedWidget.clock) }
        rule.onNodeWithTag("clock_sheet").assertIsDisplayed()
    }

    @Test
    fun AC_39_2_threeWidgetsLeaveTheFourthCellForAdding() {
        seedWidgets(3)
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("add_widget_1_1").assertIsDisplayed()
        screenshot("expanded-three")
    }

    @Test
    fun AC_39_3_pickerFiltersSmallWidgetsAndCancellationAllocatesNothing() {
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("add_widget_1_0").performClick()
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
        rule.onNodeWithTag("add_widget_1_0").performClick()
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
    fun AC_39_2_dragSwapsCellsAndAnEdgeStretchesOverTwoRows() {
        val widgets = seedWidgets(2)
        rule.onNodeWithTag("home_edit").performClick()
        val first = rule.onNodeWithTag("widget_${widgets[0].id}")
        val second = rule.onNodeWithTag("widget_${widgets[1].id}")
        val a = first.fetchSemanticsNode().boundsInRoot
        val b = second.fetchSemanticsNode().boundsInRoot
        assertThat(a.top).isEqualTo(b.top)
        val timeout = android.view.ViewConfiguration.getLongPressTimeout().toLong()
        first.performTouchInput {
            down(Offset(width * 0.3f, height * 0.7f))
            advanceEventTime(timeout + 100)
            moveBy(b.center - a.center, 300)
            up()
        }
        rule.waitUntil(5_000) { cell(widgets[0].id) == WidgetCell(1, 0) && cell(widgets[1].id) == WidgetCell(0, 0) }
        rule.onNodeWithTag("tile_resize_bottom_${widgets[0].id}").performTouchInput {
            down(center)
            moveBy(Offset(0f, a.height + 40f), 300)
            up()
        }
        rule.waitUntil(5_000) { cell(widgets[0].id) == WidgetCell(1, 0, 1, 2) }
        val tall = first.fetchSemanticsNode().boundsInRoot
        assertThat(tall.height).isGreaterThan(a.height * 2)
        rule.onNodeWithTag("add_widget_0_1").assertIsDisplayed()
        screenshot("column-and-tall")
        rule.activityRule.scenario.recreate()
        rule.waitUntil(5_000) { cell(widgets[0].id) == WidgetCell(1, 0, 1, 2) }
        assertThat(cell(widgets[1].id)).isEqualTo(WidgetCell(0, 0))
    }

    @Test
    fun AC_39_2_fullGridHasNoAddCellsAndCannotOverflow() {
        val widgets = seedWidgets(4)
        rule.onNodeWithTag("home_edit").performClick()
        val cells = widgets.map {
            rule.onNodeWithTag("widget_${it.id}").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        }
        assertThat(cells[0].top).isEqualTo(cells[1].top)
        assertThat(cells[2].top).isEqualTo(cells[3].top)
        assertThat(cells[2].top).isGreaterThan(cells[0].bottom)
        WidgetCell.singles.forEach { rule.onNodeWithTag("add_widget_${it.column}_${it.row}").assertDoesNotExist() }
        screenshot("expanded-edit")
        rule.runOnIdle { rule.activity.launcher.widgets.add() }
        rule.waitForIdle()
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
        rule.onNodeWithTag("add_widget_1_0").performClick()
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
            HostedWidget(id, info.loadLabel(rule.activity.packageManager).toString(), WidgetCell.singles[index])
        }
        runBlocking {
            container.widgets.update {
                WidgetState(
                    mapOf(
                        normalId to WidgetLayout(widgets),
                    ),
                )
            }
        }
        rule.waitUntil(5_000) { rule.activity.launcher.home.value.widgetPanels.top.widgets.size == count }
        return widgets
    }

    private fun cell(id: Int): WidgetCell? = layout().widgets.firstOrNull { it.id == id }?.cell

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
