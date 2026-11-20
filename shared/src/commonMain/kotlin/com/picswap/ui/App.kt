package com.picswap.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.picswap.domain.PhotoFiles
import com.picswap.domain.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

private sealed interface Screen {
    data object Gallery : Screen
    data class Viewer(val id: String) : Screen
    data class Editor(val id: String) : Screen
    data object Settings : Screen
    data object Instructions : Screen
}

/** Lado mayor (px) con el que se guardan las fotos importadas y con el que se muestran a pantalla completa. */
private const val FULL_SIZE = 2048
private const val THUMB_SIZE = 360

/** Color de fondo de una foto sin recortar (el que se ve en las franjas del visor). */
private const val DEFAULT_BG = 0xFF000000L

/** Raíz Compose común (Android e iOS): navegación entre galería, visor, editor de fondo y ajustes ocultos. */
@Composable
fun PicSwapRoot(store: SettingsStore, photoFiles: PhotoFiles) {
    var settings by remember { mutableStateOf(store.load()) }
    val repo = remember { PhotoRepository(photoFiles, { Clock.System.now().toEpochMilliseconds() }) }
    var ids by remember { mutableStateOf(repo.ids()) }
    /** Color de fondo de cada foto a la que ya se le ha quitado el fondo. */
    var bgColors by remember { mutableStateOf(repo.bgColors()) }
    var importing by remember { mutableStateOf(0) }
    var imageVersion by remember { mutableStateOf(0) }  // sube al cambiar una imagen: recarga miniaturas y visor
    var screen by remember { mutableStateOf<Screen>(Screen.Gallery) }
    var immersive by remember { mutableStateOf(true) }
    var showGuide by remember { mutableStateOf(settings.showGuide) }  // la guía de inicio, por encima de todo
    val scope = rememberCoroutineScope()

    val thumbCache = remember { HashMap<String, ImageBitmap>() }
    val loadThumb = remember<suspend (String) -> ImageBitmap?> {
        { id ->
            thumbCache[id] ?: withContext(Dispatchers.Default) {
                repo.displayBytes(id)?.let { decodeImage(it, THUMB_SIZE) }
            }?.also { thumbCache[id] = it }
        }
    }
    val loadFull = remember<suspend (String) -> ImageBitmap?> {
        { id -> withContext(Dispatchers.Default) { repo.displayBytes(id)?.let { decodeImage(it, FULL_SIZE) } } }
    }

    // Importar: la foto se guarda tal cual (normalizada a 2048 px). No se procesa nada automáticamente.
    val picker = rememberPhotoPicker { bytes ->
        scope.launch {
            importing++
            withContext(Dispatchers.Default) {
                val jpeg = decodeImage(bytes, FULL_SIZE)?.let { encodeJpeg(it, 92) }
                if (jpeg != null && jpeg.isNotEmpty()) repo.add(jpeg)
            }
            ids = repo.ids()
            importing--
        }
    }

    SystemBarsEffect(immersive = screen is Screen.Viewer && immersive)

    MaterialTheme(colorScheme = darkColorScheme()) {
        when (val s = screen) {
            Screen.Gallery -> GalleryScreen(
                ids = ids,
                importing = importing,
                bgColors = bgColors,
                imageVersion = imageVersion,
                loadThumb = loadThumb,
                onOpen = { immersive = true; screen = Screen.Viewer(it) },
                onDelete = { toDelete ->
                    toDelete.forEach { repo.remove(it); thumbCache.remove(it) }
                    ids = repo.ids()
                    bgColors = bgColors - toDelete
                },
                onRemoveBackground = { screen = Screen.Editor(it) },
                onRestoreOriginal = { id ->
                    repo.restoreOriginal(id)
                    bgColors = bgColors - id
                    thumbCache.remove(id)
                    imageVersion++
                },
                onPickFromGallery = picker::pickFromGallery,
                onTakePhoto = picker::takePhoto,
                showPhotoTips = settings.showPhotoTips,
                onPhotoTipsChange = { settings = settings.copy(showPhotoTips = it).also(store::save) },
                onOpenSettings = { screen = Screen.Settings },
            )
            is Screen.Viewer -> {
                PlatformBackHandler(enabled = true) { screen = Screen.Gallery }
                ViewerScreen(
                    id = s.id,
                    bgColor = Color(bgColors[s.id] ?: DEFAULT_BG),
                    immersive = immersive,
                    onToggleChrome = { immersive = !immersive },
                    onBack = { screen = Screen.Gallery },
                    imageVersion = imageVersion,
                    drag = settings.drag,
                    loadFull = loadFull,
                )
            }
            is Screen.Editor -> EditorScreen(
                presets = settings.presets,
                initialColor = settings.lastColor,
                initialTolerance = settings.lastTolerance,
                initialFeather = settings.lastFeather,
                loadOriginal = { maxSize ->
                    withContext(Dispatchers.Default) { repo.bytes(s.id)?.let { decodeImage(it, maxSize) } }
                },
                onSave = { params ->
                    // El recorte definitivo se calcula con la foto completa (la vista previa va a menor tamaño).
                    val png = withContext(Dispatchers.Default) {
                        repo.bytes(s.id)?.let { decodeImage(it, FULL_SIZE) }?.let { makeCutoutPng(it, params) }
                    }
                    if (png != null) {
                        repo.saveCutout(s.id, png, params.color)
                        bgColors = bgColors + (s.id to params.color)
                        thumbCache.remove(s.id)
                        imageVersion++
                        settings = settings.copy(lastColor = params.color, lastTolerance = params.tolerance, lastFeather = params.feather)
                            .also(store::save)
                    }
                    screen = Screen.Gallery
                },
                onBack = { screen = Screen.Gallery },
            )
            Screen.Settings -> SettingsScreen(
                settings = settings,
                onChange = { settings = it; store.save(it) },
                onBack = { screen = Screen.Gallery },
                onOpenInstructions = { screen = Screen.Instructions },
                onShowGuide = { showGuide = true },
            )
            Screen.Instructions -> InstructionsScreen(onBack = { screen = Screen.Settings })
        }

        if (showGuide) {
            GuideOverlay(
                showAtStart = settings.showGuide,
                onClose = { show ->
                    settings = settings.copy(showGuide = show).also(store::save)
                    showGuide = false
                },
            )
        }
    }
}
