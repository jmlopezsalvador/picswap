@file:OptIn(ExperimentalForeignApi::class)

package com.picswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.picswap.domain.PhotoFiles
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithBytes
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImageOrientation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.drawInRect
import platform.darwin.NSObject
import platform.posix.memcpy
import org.jetbrains.skia.Image as SkiaImage

/** iOS: la barra de estado y el indicador de inicio se ocultan siempre desde ContentView.swift. */
@Composable
actual fun SystemBarsEffect(immersive: Boolean) {}

/** iOS no tiene botón «atrás» del sistema. */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {}

actual fun decodeImage(bytes: ByteArray, maxDimension: Int): ImageBitmap? {
    return try {
        val img = SkiaImage.makeFromEncoded(bytes)
        val longest = maxOf(img.width, img.height)
        if (longest <= maxDimension) return img.toComposeImageBitmap()
        val f = maxDimension.toFloat() / longest
        val w = (img.width * f).toInt().coerceAtLeast(1)
        val h = (img.height * f).toInt().coerceAtLeast(1)
        val surface = Surface.makeRasterN32Premul(w, h)
        surface.canvas.drawImageRect(
            img,
            Rect.makeWH(img.width.toFloat(), img.height.toFloat()),
            Rect.makeWH(w.toFloat(), h.toFloat()),
            FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR),
            null,
            true,
        )
        surface.makeImageSnapshot().toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}

actual fun encodeJpeg(image: ImageBitmap, quality: Int): ByteArray {
    val skImage = SkiaImage.makeFromBitmap(image.asSkiaBitmap())
    return skImage.encodeToData(EncodedImageFormat.JPEG, quality)?.bytes ?: ByteArray(0)
}

/** Imagen de Skia a partir de píxeles ARGB no premultiplicados (los bytes van en orden B, G, R, A). */
private fun argbToSkiaImage(width: Int, height: Int, argb: IntArray): SkiaImage {
    val bytes = ByteArray(width * height * 4)
    for (i in argb.indices) {
        val c = argb[i]
        bytes[i * 4] = c.toByte()
        bytes[i * 4 + 1] = (c shr 8).toByte()
        bytes[i * 4 + 2] = (c shr 16).toByte()
        bytes[i * 4 + 3] = (c ushr 24).toByte()
    }
    val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)
    return SkiaImage.makeRaster(info, bytes, width * 4)
}

actual fun imageFromArgb(width: Int, height: Int, argb: IntArray): ImageBitmap =
    argbToSkiaImage(width, height, argb).toComposeImageBitmap()

actual fun encodePng(width: Int, height: Int, argb: IntArray): ByteArray =
    argbToSkiaImage(width, height, argb).encodeToData(EncodedImageFormat.PNG)?.bytes ?: ByteArray(0)

private fun NSData.toByteArray(): ByteArray {
    val n = length.toInt()
    val out = ByteArray(n)
    if (n > 0) out.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return out
}

private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) NSData() else usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }

/** Devuelve la imagen "de pie": las fotos de cámara traen una marca de giro que Skia no aplica. */
private fun UIImage.normalized(): UIImage {
    if (imageOrientation == UIImageOrientation.UIImageOrientationUp) return this
    val s = size
    val w = s.useContents { width }
    val h = s.useContents { height }
    UIGraphicsBeginImageContextWithOptions(s, false, 1.0)
    drawInRect(CGRectMake(0.0, 0.0, w, h))
    val result = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return result ?: this
}

private fun topViewController(): UIViewController? {
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (true) {
        val next = top?.presentedViewController ?: break
        top = next
    }
    return top
}

private class PickerDelegate(
    private val onResult: (ByteArray) -> Unit,
    private val onFinished: () -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        val data = image?.normalized()?.let { UIImageJPEGRepresentation(it, 0.95) }
        picker.dismissViewControllerAnimated(true, completion = null)
        if (data != null) onResult(data.toByteArray())
        onFinished()
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onFinished()
    }
}

private class IosPhotoPicker(private val onPicked: (ByteArray) -> Unit) : PhotoPicker {
    private var delegate: PickerDelegate? = null  // el sistema solo guarda una referencia débil: hay que retenerlo

    override fun pickFromGallery() =
        present(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary)

    override fun takePhoto() =
        present(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)

    private fun present(source: UIImagePickerControllerSourceType) {
        if (!UIImagePickerController.isSourceTypeAvailable(source)) return
        val picker = UIImagePickerController()
        picker.sourceType = source
        val d = PickerDelegate(onPicked) { delegate = null }
        delegate = d
        picker.delegate = d
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }
}

@Composable
actual fun rememberPhotoPicker(onPicked: (ByteArray) -> Unit): PhotoPicker {
    val current = rememberUpdatedState(onPicked)
    return remember { IosPhotoPicker { bytes -> current.value(bytes) } }
}

/** Fotos guardadas en Documentos/photos de la app. */
class IosPhotoFiles : PhotoFiles {
    private val fm = NSFileManager.defaultManager
    private val dir: String = run {
        val docs = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
        val d = "$docs/photos"
        fm.createDirectoryAtPath(d, withIntermediateDirectories = true, attributes = null, error = null)
        d
    }

    override fun list(): List<String> =
        (fm.contentsOfDirectoryAtPath(dir, error = null) as? List<*>)?.filterIsInstance<String>() ?: emptyList()

    override fun read(name: String): ByteArray? = fm.contentsAtPath("$dir/$name")?.toByteArray()

    override fun write(name: String, bytes: ByteArray) {
        fm.createFileAtPath("$dir/$name", contents = bytes.toNSData(), attributes = null)
    }

    override fun delete(name: String) {
        fm.removeItemAtPath("$dir/$name", error = null)
    }
}

class UserDefaultsStore : KeyValueStore {
    private val d = NSUserDefaults.standardUserDefaults
    override fun getString(key: String): String? = d.stringForKey(key)
    override fun getBoolean(key: String, default: Boolean) =
        if (d.objectForKey(key) == null) default else d.boolForKey(key)
    override fun getLong(key: String, default: Long) =
        if (d.objectForKey(key) == null) default else d.integerForKey(key)
    override fun putString(key: String, value: String) = d.setObject(value, key)
    override fun putBoolean(key: String, value: Boolean) = d.setBool(value, key)
    override fun putLong(key: String, value: Long) = d.setInteger(value, key)
}
