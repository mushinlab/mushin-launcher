package com.mushinlab.launcher

import com.mushinlab.launcher.core.model.AppKey
import kotlinx.coroutines.launch

class AppEditor(private val container: AppContainer) {
    fun setHidden(key: AppKey, hidden: Boolean) {
        container.scope.launch { container.overrides.setHidden(key, hidden) }
    }

    fun setSystemShortcut(key: AppKey, bound: Boolean) {
        container.scope.launch { container.overrides.setSystemShortcut(key, bound) }
    }

    fun rename(key: AppKey, label: String?) {
        container.scope.launch { container.overrides.rename(key, label) }
    }

    fun openDetails(key: AppKey) {
        container.launcher.openDetails(key)
    }

    fun uninstall(key: AppKey) {
        container.launcher.uninstall(key)
    }
}
