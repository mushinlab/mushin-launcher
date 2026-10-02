package com.bluecubelabs.env.core.data.db

import com.bluecubelabs.env.core.model.AppKey
import com.bluecubelabs.env.core.model.AppOverride
import com.bluecubelabs.env.core.model.AppUsage
import com.bluecubelabs.env.core.model.EnvType
import com.bluecubelabs.env.core.model.Environment
import com.bluecubelabs.env.core.model.GestureAction
import com.bluecubelabs.env.core.model.GestureBinding
import com.bluecubelabs.env.core.model.Palette
import com.bluecubelabs.env.core.model.PalettePreset
import com.bluecubelabs.env.core.model.SearchRecent
import com.bluecubelabs.env.core.model.SwipeDirection

internal fun EnvironmentEntity.toModel(): Environment = Environment(
    id = id,
    name = name,
    type = EnvType.valueOf(type),
    sortOrder = sortOrder,
    preset = runCatching { PalettePreset.valueOf(palettePreset) }.getOrDefault(PalettePreset.CUSTOM),
    palette = Palette(
        background = background,
        zebraA = zebraA,
        zebraB = zebraB,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        accent = accent,
        iconTint = iconTint,
        divider = divider,
        surface = surface,
        keyBackground = keyBackground,
        keyPressed = keyPressed,
        isDark = isDark,
    ),
    createdAt = createdAt,
)

internal fun EnvironmentItemEntity.key(): AppKey = AppKey(packageName, activityName, userSerial)

internal fun AppOverrideEntity.toModel(): AppOverride = AppOverride(
    key = AppKey(packageName, activityName, userSerial),
    customLabel = customLabel,
    hidden = hidden,
    orphanedAt = orphanedAt,
    updatedAt = updatedAt,
)

internal fun AppUsageEntity.toModel(): AppUsage = AppUsage(
    key = AppKey(packageName, activityName, userSerial),
    launchCount = launchCount,
    lastLaunchedAt = lastLaunchedAt,
    decayedScore = decayedScore,
)

internal fun SearchRecentEntity.toModel(): SearchRecent = SearchRecent(
    key = AppKey(packageName, activityName, userSerial),
    lastLaunchedAt = lastLaunchedAt,
    launchCount = launchCount,
)

internal object GestureCodec {
    fun actionName(action: GestureAction): String = when (action) {
        GestureAction.None -> "NONE"
        GestureAction.Notifications -> "NOTIFICATIONS"
        GestureAction.Search -> "SEARCH"
        GestureAction.AllApps -> "ALL_APPS"
        is GestureAction.LaunchApp -> "LAUNCH_APP"
    }

    fun toEntity(binding: GestureBinding): GestureBindingEntity {
        val key = (binding.action as? GestureAction.LaunchApp)?.key
        return GestureBindingEntity(
            direction = binding.direction.name,
            action = actionName(binding.action),
            packageName = key?.packageName,
            activityName = key?.activityName,
            userSerial = key?.userSerial,
        )
    }

    fun toModel(entity: GestureBindingEntity): GestureBinding? {
        val direction = runCatching { SwipeDirection.valueOf(entity.direction) }.getOrNull() ?: return null
        val action = when (entity.action) {
            "NOTIFICATIONS" -> GestureAction.Notifications

            "SEARCH" -> GestureAction.Search

            "ALL_APPS" -> GestureAction.AllApps

            "LAUNCH_APP" -> {
                val pkg = entity.packageName
                val activity = entity.activityName
                val serial = entity.userSerial
                if (pkg != null && activity != null && serial != null) {
                    GestureAction.LaunchApp(AppKey(pkg, activity, serial))
                } else {
                    GestureAction.None
                }
            }

            else -> GestureAction.None
        }
        return GestureBinding(direction, action)
    }
}
