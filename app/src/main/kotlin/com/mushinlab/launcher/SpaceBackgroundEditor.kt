package com.mushinlab.launcher

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import com.mushinlab.launcher.core.model.BackgroundCrop
import com.mushinlab.launcher.core.model.CropFrame
import com.mushinlab.launcher.feature.settings.BackgroundEdit
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

    fun onPicked(uri: Uri?) {
        val spaceId = pickTarget ?: return
        pickTarget = null
        if (uri == null) return
        val source = ImageDecoder.createSource(contentResolver, uri)
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { container.backgrounds.decode(source) }
            if (bitmap == null) {
                navigator.notice = Notice(R.string.notice_background_failed)
                return@launch
            }
            if (!navigator.settings.isOpen) return@launch
            editing.value = EditingImage(spaceId, bitmap, fromFile = false)
            val route = SettingsRoute.BackgroundEditor(spaceId)
            if (navigator.settings.top != route) navigator.settings.push(route)
        }
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
                container.looks.setBackgroundLook(edit.spaceId, edit.brightness, edit.blur)
            } else {
                withContext(Dispatchers.Main) { navigator.notice = Notice(R.string.notice_background_failed) }
            }
        }
    }

    fun remove(spaceId: Long) {
        container.scope.launch(Dispatchers.IO) {
            container.backgrounds.delete(spaceId)
            container.looks.setBackgroundLook(spaceId, brightness = 0f, blur = 0f)
        }
    }

    suspend fun thumbnail(spaceId: Long): Bitmap? = withContext(Dispatchers.IO) {
        container.backgrounds.loadThumbnail(spaceId, (THUMBNAIL_DP * container.densityScale()).toInt())
    }

    private companion object {
        const val THUMBNAIL_DP = 160f
    }
}
