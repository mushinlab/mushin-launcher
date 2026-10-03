package com.bluecubelabs.env

import android.app.Application

class EnvApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        container.icons.onTrimMemory(level)
    }
}
