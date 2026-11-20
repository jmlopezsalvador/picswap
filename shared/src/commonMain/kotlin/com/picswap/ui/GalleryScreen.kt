package com.picswap.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla principal: galería plana de miniaturas.
 * - Toque en una foto: la abre. Pulsación larga: modo selección con acciones abajo
 *   (Eliminar y, si hay una sola foto, «Quitar fondo» o «Restaurar original»).
 * - Botón «+»: añadir foto (cámara o galería del móvil).
 * - Pulsación larga en el icono de arriba a la derecha (⋮): ajustes ocultos.
 *
 * `bgColors` contiene, para cada foto con el fondo ya quitado, el color de fondo con el que se ve.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    ids: List<String>,
    importing: Int,
    bgColors: Map<String, Long>,
    imageVersion: Int,
    loadThumb: suspend (String) -> ImageBitmap?,
    onOpen: (String) -> Unit,
    onDelete: (Set<String>) -> Unit,
    onRemoveBackground: (String) -> Unit,
    onRestoreOriginal: (String) -> Unit,
    onPickFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var showAdd by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }
    val selecting = selected.isNotEmpty()
    val single = selected.singleOrNull()

    PlatformBackHandler(enabled = selecting) { selected = emptySet() }

    Box(Modifier.fillMaxSize().background(PicColors.Bg)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            // ---- Barra superior ----
            Row(
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selecting) {
                    IconTap(onClick = { selected = emptySet() }) { AppIcon(IconKind.Close, PicColors.TextPrimary) }
                    Text(
                        if (selected.size == 1) "1 seleccionada" else "${selected.size} seleccionadas",
                        color = PicColors.TextPrimary, fontSize = 20.sp,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                    )
                } else {
                    Text(
                        "Fotos", color = PicColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f).padding(start = 16.dp),
                    )
                    // Iconos de atrezo: no hacen nada.
                    IconTap(onClick = {}) { AppIcon(IconKind.Search, PicColors.TextPrimary) }
                    IconTap(onClick = {}) { AppIcon(IconKind.Sliders, PicColors.TextPrimary) }
                    // El «⋮» parece un menú más, pero con pulsación larga abre los ajustes ocultos.
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).combinedClickable(onClick = {}, onLongClick = onOpenSettings),
                        contentAlignment = Alignment.Center,
                    ) { AppIcon(IconKind.More, PicColors.TextPrimary) }
                }
            }

            // ---- Cuadrícula ----
            if (ids.isEmpty() && importing == 0) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No hay fotos", color = PicColors.TextPrimary, fontSize = 18.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Pulsa + para añadir la primera", color = PicColors.TextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 112.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(importing) {
                        Box(
                            Modifier.aspectRatio(1f).background(PicColors.Surface),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(Modifier.size(28.dp), color = PicColors.Accent, strokeWidth = 3.dp) }
                    }
                    items(ids, key = { it }) { id ->
                        Tile(
                            id = id,
                            bgColor = bgColors[id]?.let { Color(it) } ?: PicColors.Surface,
                            imageVersion = imageVersion,
                            selecting = selecting,
                            selected = id in selected,
                            loadThumb = loadThumb,
                            onClick = {
                                if (selecting) selected = if (id in selected) selected - id else selected + id
                                else onOpen(id)
                            },
                            onLongClick = { selected = if (id in selected) selected - id else selected + id },
                        )
                    }
                }
            }
        }

        if (selecting) {
            // ---- Acciones de la selección ----
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(PicColors.Surface)
                    .windowInsetsPadding(WindowInsets.navigationBars).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ActionButton(IconKind.Trash, "Eliminar") { confirmDelete = true }
                if (single != null) {
                    if (single in bgColors) {
                        ActionButton(IconKind.Undo, "Restaurar original") { confirmRestore = true }
                    } else {
                        ActionButton(IconKind.Wand, "Quitar fondo") {
                            selected = emptySet()
                            onRemoveBackground(single)
                        }
                    }
                }
            }
        } else {
            // ---- Botón añadir ----
            Box(
                Modifier.align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(20.dp)
                    .shadow(6.dp, CircleShape)
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(PicColors.Accent)
                    .clickable { showAdd = true },
                contentAlignment = Alignment.Center,
            ) { AppIcon(IconKind.Plus, Color.White, 28.dp) }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            containerColor = PicColors.Surface,
            title = { Text("Añadir foto", color = PicColors.TextPrimary) },
            text = {
                Column {
                    DialogOption("Hacer una foto") { showAdd = false; onTakePhoto() }
                    DialogOption("Elegir de la galería del móvil") { showAdd = false; onPickFromGallery() }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton({ showAdd = false }) { Text("Cancelar", color = PicColors.Accent) } },
        )
    }

    if (confirmDelete) {
        val n = selected.size
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = PicColors.Surface,
            title = { Text(if (n == 1) "¿Eliminar 1 foto?" else "¿Eliminar $n fotos?", color = PicColors.TextPrimary) },
            text = { Text("Se eliminarán solo de PicSwap.", color = PicColors.TextSecondary) },
            confirmButton = {
                TextButton({
                    confirmDelete = false
                    onDelete(selected)
                    selected = emptySet()
                }) { Text("Eliminar", color = PicColors.Accent) }
            },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancelar", color = PicColors.Accent) } },
        )
    }

    if (confirmRestore && single != null) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            containerColor = PicColors.Surface,
            title = { Text("¿Restaurar la foto original?", color = PicColors.TextPrimary) },
            text = { Text("Se perderá el fondo que hayas aplicado a esta foto.", color = PicColors.TextSecondary) },
            confirmButton = {
                TextButton({
                    confirmRestore = false
                    onRestoreOriginal(single)
                    selected = emptySet()
                }) { Text("Restaurar", color = PicColors.Accent) }
            },
            dismissButton = { TextButton({ confirmRestore = false }) { Text("Cancelar", color = PicColors.Accent) } },
        )
    }
}

@Composable
private fun IconTap(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
private fun ActionButton(icon: IconKind, label: String, onClick: () -> Unit) {
    Column(
        Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(icon, PicColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(label, color = PicColors.TextPrimary, fontSize = 12.sp)
    }
}

@Composable
private fun DialogOption(text: String, onClick: () -> Unit) {
    Text(
        text, color = PicColors.TextPrimary, fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Tile(
    id: String,
    bgColor: Color,
    imageVersion: Int,
    selecting: Boolean,
    selected: Boolean,
    loadThumb: suspend (String) -> ImageBitmap?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val bmp by produceState<ImageBitmap?>(null, id, imageVersion) { value = loadThumb(id) }
    Box(
        Modifier.aspectRatio(1f).background(bgColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        bmp?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        if (selected) Box(Modifier.fillMaxSize().background(PicColors.Accent.copy(alpha = 0.35f)))
        if (selecting) {
            Box(
                Modifier.padding(6.dp).size(24.dp).clip(CircleShape)
                    .background(if (selected) PicColors.Accent else Color.Black.copy(alpha = 0.35f))
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) { if (selected) AppIcon(IconKind.Check, Color.White, 16.dp) }
        }
    }
}
