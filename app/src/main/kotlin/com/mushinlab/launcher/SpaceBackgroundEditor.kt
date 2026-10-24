package com.mushinlab.launcher

import android.content.ContentResolver
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import com.mushinlab.launcher.core.model.BackgroundCrop
import com.mushinlab.launcher.core.model.CropFrame
import com.mushinlab.launcher.core.model.SpaceBackground
import com.mushinlab.launcher.feature.settings.BackgroundEdit
import com.mushinlab.launcher.feature.settings.BackgroundPreset
import com.mushinlab.launcher.feature.settings.PresetArt
import com.mushinlab.launcher.feature.settings.SettingsRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditingImage(val spaceId: Long, val bitmap: Bitmap, val fromFile: Boolean)

class SpaceBackgroundEditor(
    private val container: AppContainer,
    private val contentResolver: ContentResolver,
    private val resources: Resources,
    private val scope: CoroutineScope,
    private val navigator: LauncherNavigator,
    private val request: (LauncherEffect) -> Unit,
) {
    private val editing = MutableStateFlow<EditingImage?>(null)
    private var pickTarget: Long? = null
    val source: StateFlow<EditingImage?> = editing.asStateFlow()

    fun pick(spaceId: Long) {
        pickTarget = spaceId
        request(LauncherEffect.PickBackground(spaceId))
    }

    fun openFromHome(spaceId: Long) {
        val route = if (spaceId in container.backgrounds.versions.value) {
            SettingsRoute.BackgroundEditor(spaceId)
        } else {
            SettingsRoute.BackgroundPicker(spaceId)
        }
        navigator.openSettings(route)
    }

    fun usePreset(spaceId: Long, preset: BackgroundPreset) {
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                when (preset) {
                    is BackgroundPreset.Plain -> plainBitmap(preset.argb)

                    is BackgroundPreset.Picture ->
                        container.backgrounds.decode(ImageDecoder.createSource(resources, preset.art))
                            ?.let { PresetArt.render(it, preset.dark) }
                }
            }
            open(spaceId, bitmap)
        }
    }

    fun onPicked(uri: Uri?) {
        val spaceId = pickTarget ?: return
        pickTarget = null
        if (uri == null) return
        val source = ImageDecoder.createSource(contentResolver, uri)
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { container.backgrounds.decode(source) }
            if (navigator.settings.isOpen) open(spaceId, bitmap)
        }
    }

    private fun open(spaceId: Long, bitmap: Bitmap?) {
        if (bitmap == null) {
            navigator.notice = Notice(R.string.notice_background_failed)
            return
        }
        editing.value = EditingImage(spaceId, bitmap, fromFile = false)
        navigator.settings.open(SettingsRoute.BackgroundEditor(spaceId))
    }

    fun edit(spaceId: Long) {
        if (editing.value?.spaceId == spaceId) return
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { container.backgrounds.load(spaceId) } ?: return@launch
            if (editing.value?.spaceId != spaceId) editing.value = EditingImage(spaceId, bitmap, fromFile = true)
        }
    }

    fun close(spaceId: Long) {
        if (editing.value?.spaceId == spaceId) editing.value = null
    }

    fun save(edit: BackgroundEdit) {
        val image = editing.value?.takeIf { it.spaceId == edit.spaceId } ?: return
        container.scope.launch(Dispatchers.IO) {
            val bitmap = image.bitmap
            val frame = CropFrame(bitmap.width, bitmap.height, edit.frameWidth.toFloat(), edit.frameHeight.toFloat())
            val unchanged = image.fromFile && edit.crop == BackgroundCrop()
            val saved = unchanged ||
                container.backgrounds.save(
                    edit.spaceId,
                    bitmap,
                    frame.region(edit.crop),
                    edit.frameWidth,
                    edit.frameHeight,
                )
            if (saved) {
                container.looks.setBackgroundLook(edit.spaceId, edit.brightness, edit.blur, edit.rowAlpha)
            } else {
                withContext(Dispatchers.Main) { navigator.notice = Notice(R.string.notice_background_failed) }
            }
        }
    }

    fun remove(spaceId: Long) {
        container.scope.launch(Dispatchers.IO) {
            container.backgrounds.delete(spaceId)
            container.looks.setBackgroundLook(
                spaceId,
                brightness = 0f,
                blur = 0f,
                rowAlpha = SpaceBackground.ROW_ALPHA_DEFAULT,
            )
        }
    }

    suspend fun thumbnail(spaceId: Long): Bitmap? = withContext(Dispatchers.IO) {
        container.backgrounds.loadThumbnail(spaceId, (THUMBNAIL_DP * container.densityScale()).toInt())
    }

    private companion object {
        const val THUMBNAIL_DP = 160f
    }
}

private fun plainBitmap(argb: Int): Bitmap =
    Bitmap.createBitmap(PLAIN_WIDTH, PLAIN_HEIGHT, Bitmap.Config.ARGB_8888).apply { eraseColor(argb) }

private const val PLAIN_WIDTH = 90
private const val PLAIN_HEIGHT = 200
