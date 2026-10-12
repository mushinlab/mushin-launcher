package com.bluecubelabs.env

import android.app.StatusBarManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.provider.Settings

internal fun MainActivity.openAssistantSettings() {
    try {
        startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
    }
}

internal fun MainActivity.requestSearchTile() {
    val manager = getSystemService(StatusBarManager::class.java) ?: return
    manager.requestAddTileService(
        ComponentName(this, SearchTileService::class.java),
        getString(R.string.tile_search),
        Icon.createWithResource(this, R.drawable.ic_tile_search),
        mainExecutor,
    ) { result ->
        if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
            notice = Notice(getString(R.string.notice_tile_added))
        }
    }
}
