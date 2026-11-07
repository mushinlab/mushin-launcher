package com.mushinlab.launcher

import android.appwidget.AppWidgetManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.data.repo.SpaceOutcome
import com.mushinlab.launcher.core.model.FolderKeys
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.model.WidgetLayout
import com.mushinlab.launcher.core.model.WidgetPanelPosition
import com.mushinlab.launcher.core.model.WidgetPanelSize
import com.mushinlab.launcher.core.model.WidgetState
import com.mushinlab.launcher.feature.settings.R as SettingsR
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class WidgetsUiTest : LauncherUiTestBase() {
    @Before
    fun resetWidgets() = runBlocking {
        container.widgets.update { WidgetState() }
        container.settings.update { it.copy(showClock = true) }
    }

    @After
    fun clearWidgets() = runBlocking {
        container.widgetHost.appWidgetIds.forEach(container.widgetHost::deleteAppWidgetId)
        container.widgets.update { WidgetState() }
    }

    @Test
    fun AC_38_1_emptyPanelsCollapseAndAddingOnlyAppearsWhileEditing() {
        runBlocking {
            container.widgets.update { WidgetState(mapOf(normalId to WidgetLayout(clockPanel = null))) }
        }
        rule.waitUntil(5_000) { !rule.activity.launcher.home.value.widgetPanels.top.clock }
        rule.onNodeWithTag("widget_panel_TOP").assertDoesNotExist()
        rule.onNodeWithTag("widget_panel_BOTTOM").assertDoesNotExist()
        rule.onNodeWithTag("add_widget_TOP").assertDoesNotExist()
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("add_widget_TOP").assertIsDisplayed()
        rule.onNodeWithTag("add_widget_BOTTOM").assertIsDisplayed()
        rule.onNodeWithTag("home_widgets").performClick()
        rule.onNodeWithTag("widgets_sheet").assertIsDisplayed()
        rule.onNodeWithTag("widget_size_TOP_dropdown").assertIsDisplayed()
    }

    @Test
    fun AC_38_3_systemPickerAddsTimeToTheChosenPanel() {
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("add_widget_BOTTOM").performClick()
        chooseSystemWidget(rule.activity.getString(R.string.widget_time))
        rule.waitUntil(5_000) { container.widgets.loaded.value?.pending == null }
        com.google.common.truth.Truth.assertWithMessage("Widget state: ${container.widgets.loaded.value}")
            .that(
                container.widgets.loaded.value?.layouts?.get(normalId)?.clockPanel,
            ).isEqualTo(WidgetPanelPosition.BOTTOM)
        rule.onNodeWithTag("widget_panel_TOP").assertDoesNotExist()
        rule.onNodeWithTag("widget_panel_BOTTOM").assertIsDisplayed()
        assertThat(container.widgetHost.appWidgetIds).isEmpty()
    }

    @Test
    fun AC_38_4_cancellingSystemPickerReleasesTheAllocatedId() {
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("add_widget_TOP").performClick()
        rule.waitUntil(5_000) { container.widgets.loaded.value?.pending != null }
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        rule.waitUntil(5_000) { automation.rootInActiveWindow?.packageName?.toString() == "com.android.settings" }
        automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        rule.waitUntil(5_000) { container.widgets.loaded.value?.pending == null }
        assertThat(container.widgetHost.appWidgetIds).isEmpty()
    }

    @Test
    fun AC_38_3_systemPickerBindsAndDisplaysAnInstalledAppsWidget() {
        val manager = AppWidgetManager.getInstance(rule.activity)
        val provider = manager.installedProviders.first { it.configure == null }
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("add_widget_TOP").performClick()
        chooseSystemWidget(provider.loadLabel(rule.activity.packageManager))
        rule.waitUntil(5_000) { container.widgets.loaded.value?.layouts?.get(normalId)?.widgets?.isNotEmpty() == true }
        val widget = container.widgets.loaded.value?.layouts?.get(normalId)?.widgets?.first() ?: error("Missing widget")
        assertThat(manager.getAppWidgetInfo(widget.id).provider).isEqualTo(provider.provider)
        rule.onNodeWithTag("widget_${widget.id}").assertIsDisplayed()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("widget_${widget.id}").assertIsDisplayed()
        rule.onNodeWithTag("home_widgets").performClick()
        rule.onNodeWithTag("widget_remove_${widget.id}").performClick()
        rule.waitUntil(5_000) {
            container.widgets.loaded.value?.layouts?.get(normalId)?.widgets?.isEmpty() == true &&
                container.widgetHost.appWidgetIds.isEmpty()
        }
        assertThat(container.widgetHost.appWidgetIds).isEmpty()
    }

    @Test
    fun AC_38_2_panelHeightAndClockPositionBelongToTheCurrentSpace() {
        val work = container.spaces.spaces.value.first { it.type == SpaceType.WORK }
        runBlocking {
            container.spaces.setItems(work.id, spaceItems())
            container.widgets.update {
                WidgetState(
                    mapOf(
                        normalId to WidgetLayout(topSize = WidgetPanelSize.SMALL),
                        work.id to WidgetLayout(
                            clockPanel = WidgetPanelPosition.BOTTOM,
                            bottomSize = WidgetPanelSize.LARGE,
                        ),
                    ),
                )
            }
        }
        rule.waitUntil(5_000) { rule.activity.launcher.home.value.widgetPanels.top.size == WidgetPanelSize.SMALL }
        rule.onNodeWithTag("widget_panel_TOP").assertHeightIsEqualTo(120.dp)
        runBlocking { container.settings.update { it.copy(currentSpaceId = work.id) } }
        rule.waitUntil(5_000) { rule.activity.launcher.home.value.widgetPanels.bottom.clock }
        rule.onNodeWithTag("widget_panel_TOP").assertDoesNotExist()
        rule.onNodeWithTag("widget_panel_BOTTOM").assertIsDisplayed()
        runBlocking { container.settings.update { it.copy(currentSpaceId = normalId) } }
        rule.waitUntil(5_000) { rule.activity.launcher.home.value.widgetPanels.top.clock }
        rule.onNodeWithTag("widget_panel_TOP").assertHeightIsEqualTo(120.dp)
    }

    @Test
    fun AC_38_4_cancellingProviderConfigurationReleasesItsBoundId() {
        val manager = AppWidgetManager.getInstance(rule.activity)
        val provider = manager.installedProviders.first { it.configure != null }
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("add_widget_TOP").performClick()
        chooseSystemWidget(provider.loadLabel(rule.activity.packageManager))
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        rule.waitUntil(5_000) {
            val root = automation.rootInActiveWindow
            val pending = container.widgets.loaded.value?.pending
            pending != null && manager.getAppWidgetInfo(pending.id) != null &&
                root?.packageName?.toString() == provider.configure.packageName &&
                root.findAccessibilityNodeInfosByText("Choose widget").isEmpty()
        }
        automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        rule.waitUntil(5_000) { container.widgets.loaded.value?.pending == null }
        assertThat(container.widgets.loaded.value?.layouts?.get(normalId)?.widgets.orEmpty()).isEmpty()
        assertThat(container.widgetHost.appWidgetIds).isEmpty()
    }

    @Test
    fun AC_38_2_twoLargePanelsKeepEveryAppListRowReachable() {
        val apps = runBlocking { container.catalog.apps.first { it.size >= 7 } }.take(7)
        val folder = runBlocking {
            val id = (container.folders.create(normalId, 20) as SpaceOutcome.Ok).value
            container.folders.rename(id, "Scrollable folder")
            container.folders.setApps(id, apps.take(2).map { it.key })
            container.spaces.setItems(normalId, apps.map { it.key } + FolderKeys.of(id))
            id
        }
        val provider = AppWidgetManager.getInstance(rule.activity).installedProviders.first { it.configure == null }
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("add_widget_BOTTOM").performClick()
        chooseSystemWidget(provider.loadLabel(rule.activity.packageManager))
        rule.waitUntil(5_000) { container.widgets.loaded.value?.layouts?.get(normalId)?.widgets?.isNotEmpty() == true }
        rule.runOnIdle {
            rule.activity.launcher.widgets.resize(WidgetPanelPosition.TOP, WidgetPanelSize.LARGE)
            rule.activity.launcher.widgets.resize(WidgetPanelPosition.BOTTOM, WidgetPanelSize.LARGE)
        }
        rule.waitUntil(5_000) { rule.activity.launcher.home.value.widgetPanels.bottom.size == WidgetPanelSize.LARGE }
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("row_7").performScrollTo().performTouchInput { click() }
        rule.waitUntil(5_000) { (rule.activity.launcher.navigator.overlay as? Overlay.Folder)?.id == folder }
        rule.runOnIdle { rule.activity.launcher.navigator.handleBack() }
        rule.onNodeWithTag("home_edit").performClick()
        rule.onNodeWithTag("row_7").performScrollTo().performTouchInput { click() }
        rule.onNodeWithTag("edit_item_sheet").assertIsDisplayed()
    }

    private fun chooseSystemWidget(label: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        var node: AccessibilityNodeInfo? = null
        rule.waitUntil(5_000) {
            node = automation.rootInActiveWindow?.findAccessibilityNodeInfosByText(label)?.firstOrNull {
                it.text?.toString() == label && it.isVisibleToUser
            }
            if (node == null) {
                automation.rootInActiveWindow?.let(::scrollableNode)
                    ?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                automation.waitForIdle(100, 1_000)
            }
            node != null
        }
        automation.waitForIdle(200, 2_000)
        node = automation.rootInActiveWindow?.findAccessibilityNodeInfosByText(label)?.firstOrNull {
            it.text?.toString() == label && it.isVisibleToUser
        }
        val bounds = android.graphics.Rect()
        (node ?: error("Missing system widget row after scrolling")).getBoundsInScreen(bounds)
        val time = android.os.SystemClock.uptimeMillis()
        listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP).forEach { action ->
            val event = android.view.MotionEvent.obtain(
                time,
                time,
                action,
                bounds.exactCenterX(),
                bounds.exactCenterY(),
                0,
            )
            event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
            automation.injectInputEvent(event, true)
            event.recycle()
        }
    }

    private fun scrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        return (0 until node.childCount).firstNotNullOfOrNull { node.getChild(it)?.let(::scrollableNode) }
    }
}
