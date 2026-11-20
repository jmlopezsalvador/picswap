package com.picswap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.picswap.domain.DragMode
import kotlin.math.roundToInt

/**
 * Ajustes ocultos (se abren con una pulsación larga en la esquina superior derecha de la galería):
 * - Los 4 colores de fondo predefinidos del editor (se pueden cambiar con una paleta).
 * - El comportamiento del arrastre del objeto en el visor.
 */
@Composable
fun SettingsScreen(settings: AppSettings, onChange: (AppSettings) -> Unit, onBack: () -> Unit) {
    PlatformBackHandler(enabled = true, onBack = onBack)
    var editingPreset by remember { mutableStateOf<Int?>(null) }
    var edgeValue by remember(settings.drag.edgeDp) { mutableStateOf(settings.drag.edgeDp.toFloat()) }
    val drag = settings.drag

    Column(Modifier.fillMaxSize().background(PicColors.Bg).windowInsetsPadding(WindowInsets.systemBars)) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                AppIcon(IconKind.Back, PicColors.TextPrimary)
            }
            Text("Ajustes", color = PicColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp))
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
            // ---------- Colores del editor ----------
            SectionTitle("Colores de fondo del editor")
            Hint("Los cuatro colores que aparecen al quitar el fondo de una foto. Toca uno para cambiarlo.")
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                settings.presets.forEachIndexed { i, argb ->
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(Color(argb))
                            .border(1.5.dp, PicColors.TextSecondary, CircleShape)
                            .clickable { editingPreset = i },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Restablecer colores originales", color = PicColors.Accent, fontSize = 14.sp,
                modifier = Modifier.clickable { onChange(settings.copy(presets = DEFAULT_PRESETS)) }.padding(vertical = 6.dp),
            )

            Spacer(Modifier.height(26.dp))

            // ---------- Arrastre del objeto ----------
            SectionTitle("Arrastre del objeto")
            Hint("Cómo se comporta la foto al arrastrarla en el visor para sacar el objeto.")
            Spacer(Modifier.height(8.dp))
            RadioRow(
                selected = drag.mode == DragMode.Drop,
                title = "Arrastrar y soltar",
                subtitle = "El objeto sigue al dedo y desaparece al soltarlo.",
            ) { onChange(settings.copy(drag = drag.copy(mode = DragMode.Drop))) }
            if (drag.mode == DragMode.Drop) {
                Column(Modifier.padding(start = 40.dp)) {
                    RadioRow(
                        selected = !drag.edgeOnly,
                        title = "En cualquier momento",
                        subtitle = "Desaparece al soltarlo en cualquier punto.",
                    ) { onChange(settings.copy(drag = drag.copy(edgeOnly = false))) }
                    RadioRow(
                        selected = drag.edgeOnly,
                        title = "Solo en el borde de la pantalla",
                        subtitle = "Solo desaparece si lo sueltas junto al borde; si no, vuelve.",
                    ) { onChange(settings.copy(drag = drag.copy(edgeOnly = true))) }
                    if (drag.edgeOnly) {
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Ancho del borde", color = PicColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.width(110.dp))
                            Slider(
                                value = edgeValue,
                                onValueChange = { edgeValue = it },
                                onValueChangeFinished = { onChange(settings.copy(drag = drag.copy(edgeDp = edgeValue.roundToInt()))) },
                                valueRange = 30f..200f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = PicColors.Accent,
                                    activeTrackColor = PicColors.Accent,
                                    inactiveTrackColor = PicColors.Surface,
                                ),
                            )
                            Text("${edgeValue.roundToInt()} dp", color = PicColors.TextPrimary, fontSize = 13.sp,
                                modifier = Modifier.padding(start = 8.dp).width(50.dp))
                        }
                    }
                }
            }
            RadioRow(
                selected = drag.mode == DragMode.Follow,
                title = "Acompañar",
                subtitle = "Como pasar de foto en una galería: sigue al dedo y se va si el gesto es suficiente; si no, vuelve.",
            ) { onChange(settings.copy(drag = drag.copy(mode = DragMode.Follow))) }
        }

        Text(
            "Desarrollado por Wikicode · Adrián Semi · 2026",
            color = PicColors.TextSecondary, fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            textAlign = TextAlign.Center,
        )
    }

    editingPreset?.let { index ->
        ColorPickerDialog(
            initial = settings.presets[index],
            onDismiss = { editingPreset = null },
            onPick = { picked ->
                onChange(settings.copy(presets = settings.presets.mapIndexed { i, c -> if (i == index) picked else c }))
                editingPreset = null
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = PicColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun Hint(text: String) {
    Spacer(Modifier.height(4.dp))
    Text(text, color = PicColors.TextSecondary, fontSize = 13.sp)
}

@Composable
private fun RadioRow(selected: Boolean, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.padding(top = 2.dp).size(22.dp).clip(CircleShape)
                .border(2.dp, if (selected) PicColors.Accent else PicColors.TextSecondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(12.dp).clip(CircleShape).background(PicColors.Accent))
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(title, color = PicColors.TextPrimary, fontSize = 15.sp)
            Text(subtitle, color = PicColors.TextSecondary, fontSize = 12.sp)
        }
    }
}
