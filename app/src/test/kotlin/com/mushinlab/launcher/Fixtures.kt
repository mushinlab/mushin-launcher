package com.mushinlab.launcher

import com.mushinlab.launcher.core.apps.LaunchResult
import com.mushinlab.launcher.core.model.AppEntry
import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.AppOverride
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.LauncherSettings
import com.mushinlab.launcher.core.model.LearnedGesture
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.Palettes
import com.mushinlab.launcher.core.model.ProfileType
import com.mushinlab.launcher.core.model.Space
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.model.SwipeDirection
import com.mushinlab.launcher.core.search.GestureSample

fun key(name: String) = AppKey(name, name.replaceFirstChar(Char::uppercase), 0)

fun entry(key: AppKey, system: Boolean = false) = AppEntry(key, key.activityName, 0, 0, system, ProfileType.MAIN)

fun space(id: Long, type: SpaceType = SpaceType.NORMAL, name: String = "s$id") =
    Space(id, name, type, id.toInt(), PalettePreset.GRAPHITE, Palettes.Graphite, 0)

fun override(key: AppKey, label: String? = null, hidden: Boolean = false) =
    AppOverride(key, label, hidden, null, 0)

class FakeLauncherModel(
    var apps: Map<AppKey, AppEntry> = emptyMap(),
    var overrides: Map<AppKey, AppOverride> = emptyMap(),
    override var currentSpace: Space? = space(1),
    var items: MutableList<AppKey> = mutableListOf(),
    var capacity: Int = Int.MAX_VALUE,
    var result: LaunchResult = LaunchResult.Started,
    var shadeAvailable: Boolean = true,
    override var settings: LauncherSettings = LauncherSettings(),
    override var gestureBindings: Map<SwipeDirection, GestureAction> = emptyMap(),
    override var learnedGestures: List<LearnedGesture> = emptyList(),
) : LauncherModel {
    val launched = mutableListOf<Pair<AppKey, Boolean>>()
    val hidden = mutableMapOf<AppKey, Boolean>()
    var selectedLayout: String? = null
    var shadeRequests = 0

    override fun entry(key: AppKey): AppEntry? = apps[key]

    override fun override(key: AppKey): AppOverride? = overrides[key]

    override fun isInCurrentSpace(key: AppKey): Boolean = key in items

    override fun launch(
        key: AppKey,
        fromSearch: Boolean,
        animate: Boolean,
        gestures: List<GestureSample>,
    ): LaunchResult {
        launched += key to fromSearch
        return result
    }

    override fun expandShade(): Boolean {
        shadeRequests++
        return shadeAvailable
    }

    override fun addToCurrentSpace(key: AppKey): Boolean {
        if (items.size >= capacity) return false
        items += key
        return true
    }

    override fun removeFromCurrentSpace(key: AppKey) {
        items -= key
    }

    override fun setHidden(key: AppKey, hidden: Boolean) {
        this.hidden[key] = hidden
    }

    override fun setActiveLayout(id: String) {
        selectedLayout = id
    }
}

object NoTrace : SearchTrace {
    override fun begin() = Unit

    override fun end() = Unit
}
