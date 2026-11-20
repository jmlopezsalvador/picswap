package com.picswap.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.picswap.domain.PhotoFiles
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
actual fun SystemBarsEffect(immersive: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        // Que el contenido llegue también bajo la muesca de la cámara (sin franja negra arriba).
        if (Build.VERSION.SDK_INT >= 28) {
            val attrs = window.attributes
            attrs.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = attrs
        }
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        if (immersive) controller.hide(WindowInsetsCompat.Type.systemBars())
        else controller.show(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

actual fun decodeImage(bytes: ByteArray, maxDimension: Int): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDimension) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    var bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null

    // Orientación EXIF (las fotos de cámara suelen venir "tumbadas" con una marca de giro).
    val orientation = try {
        ExifInterface(ByteArrayInputStream(bytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (e: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }
    val m = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
    }
    if (!m.isIdentity) bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)

    // Ajuste final exacto al tamaño máximo (el muestreo por potencias de 2 se queda corto).
    val longest = maxOf(bmp.width, bmp.height)
    if (longest > maxDimension) {
        val f = maxDimension.toFloat() / longest
        bmp = Bitmap.createScaledBitmap(bmp, (bmp.width * f).toInt().coerceAtLeast(1), (bmp.height * f).toInt().coerceAtLeast(1), true)
    }
    return bmp.asImageBitmap()
}

actual fun encodeJpeg(image: ImageBitmap, quality: Int): ByteArray {
    val out = ByteArrayOutputStream()
    image.asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, quality, out)
    return out.toByteArray()
}

actual fun imageFromArgb(width: Int, height: Int, argb: IntArray): ImageBitmap =
    Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()

actual fun encodePng(width: Int, height: Int, argb: IntArray): ByteArray {
    val bmp = Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888)
    val out = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
    return out.toByteArray()
}

@Composable
actual fun rememberPhotoPicker(onPicked: (ByteArray) -> Unit): PhotoPicker {
    val context = LocalContext.current
    val currentOnPicked = rememberUpdatedState(onPicked)
    val cameraFile = remember { arrayOfNulls<File>(1) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            val bytes = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (e: Exception) {
                null
            }
            if (bytes != null) currentOnPicked.value(bytes)
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok: Boolean ->
        val file = cameraFile[0]
        if (ok && file != null) {
            val bytes = try { file.readBytes() } catch (e: Exception) { null }
            if (bytes != null) currentOnPicked.value(bytes)
        }
        file?.delete()
        cameraFile[0] = null
    }

    return remember {
        object : PhotoPicker {
            override fun pickFromGallery() {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            override fun takePhoto() {
                val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                val file = File(dir, "tmp_${System.currentTimeMillis()}.jpg")
                cameraFile[0] = file
                val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                cameraLauncher.launch(uri)
            }
        }
    }
}

/** Fotos guardadas en el almacenamiento interno de la app (carpeta `photos`). */
class AndroidPhotoFiles(context: Context) : PhotoFiles {
    private val dir = File(context.filesDir, "photos").apply { mkdirs() }
    override fun list(): List<String> = dir.list()?.toList() ?: emptyList()
    override fun read(name: String): ByteArray? = File(dir, name).takeIf { it.exists() }?.readBytes()
    override fun write(name: String, bytes: ByteArray) { File(dir, name).writeBytes(bytes) }
    override fun delete(name: String) { File(dir, name).delete() }
}

class SharedPrefsStore(context: Context) : KeyValueStore {
    private val p = context.getSharedPreferences("picswap", Context.MODE_PRIVATE)
    override fun getString(key: String): String? = p.getString(key, null)
    override fun getBoolean(key: String, default: Boolean) = p.getBoolean(key, default)
    override fun getLong(key: String, default: Long) = p.getLong(key, default)
    override fun putString(key: String, value: String) = p.edit().putString(key, value).apply()
    override fun putBoolean(key: String, value: Boolean) = p.edit().putBoolean(key, value).apply()
    override fun putLong(key: String, value: Long) = p.edit().putLong(key, value).apply()
}
