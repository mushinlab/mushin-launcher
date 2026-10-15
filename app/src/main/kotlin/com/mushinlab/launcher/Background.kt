package com.mushinlab.launcher

import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun MainActivity.pickBackground() {
    backgroundPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
}

internal fun MainActivity.setBackground(uri: Uri) {
    val container = (application as MushinApp).container
    val source = ImageDecoder.createSource(contentResolver, uri)
    container.scope.launch(Dispatchers.IO) {
        if (!container.background.save(source)) {
            withContext(Dispatchers.Main) { notice = Notice(getString(R.string.notice_background_failed)) }
        }
    }
}
