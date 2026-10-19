package com.eunilsung.ods.ui.uikit.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * 시스템 카메라 앱으로 원본 사진을 찍는다. 카메라 앱이 앱 캐시의 임시 파일에 쓰게 하고,
 * 읽어서 줄인 뒤 **바로 지운다** — 신분증 사진이 기기에 남지 않게 한다.
 */
@Composable
actual fun rememberPhotoCapture(onResult: (ByteArray?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onResult)
    val file = remember { File(File(context.cacheDir, CAPTURE_DIR).apply { mkdirs() }, CAPTURE_FILE) }
    val uri = remember { FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val bytes = if (saved && file.exists()) runCatching { file.readScaledJpeg() }.getOrNull() else null
        file.delete()
        callback(bytes)
    }
    return {
        file.delete()
        runCatching { launcher.launch(uri) }.onFailure { callback(null) }
    }
}

/** 긴 변이 [MAX_EDGE] 를 넘지 않게 줄여 JPEG 로 읽는다. 원본은 수천 픽셀이라 그대로 들고 있기에는 크다. */
private fun File.readScaledJpeg(): ByteArray? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
    val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    return ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        bitmap.recycle()
        out.toByteArray()
    }
}

private const val CAPTURE_DIR = "capture"
private const val CAPTURE_FILE = "id_card.jpg"
private const val AUTHORITY_SUFFIX = ".capture"
private const val MAX_EDGE = 1600
private const val JPEG_QUALITY = 85
