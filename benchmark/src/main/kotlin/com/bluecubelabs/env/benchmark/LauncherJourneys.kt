package com.bluecubelabs.env.benchmark

import android.content.ComponentName
import android.content.Intent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

const val TARGET_PACKAGE = "com.bluecubelabs.env"
private const val WAIT_MS = 3_000L

fun MacrobenchmarkScope.startLauncher() {
    startActivityAndWait(
        Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .setComponent(ComponentName(TARGET_PACKAGE, "$TARGET_PACKAGE.MainActivity")),
    )
}

fun MacrobenchmarkScope.waitForHome() {
    device.wait(Until.hasObject(By.res("home")), WAIT_MS)
}

fun MacrobenchmarkScope.doubleTapBackground() {
    val x = device.displayWidth / 2
    val y = device.displayHeight * 85 / 100
    device.click(x, y)
    device.click(x, y)
    device.wait(Until.hasObject(By.res("keyboard")), WAIT_MS)
}

fun MacrobenchmarkScope.typeKeys(text: String) {
    text.forEach { c ->
        device.findObject(By.res("key_$c"))?.click()
    }
    device.waitForIdle()
}

fun MacrobenchmarkScope.longPressBackground() {
    val x = device.displayWidth / 2
    val y = device.displayHeight * 85 / 100
    device.swipe(x, y, x, y, 120)
    device.wait(Until.hasObject(By.res("popup")), WAIT_MS)
}

fun MacrobenchmarkScope.closeOverlay() {
    device.pressBack()
    device.waitForIdle()
}
