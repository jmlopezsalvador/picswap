package com.picswap.domain

/** Acceso a los ficheros de fotos de la app (almacenamiento interno). Una implementación por plataforma. */
interface PhotoFiles {
    fun list(): List<String>
    fun read(name: String): ByteArray?
    fun write(name: String, bytes: ByteArray)
    fun delete(name: String)
}

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

    /** Guarda el recorte y el color de fondo (ARGB) con el que esa foto se muestra. */
    fun saveCutout(id: String, pngBytes: ByteArray, bgColor: Long) {
        files.write(cutoutName(id), pngBytes)
        files.write(metaName(id), bgColor.toString().encodeToByteArray())
    }

    /** Color de fondo de cada foto recortada (solo aparecen las que tienen recorte). */
    fun bgColors(): Map<String, Long> {
        val names = files.list().toSet()
        val result = HashMap<String, Long>()
        for (id in ids()) {
            if (cutoutName(id) !in names) continue
            val color = files.read(metaName(id))?.decodeToString()?.trim()?.toLongOrNull() ?: continue
            result[id] = color
        }
        return result
    }

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
