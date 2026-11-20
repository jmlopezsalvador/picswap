package com.picswap.domain

/** Acceso a los ficheros de fotos de la app (almacenamiento interno). Una implementación por plataforma. */
interface PhotoFiles {
    fun list(): List<String>
    fun read(name: String): ByteArray?
    fun write(name: String, bytes: ByteArray)
    fun delete(name: String)
}

/**
 * Datos de una foto recortada: color de fondo (ARGB) y vista por defecto en el visor. El zoom es un factor sobre el
 * tamaño «ajustado a pantalla»; el desplazamiento (`panX`, `panY`) va en fracciones del ancho y el alto de la pantalla.
 */
data class PhotoMeta(val bgColor: Long, val zoom: Float = 1f, val panX: Float = 0f, val panY: Float = 0f)

/**
 * Colección plana de fotos de la app. Cada foto es un fichero `<marca de tiempo>.jpg` (original) y,
 * opcionalmente, `<marca de tiempo>.png` (recorte con el fondo transparente).
 * Eliminar solo borra el fichero interno; nunca toca la galería del móvil.
 */
class PhotoRepository(
    private val files: PhotoFiles,
    private val nowMillis: () -> Long,
) {
    /** Identificadores de las fotos, de la más reciente a la más antigua. */
    fun ids(): List<String> = files.list()
        .filter { it.endsWith(EXT) }
        .sortedByDescending { it.removeSuffix(EXT).toLongOrNull() ?: 0L }

    fun add(jpegBytes: ByteArray): String {
        val existing = files.list().toSet()
        var stamp = nowMillis()
        while ("$stamp$EXT" in existing) stamp++
        val id = "$stamp$EXT"
        files.write(id, jpegBytes)
        return id
    }

    /** Bytes del original (JPEG). */
    fun bytes(id: String): ByteArray? = files.read(id)

    /** Recorte con transparencia (PNG) de la foto, si existe. */
    fun cutoutBytes(id: String): ByteArray? = files.read(cutoutName(id))

    /** Guarda el recorte junto con los datos de la foto (color de fondo y vista por defecto). */
    fun saveCutout(id: String, pngBytes: ByteArray, meta: PhotoMeta) {
        files.write(cutoutName(id), pngBytes)
        files.write(metaName(id), "${meta.bgColor} ${meta.zoom} ${meta.panX} ${meta.panY}".encodeToByteArray())
    }

    fun saveCutout(id: String, pngBytes: ByteArray, bgColor: Long) = saveCutout(id, pngBytes, PhotoMeta(bgColor))

    /** Datos de cada foto recortada (solo aparecen las que tienen recorte). */
    fun metas(): Map<String, PhotoMeta> {
        val names = files.list().toSet()
        val result = HashMap<String, PhotoMeta>()
        for (id in ids()) {
            if (cutoutName(id) !in names) continue
            val parts = files.read(metaName(id))?.decodeToString()?.trim()?.split(' ')?.filter { it.isNotBlank() } ?: continue
            val color = parts.getOrNull(0)?.toLongOrNull() ?: continue
            result[id] = PhotoMeta(
                bgColor = color,
                zoom = parts.getOrNull(1)?.toFloatOrNull() ?: 1f,
                panX = parts.getOrNull(2)?.toFloatOrNull() ?: 0f,
                panY = parts.getOrNull(3)?.toFloatOrNull() ?: 0f,
            )
        }
        return result
    }

    /** Color de fondo de cada foto recortada. */
    fun bgColors(): Map<String, Long> = metas().mapValues { it.value.bgColor }

    /** Lo que se muestra: el recorte si existe y, si no, el original. */
    fun displayBytes(id: String): ByteArray? = cutoutBytes(id) ?: bytes(id)

    /** Vuelve a la foto original (borra el recorte y su color). */
    fun restoreOriginal(id: String) {
        files.delete(cutoutName(id))
        files.delete(metaName(id))
    }

    /** Elimina la foto y todo lo suyo. */
    fun remove(id: String) {
        files.delete(id)
        restoreOriginal(id)
    }

    private fun cutoutName(id: String) = id.removeSuffix(EXT) + ".png"
    private fun metaName(id: String) = id.removeSuffix(EXT) + ".bg"

    private companion object {
        const val EXT = ".jpg"
    }
}
