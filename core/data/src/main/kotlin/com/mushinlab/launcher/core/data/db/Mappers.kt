package com.mushinlab.launcher.core.data.db

import com.mushinlab.launcher.core.model.AppKey
import com.mushinlab.launcher.core.model.AppOverride
import com.mushinlab.launcher.core.model.AppUsage
import com.mushinlab.launcher.core.model.GestureAction
import com.mushinlab.launcher.core.model.GestureBinding
import com.mushinlab.launcher.core.model.LearnedGesture
import com.mushinlab.launcher.core.model.Palette
import com.mushinlab.launcher.core.model.PalettePreset
import com.mushinlab.launcher.core.model.Palettes
import com.mushinlab.launcher.core.model.SearchRecent
import com.mushinlab.launcher.core.model.ShapeVertex
import com.mushinlab.launcher.core.model.Space
import com.mushinlab.launcher.core.model.SpaceType
import com.mushinlab.launcher.core.model.SpaceUsage
import com.mushinlab.launcher.core.model.SwipeDirection

internal fun SpaceEntity.toModel(): Space {
    val preset = runCatching { PalettePreset.valueOf(palettePreset) }.getOrDefault(PalettePreset.CUSTOM)
    return Space(
        id = id,
        name = name,
        type = SpaceType.valueOf(type),
        sortOrder = sortOrder,
        preset = preset,
        palette = Palettes.presets[preset] ?: Palette(
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
        coloredIcons = coloredIcons,
        listOffset = listOffset,
        backgroundBrightness = backgroundBrightness,
        backgroundBlur = backgroundBlur,
        hintAt = hintAt,
    )
}

internal fun SpaceItemEntity.key(): AppKey = AppKey(packageName, activityName, userSerial)

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

internal fun SpaceUsageEntity.toModel(): SpaceUsage = SpaceUsage(
    spaceId = spaceId,
    key = AppKey(packageName, activityName, userSerial),
    launchCount = launchCount,
    lastLaunchedAt = lastLaunchedAt,
    decayedScore = decayedScore,
    dismissedAt = dismissedAt,
)

internal fun GestureSampleEntity.toModel(): LearnedGesture =
    LearnedGesture(path, AppKey(packageName, activityName, userSerial), weight, ShapeCodec.decode(shape))

internal fun LearnedGesture.toEntity(): GestureSampleEntity =
    GestureSampleEntity(path, key.packageName, key.activityName, key.userSerial, weight, ShapeCodec.encode(shape))

internal fun SearchRecentEntity.toModel(): SearchRecent = SearchRecent(
    key = AppKey(packageName, activityName, userSerial),
    lastLaunchedAt = lastLaunchedAt,
    launchCount = launchCount,
)

internal object ShapeCodec {
    fun encode(shape: List<ShapeVertex>): String? =
        shape.takeIf { it.isNotEmpty() }?.joinToString(";") { "${it.x},${it.y},${it.dwellMs}" }

    fun decode(raw: String?): List<ShapeVertex> = raw.orEmpty().split(';').mapNotNull { part ->
        val fields = part.split(',')
        if (fields.size != FIELDS) return@mapNotNull null
        val x = fields[0].toFloatOrNull() ?: return@mapNotNull null
        val y = fields[1].toFloatOrNull() ?: return@mapNotNull null
        ShapeVertex(x, y, fields[2].toLongOrNull() ?: 0)
    }

    private const val FIELDS = 3
}

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
