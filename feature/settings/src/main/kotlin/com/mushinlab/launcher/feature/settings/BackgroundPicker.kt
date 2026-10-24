package com.mushinlab.launcher.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushinlab.launcher.core.ui.Glyph
import com.mushinlab.launcher.core.ui.LocalMushinColors
import com.mushinlab.launcher.core.ui.MushinType
import com.mushinlab.launcher.core.ui.SingleLineText
import com.mushinlab.launcher.core.ui.pressable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun BackgroundPickerScreen(
    spaceId: Long,
    actions: SettingsActions,
    push: (SettingsRoute) -> Unit,
    back: () -> Unit,
) {
    val folders = PresetFolder.entries
    SettingsPage(stringResource(R.string.background_picker), back) {
        TileGrid(FOLDER_COLUMNS, folders.size + 1) { index, modifier ->
            val folder = folders.getOrNull(index)
            if (folder == null) {
                FolderTile(stringResource(R.string.background_folder_gallery), "folder_gallery", modifier, {
                    actions.background.pick(spaceId)
                }) { GalleryPreview() }
            } else {
                FolderTile(folderName(folder), "folder_${folder.name}", modifier, {
                    push(SettingsRoute.BackgroundFolder(spaceId, folder))
                }) { FolderPreview(folder) }
            }
        }
    }
}

@Composable
internal fun BackgroundFolderScreen(spaceId: Long, folder: PresetFolder, actions: SettingsActions, back: () -> Unit) {
    val presets = BackgroundPresets.of(folder)
    SettingsPage(folderName(folder), back) {
        if (presets.isEmpty()) NoteText(stringResource(R.string.background_folder_empty))
        TileGrid(PRESET_COLUMNS, presets.size) { index, modifier ->
            val description = stringResource(R.string.background_preset, index + 1)
            PresetTile(presets[index], description, modifier.testTag("preset_${folder.name}_$index")) {
                actions.background.usePreset(spaceId, presets[index])
            }
        }
    }
}

@Composable
private fun folderName(folder: PresetFolder): String = stringResource(
    when (folder) {
        PresetFolder.PLAIN -> R.string.background_folder_plain
        PresetFolder.JAPANESE -> R.string.background_folder_japanese
        PresetFolder.COSMOS -> R.string.background_folder_cosmos
    },
)

@Composable
private fun TileGrid(columns: Int, count: Int, tile: @Composable (Int, Modifier) -> Unit) {
    Column(
        Modifier.padding(horizontal = CARD_MARGIN, vertical = CARD_GAP),
        verticalArrangement = Arrangement.spacedBy(TILE_GAP),
    ) {
        List(count) { it }.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TILE_GAP)) {
                row.forEach { tile(it, Modifier.weight(1f)) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun FolderTile(
    label: String,
    tag: String,
    modifier: Modifier,
    onClick: () -> Unit,
    preview: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalMushinColors.current
    Column(
        modifier
            .clip(RoundedCornerShape(TILE_RADIUS))
            .background(colors.surface)
            .pressable(onClick = onClick)
            .testTag(tag)
            .padding(TILE_PADDING),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(PREVIEW_RADIUS))
                .background(colors.keyBackground),
            contentAlignment = Alignment.Center,
            content = preview,
        )
        Spacer(Modifier.height(TILE_PADDING))
        SingleLineText(label, MushinType.style(15.sp, colors.textPrimary))
    }
}

@Composable
private fun BoxScope.FolderPreview(folder: PresetFolder) {
    val presets = BackgroundPresets.of(folder)
    when {
        presets.isEmpty() -> BasicText(
            stringResource(R.string.background_folder_empty),
            style = MushinType.style(13.sp, LocalMushinColors.current.textSecondary),
            modifier = Modifier.padding(TILE_PADDING),
        )

        folder == PresetFolder.PLAIN -> Column(Modifier.matchParentSize()) {
            presets.take(SWATCHES).chunked(2).forEach { row ->
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    row.forEach { preset -> PresetImage(preset, Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        }

        else -> Row(Modifier.matchParentSize()) {
            listOf(presets.first(), presets[presets.size / 2]).forEach { preset ->
                PresetImage(preset, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun GalleryPreview() {
    Image(
        painterResource(Glyph.IMAGE.res),
        contentDescription = null,
        colorFilter = ColorFilter.tint(LocalMushinColors.current.textSecondary),
        modifier = Modifier.size(GALLERY_ICON),
    )
}

@Composable
private fun PresetTile(preset: BackgroundPreset, description: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(TILE_RADIUS)
    Box(
        modifier
            .aspectRatio(PRESET_RATIO)
            .clip(shape)
            .border(1.dp, LocalMushinColors.current.divider, shape)
            .semantics { contentDescription = description },
    ) {
        PresetImage(preset, Modifier.fillMaxSize())
        Box(Modifier.matchParentSize().pressable(onClick = onClick))
    }
}

@Composable
private fun PresetImage(preset: BackgroundPreset, modifier: Modifier) {
    when (preset) {
        is BackgroundPreset.Plain -> Box(modifier.background(Color(preset.argb)))

        is BackgroundPreset.Picture -> {
            val resources = LocalResources.current
            val width = with(LocalDensity.current) { PREVIEW_WIDTH.roundToPx() }
            val image by produceState<ImageBitmap?>(null, preset, width) {
                value = withContext(Dispatchers.Default) {
                    PresetArt.preview(resources, preset.art, preset.dark, width)?.asImageBitmap()
                }
            }
            Box(modifier) {
                image?.let {
                    Image(
                        it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
        }
    }
}

private const val FOLDER_COLUMNS = 2
private const val PRESET_COLUMNS = 3
private const val PRESET_RATIO = 9f / 16f
private const val SWATCHES = 4
private val TILE_GAP = 12.dp
private val TILE_RADIUS = 10.dp
private val PREVIEW_RADIUS = 6.dp
private val TILE_PADDING = 8.dp
private val GALLERY_ICON = 40.dp
private val PREVIEW_WIDTH = 120.dp
