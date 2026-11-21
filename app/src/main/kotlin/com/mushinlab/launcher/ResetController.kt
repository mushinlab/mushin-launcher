package com.mushinlab.launcher

import android.content.res.Resources
import com.mushinlab.launcher.core.data.R as CoreR
import com.mushinlab.launcher.core.data.db.MushinDatabase
import com.mushinlab.launcher.core.model.PalettePreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ResetController(
    private val container: AppContainer,
    private val scope: CoroutineScope,
    private val resources: Resources,
    private val privateSpace: PrivateSpaceController,
    private val navigator: LauncherNavigator,
) {
    fun run() {
        val seed = MushinDatabase.SeedNames(
            resources.getString(CoreR.string.space_default_normal),
            resources.getString(CoreR.string.space_default_work),
        )
        scope.launch {
            privateSpace.cancel()
            privateSpace.stop()
            container.resetAll(seed)
            container.privateSpaces.ensure(
                resources.getString(CoreR.string.space_default_private),
                PalettePreset.NIGHT,
            )
            navigator.closeOverlays()
        }
    }
}
