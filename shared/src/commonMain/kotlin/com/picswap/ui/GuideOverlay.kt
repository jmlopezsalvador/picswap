package com.picswap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private class GuideStep(val title: String, val text: String, val art: Art)

private val guideSteps = listOf(
    GuideStep(
        "Bienvenido a PicSwap",
        "PicSwap parece una galería de fotos normal, pero sirve para hacer un efecto de magia: enseñas en el móvil la foto " +
            "de un objeto, lo agarras con los dedos y lo sacas de la pantalla. La imagen desaparece y solo queda el fondo, " +
            "como si el objeto hubiera salido del móvil de verdad.",
        Art.Effect,
    ),
    GuideStep(
        "1. Añade tus fotos",
        "Pulsa el botón + para hacer una foto o elegir una de la galería del móvil. Las fotos se guardan solo dentro de " +
            "PicSwap: nunca se tocan las de tu móvil.",
        Art.Gallery,
    ),
    GuideStep(
        "2. Haz una buena foto",
        "Pon el objeto sobre una superficie lisa y de un solo color, muy distinta a los colores del objeto (por ejemplo, un " +
            "papel claro sobre un fondo oscuro). Con luz uniforme, sin sombras duras ni reflejos, y el móvil quieto. " +
            "Cuanto mejor sea la foto, más limpio saldrá el recorte del fondo.",
        Art.PhotoTips,
    ),
    GuideStep(
        "3. Prepara la foto",
        "Mantén pulsada una foto para seleccionarla. Abajo aparecen sus acciones: Eliminar y Quitar fondo. Si ya le has " +
            "quitado el fondo, en su lugar aparece Restaurar original.",
        Art.Selection,
    ),
    GuideStep(
        "4. Quita el fondo",
        "En el editor ves el resultado en directo. Elige el color del fondo, ajusta la tolerancia (cuánto fondo se retira) y " +
            "el desenfoque del borde (para que no se note el corte). Con el cuentagotas tocas en el fondo para decir cuál quitar, " +
            "y pellizcando dejas el objeto del tamaño que quieras.",
        Art.Editor,
    ),
    GuideStep(
        "5. Haz el efecto",
        "Abre la foto: se ve a pantalla completa, sin barras. Arrastra el objeto con un dedo hacia fuera de la pantalla y " +
            "desaparece, dejando solo el color de fondo. Solo funciona si empiezas el arrastre sobre el objeto, no sobre la zona " +
            "transparente: así puedes sujetar el móvil con otros dedos.",
        Art.Viewer,
    ),
    GuideStep(
        "Zoom y ajustes ocultos",
        "Antes del efecto puedes pellizcar con dos dedos sobre el objeto para cambiar su tamaño; se queda como lo dejes. " +
            "En la galería, mantén pulsados los tres puntitos de arriba a la derecha: se abren los ajustes ocultos (colores del " +
            "editor, modo de arrastre, instrucciones y esta guía).",
        Art.Settings,
    ),
    GuideStep(
        "¡Listo!",
        "Puedes volver a ver esta guía y las instrucciones detalladas desde los ajustes ocultos.",
        Art.Ready,
    ),
)

/**
 * Guía de inicio: ventana por encima de la app con varios pasos, cada uno con su dibujo.
 * En el último paso se puede elegir no volver a mostrarla al abrir la app.
 *
 * @param showAtStart si ahora mismo la guía está activada al abrir la app (es el valor inicial del interruptor).
 * @param onClose se llama al cerrar con el nuevo valor de «mostrar al abrir la app».
 */
@Composable
fun GuideOverlay(showAtStart: Boolean, onClose: (showAtStart: Boolean) -> Unit) {
    var step by remember { mutableStateOf(0) }
    var dontShowAgain by remember { mutableStateOf(!showAtStart) }
    val last = step == guideSteps.lastIndex
    val current = guideSteps[step]

    PlatformBackHandler(enabled = true) { if (step > 0) step-- else onClose(showAtStart) }

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f))
            .pointerInput(Unit) { detectTapGestures { } }  // la guía tapa la app: los toques no pasan a lo de debajo
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(24.dp)).background(PicColors.Surface).padding(18.dp),
        ) {
            Illustration(current.art, height = 190.dp)
            Spacer(Modifier.height(14.dp))
            Text(current.title, color = PicColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Column(Modifier.height(150.dp).verticalScroll(rememberScrollState())) {
                Text(current.text, color = PicColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
            }
            Spacer(Modifier.height(10.dp))

            // Indicador de paso
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                for (i in guideSteps.indices) {
                    Box(
                        Modifier.padding(horizontal = 3.dp).size(if (i == step) 9.dp else 7.dp).clip(CircleShape)
                            .background(if (i == step) PicColors.Accent else PicColors.TextSecondary.copy(alpha = 0.5f)),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            if (last) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("No mostrar esta guía al abrir la app", color = PicColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = dontShowAgain,
                        onCheckedChange = { dontShowAgain = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = PicColors.Accent, checkedThumbColor = Color.White),
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (!last) {
                    Text(
                        "Saltar", color = PicColors.TextSecondary, fontSize = 14.sp,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onClose(showAtStart) }.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                if (step > 0) {
                    Text(
                        "Atrás", color = PicColors.Accent, fontSize = 14.sp,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { step-- }.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(PicColors.Accent)
                        .clickable { if (last) onClose(!dontShowAgain) else step++ }
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (last) "Empezar" else "Siguiente", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
