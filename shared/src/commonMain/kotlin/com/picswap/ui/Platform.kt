package com.picswap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Lo que depende del sistema se declara aquí y se implementa en androidMain / iosMain.
 * Todo lo demás va en común.
 */

/** Modo inmersivo: oculta (true) o muestra (false) las barras de estado y de navegación. */
@Composable
expect fun SystemBarsEffect(immersive: Boolean)

/** Botón/gesto «atrás» del sistema (Android). En iOS no hace nada. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

/**
 * Decodifica una imagen reduciéndola para que su lado mayor no pase de `maxDimension` píxeles
 * (y aplicando la orientación EXIF si la hay). Devuelve null si los bytes no son una imagen válida.
 */
expect fun decodeImage(bytes: ByteArray, maxDimension: Int): ImageBitmap?

/** Codifica una imagen como JPEG con la calidad dada (0-100). */
expect fun encodeJpeg(image: ImageBitmap, quality: Int): ByteArray

/** Codifica píxeles ARGB (no premultiplicados) como PNG conservando la transparencia. */
expect fun encodePng(width: Int, height: Int, argb: IntArray): ByteArray

/** Crea una imagen a partir de píxeles ARGB (no premultiplicados). Sirve para la vista previa en vivo. */
expect fun imageFromArgb(width: Int, height: Int, argb: IntArray): ImageBitmap

/** Lanzadores del sistema para obtener una foto nueva. El resultado llega como bytes de una imagen. */
interface PhotoPicker {
    fun pickFromGallery()
    fun takePhoto()
}

/** Crea los lanzadores. `onPicked` se llama con los bytes originales de la foto elegida o hecha. */
@Composable
expect fun rememberPhotoPicker(onPicked: (ByteArray) -> Unit): PhotoPicker
