package com.picswap.ui

import com.picswap.domain.DragMode
import com.picswap.domain.DragSettings

/** Almacenamiento clave-valor local. Android: SharedPreferences. iOS: NSUserDefaults (ver las `actual`). */
interface KeyValueStore {
    fun getString(key: String): String?
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getLong(key: String, default: Long): Long
    fun putString(key: String, value: String)
    fun putBoolean(key: String, value: Boolean)
    fun putLong(key: String, value: Long)
}

/** Colores de fondo predefinidos del editor al arrancar la app (negro, blanco, verde croma, azul croma). */
val DEFAULT_PRESETS: List<Long> = listOf(0xFF000000L, 0xFFFFFFFFL, 0xFF00B140L, 0xFF0047BBL)

/**
 * Ajustes de la app.
 * @param presets los 4 colores de fondo predefinidos del editor (editables en los ajustes ocultos).
 * @param drag cómo se comporta el arrastre del objeto en el visor.
 * @param lastColor / `lastTolerance`: lo último que se usó en el editor (se propone por defecto).
 */
data class AppSettings(
    val presets: List<Long> = DEFAULT_PRESETS,
    val drag: DragSettings = DragSettings(),
    val lastColor: Long = 0xFF000000L,
    val lastTolerance: Int = 40,
    val lastFeather: Int = 10,
    val lastFeatherAmount: Int = 100,
    /** Mostrar la guía de inicio al abrir la app. */
    val showGuide: Boolean = true,
    /** Mostrar los consejos antes de hacer una foto con la cámara. */
    val showPhotoTips: Boolean = true,
)

class SettingsStore(private val prefs: KeyValueStore) {
    fun load(): AppSettings = AppSettings(
        presets = DEFAULT_PRESETS.mapIndexed { i, def -> prefs.getLong("preset$i", def) },
        drag = DragSettings(
            mode = if (prefs.getLong("dragMode", 0L) == 1L) DragMode.Follow else DragMode.Drop,
            edgeOnly = prefs.getBoolean("dragEdgeOnly", false),
            edgeDp = prefs.getLong("dragEdgeDp", 80L).toInt(),
        ),
        lastColor = prefs.getLong("bgColor", 0xFF000000L),
        lastTolerance = prefs.getLong("tolerance", 40L).toInt(),
        lastFeather = prefs.getLong("feather", 10L).toInt(),
        lastFeatherAmount = prefs.getLong("featherAmount", 100L).toInt(),
        showGuide = prefs.getBoolean("showGuide", true),
        showPhotoTips = prefs.getBoolean("photoTips", true),
    )

    fun save(s: AppSettings) {
        s.presets.forEachIndexed { i, c -> prefs.putLong("preset$i", c) }
        prefs.putLong("dragMode", if (s.drag.mode == DragMode.Follow) 1L else 0L)
        prefs.putBoolean("dragEdgeOnly", s.drag.edgeOnly)
        prefs.putLong("dragEdgeDp", s.drag.edgeDp.toLong())
        prefs.putLong("bgColor", s.lastColor)
        prefs.putLong("tolerance", s.lastTolerance.toLong())
        prefs.putLong("feather", s.lastFeather.toLong())
        prefs.putLong("featherAmount", s.lastFeatherAmount.toLong())
        prefs.putBoolean("showGuide", s.showGuide)
        prefs.putBoolean("photoTips", s.showPhotoTips)
    }
}
