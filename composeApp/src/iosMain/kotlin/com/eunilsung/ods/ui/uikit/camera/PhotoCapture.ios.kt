package com.eunilsung.ods.ui.uikit.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.posix.memcpy

@Composable
actual fun rememberPhotoCapture(onResult: (ByteArray?) -> Unit): () -> Unit {
    val callback by rememberUpdatedState(onResult)
    val delegate = remember { PickerDelegate { callback(it) } }
    return remember(delegate) {
        {
            val picker = UIImagePickerController()
            val camera = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
            picker.sourceType = if (UIImagePickerController.isSourceTypeAvailable(camera)) camera
            else UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
            picker.delegate = delegate
            val presenter = topViewController()
            if (presenter == null) callback(null) else presenter.presentViewController(picker, animated = true, completion = null)
        }
    }
}

/** 지금 맨 위에 떠 있는 화면 — 거기서 카메라를 띄운다. */
private fun topViewController(): UIViewController? {
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}

/** 찍거나 고른 사진을 JPEG 로 바꿔 넘긴다. 취소하면 null. */
private class PickerDelegate(
    private val onResult: (ByteArray?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        val bytes = image?.let { UIImageJPEGRepresentation(it, JPEG_QUALITY) }?.toByteArray()
        picker.dismissViewControllerAnimated(true, completion = null)
        onResult(bytes)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onResult(null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    return bytes
}

private const val JPEG_QUALITY = 0.85
