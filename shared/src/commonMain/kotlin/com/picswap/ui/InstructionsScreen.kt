package com.picswap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Un bloque de las instrucciones: título, párrafos y, entre ellos, dibujos. */
private class Block(val title: String, val items: List<Item>)

private sealed interface Item {
    class P(val text: String) : Item
    class Sub(val text: String) : Item
    class Bullets(val lines: List<String>) : Item
    class Pic(val art: Art, val caption: String) : Item
}

private val instructions = listOf(
    Block(
        "¿Qué es PicSwap?",
        listOf(
            Item.P(
                "PicSwap parece una galería de fotos cualquiera, pero sirve para hacer un efecto de magia. El mago enseña en el " +
                    "móvil la foto de un objeto (por ejemplo, un papel doblado o una carta sobre un fondo oscuro), lo agarra con " +
                    "los dedos y lo saca de la pantalla: la imagen desaparece y solo queda el fondo, como si el objeto hubiera " +
                    "salido del móvil de verdad.",
            ),
            Item.Pic(Art.Effect, "El objeto sale por el lado de la pantalla y solo queda el fondo."),
            Item.P(
                "La clave es que el fondo que queda sea del mismo color que el fondo de la foto, para que no se note ningún " +
                    "corte. Por eso la app tiene un editor que quita el fondo de la foto y lo sustituye por un color plano exacto.",
            ),
            Item.Sub("Resumen de cómo se prepara"),
            Item.Bullets(
                listOf(
                    "1. Haz una foto del objeto sobre una superficie lisa y de un solo color, muy distinta a la del objeto, y añádela a la galería.",
                    "2. Mantén pulsada la foto y elige Quitar fondo. Ajusta el color, la tolerancia y el desenfoque, y guarda.",
                    "3. Abre la foto: se ve a pantalla completa. Sujeta el móvil como siempre, agarra el objeto y arrástralo fuera.",
                ),
            ),
            Item.P("Todo funciona sin internet y sin cuentas: tus fotos se quedan solo en el móvil, dentro de la app."),
        ),
    ),
    Block(
        "La galería",
        listOf(
            Item.Pic(Art.Gallery, "Galería con las fotos y el botón + para añadir."),
            Item.Bullets(
                listOf(
                    "Botón +: te deja hacer una foto o elegir una de la galería del móvil. La foto se copia a PicSwap tal como está, sin procesar. Antes de abrir la cámara aparece un aviso con consejos para hacer una buena foto (se puede desactivar en los ajustes ocultos).",
                    "Un toque en una foto: la abre a pantalla completa.",
                    "Pulsación larga en una foto: entra en modo selección. Puedes tocar más fotos para marcarlas.",
                    "Los botones de búsqueda y filtros de arriba son de adorno: no hacen nada. Forman parte del disfraz de galería.",
                    "Los tres puntitos de arriba a la derecha son un botón secreto: manteniéndolos pulsados se abren los ajustes ocultos.",
                ),
            ),
            Item.Pic(Art.Selection, "En la selección aparecen las acciones abajo."),
            Item.Sub("Acciones de la selección"),
            Item.Bullets(
                listOf(
                    "Eliminar: borra las fotos seleccionadas de PicSwap (pide confirmación). Nunca borra nada de la galería del móvil.",
                    "Quitar fondo (solo con una foto seleccionada): abre el editor para preparar la foto.",
                    "Restaurar original: si la foto ya tiene el fondo quitado, esta acción sustituye a la anterior. Pide confirmación y devuelve la foto original.",
                ),
            ),
        ),
    ),
    Block(
        "Cómo hacer una buena foto",
        listOf(
            Item.P(
                "La calidad del recorte depende sobre todo de la foto. Si el objeto y el fondo se distinguen bien, el editor " +
                    "separará el fondo casi sin esfuerzo. Estos consejos hacen que salga mejor a la primera:",
            ),
            Item.Pic(Art.PhotoTips, "Bien: fondo liso y distinto al objeto. Mal: fondo con dibujo y parecido al objeto."),
            Item.Bullets(
                listOf(
                    "Superficie lisa y de un solo color: una mesa sin dibujo, una cartulina, una tela lisa. Evita manteles con estampado, madera con vetas o suelos con textura.",
                    "Color muy distinto al del objeto: papel o carta clara sobre fondo oscuro, o al revés. Si el objeto y el fondo se parecen, el editor no sabrá dónde acaba uno y empieza el otro.",
                    "Luz uniforme: mejor luz natural o una lámpara que ilumine por igual. Evita sombras duras, focos que hagan brillos y reflejos sobre la superficie.",
                    "Encuadre limpio: que el objeto quede bien visible, sin cortar, y con algo de fondo alrededor. Sujeta el móvil quieto para que la foto salga nítida.",
                    "Un solo objeto por foto: así el editor sabe cuál es el que debe conservar.",
                ),
            ),
            Item.P("Si ya tienes una foto con el fondo irregular, aún puedes probar a subir la tolerancia o usar el cuentagotas en el editor."),
        ),
    ),
    Block(
        "Quitar el fondo (el editor)",
        listOf(
            Item.P(
                "Es la herramienta clave de la app. Sustituye el fondo de la foto por un color plano exacto, de modo que, al sacar " +
                    "el objeto, el fondo que queda sea idéntico al de la foto. Arriba ves el resultado en directo; abajo están los controles.",
            ),
            Item.Pic(Art.Editor, "El editor: vista previa arriba y controles abajo."),
            Item.Bullets(
                listOf(
                    "Color: el color que se verá detrás del objeto. Hay cuatro colores predefinidos (se pueden cambiar en los ajustes ocultos) y un quinto botón, con forma de arcoíris, que abre una paleta para elegir un color solo para esta foto.",
                    "Tolerancia: cuánto fondo se retira. Con una tolerancia baja solo se quita el fondo muy parecido al color de referencia; con una alta se quita más. Si el fondo tiene sombras o luces irregulares, sube la tolerancia. Si empieza a comerse partes del objeto, bájala.",
                    "Desenfoque: suaviza el borde entre el objeto y el fondo. Con 0 el corte es duro y puede notarse; con valores medios se disimula; con valores altos el borde queda muy difuminado. Pruébalo en directo hasta que no se aprecie el corte.",
                    "Cuentagotas: normalmente la app detecta el fondo por sí sola mirando los bordes de la foto. Si falla, activa el cuentagotas y toca en una zona del fondo: ese es el color que se quitará, y se parte desde ese punto. Verás un aro donde tocaste. Con «Volver a automático» recuperas la detección normal.",
                    "Guardar: aplica el recorte a la foto. Con la flecha de atrás sales sin cambios.",
                ),
            ),
            Item.Pic(Art.Eyedropper, "El cuentagotas: toca el fondo que quieres quitar."),
            Item.Sub("Si no te gusta el resultado"),
            Item.P(
                "Usa Restaurar original desde la galería y vuelve a empezar: la foto original siempre se conserva. Si el recorte " +
                    "no queda bien, revisa también los consejos para hacer la foto.",
            ),
        ),
    ),
    Block(
        "Hacer el efecto (el visor)",
        listOf(
            Item.P(
                "Al abrir una foto se muestra a pantalla completa, sin ninguna barra ni reloj. Con un toque en cualquier sitio " +
                    "aparece una barra superior con la flecha de atrás (y unos iconos de adorno); otro toque la oculta.",
            ),
            Item.Pic(Art.Viewer, "Arrastra el objeto hacia fuera de la pantalla."),
            Item.Bullets(
                listOf(
                    "Arrastrar: pon un dedo sobre el objeto y arrástralo hacia cualquier lado (arriba, abajo, izquierda, derecha o en diagonal). El objeto sale de la pantalla y queda solo el color de fondo. Para repetir el efecto, sal de la foto y vuelve a abrirla.",
                    "El arrastre solo funciona si empieza sobre el objeto visible, no sobre la zona transparente. Así puedes sujetar el móvil con otros dedos apoyados en la pantalla sin que pase nada.",
                    "El arrastre solo funciona en pantalla completa. Si ves la barra superior, toca la pantalla una vez para ocultarla.",
                ),
            ),
            Item.Pic(Art.Hold, "Un dedo sobre la transparencia se ignora; el dedo sobre el objeto lo arrastra."),
            Item.Sub("Zoom"),
            Item.P(
                "Para ajustar el tamaño del objeto antes del efecto, pellizca con dos dedos sobre el objeto: hacia fuera para " +
                    "acercar y hacia dentro para alejar. También puedes moverlo con los dos dedos. La imagen se queda con el zoom con " +
                    "el que la sueltes. Los dos dedos deben empezar sobre el objeto.",
            ),
            Item.Pic(Art.Pinch, "Pellizco con dos dedos sobre el objeto."),
        ),
    ),
    Block(
        "Los ajustes ocultos",
        listOf(
            Item.P(
                "En la galería, mantén pulsados los tres puntitos de la esquina superior derecha. Se abre una pantalla de ajustes " +
                    "que el público no ve nunca (el botón parece un menú más).",
            ),
            Item.Pic(Art.Settings, "Colores del editor y modo de arrastre."),
            Item.Sub("Colores de fondo del editor"),
            Item.P(
                "Son los cuatro colores que aparecen en el editor. Al empezar son negro, blanco, verde croma y azul croma. Toca uno " +
                    "para cambiarlo con una paleta (por ejemplo, un marrón en lugar del negro). Con «Restablecer colores originales» " +
                    "vuelven los de fábrica. Cambiar un color predefinido no modifica las fotos que ya tienen un color guardado.",
            ),
            Item.Sub("Arrastre del objeto"),
            Item.P("Aquí eliges cómo se comporta el objeto al arrastrarlo en el visor. Hay dos formas:"),
            Item.Bullets(
                listOf(
                    "Arrastrar y soltar: el objeto sigue al dedo y desaparece al soltarlo. Tiene dos variantes: «En cualquier momento» (desaparece al soltarlo en cualquier punto, tras un pequeño arrastre) y «Solo en el borde de la pantalla» (solo desaparece si sueltas el dedo dentro de un marco pegado a los bordes; si lo sueltas en otro sitio, el objeto vuelve a su posición). Con la segunda variante puedes ajustar el ancho del marco.",
                    "Acompañar: imita el gesto de pasar de foto en una galería. El objeto sigue al dedo y, al soltar, se va si has arrastrado lo suficiente o si has hecho un gesto rápido; si no, vuelve a su sitio. Sale siempre en la dirección en que haces el gesto.",
                ),
            ),
            Item.Pic(Art.EdgeBand, "Modo «solo en el borde»: el objeto desaparece al soltarlo en el marco."),
            Item.Sub("Ayuda"),
            Item.P(
                "Desde los ajustes puedes abrir estas instrucciones, volver a ver la guía de inicio, activar o desactivar que la " +
                    "guía aparezca cada vez que se abre la app y activar o desactivar el aviso con consejos que sale antes de hacer una foto.",
            ),
        ),
    ),
    Block(
        "Si algo no sale bien",
        listOf(
            Item.Bullets(
                listOf(
                    "El recorte se come parte del objeto: baja la tolerancia, o usa el cuentagotas tocando en el fondo.",
                    "Quedan restos del fondo: sube la tolerancia, o usa el cuentagotas en la zona donde quedan restos.",
                    "Se nota el borde del objeto o un pequeño halo: sube el desenfoque.",
                    "Al arrastrar no pasa nada: comprueba que estás en pantalla completa (si ves la barra superior, toca una vez) y que el dedo empieza sobre el objeto, no sobre la zona transparente.",
                    "El objeto desaparece muy pronto, con solo mover un poco el dedo: cambia el modo de arrastre a «Solo en el borde» o a «Acompañar».",
                    "Al empezar el arrastre desde el mismo borde de la pantalla, el sistema puede mostrar sus barras o activar el gesto de volver. Empieza el arrastre más hacia el centro del objeto.",
                    "El pellizco no responde: los dos dedos deben empezar sobre el objeto visible, no sobre la transparencia.",
                ),
            ),
        ),
    ),
)

/** Instrucciones detalladas de la app, con dibujos. Se abren desde los ajustes ocultos. */
@Composable
fun InstructionsScreen(onBack: () -> Unit) {
    PlatformBackHandler(enabled = true, onBack = onBack)
    Column(Modifier.fillMaxSize().background(PicColors.Bg).windowInsetsPadding(WindowInsets.systemBars)) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                AppIcon(IconKind.Back, PicColors.TextPrimary)
            }
            Text("Instrucciones", color = PicColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
            instructions.forEachIndexed { index, block ->
                if (index > 0) Spacer(Modifier.height(30.dp))
                Text(block.title, color = PicColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                for (item in block.items) {
                    when (item) {
                        is Item.P -> {
                            Text(item.text, color = PicColors.TextSecondary, fontSize = 14.sp, lineHeight = 21.sp)
                            Spacer(Modifier.height(10.dp))
                        }
                        is Item.Sub -> {
                            Spacer(Modifier.height(4.dp))
                            Text(item.text, color = PicColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                        }
                        is Item.Bullets -> {
                            for (line in item.lines) {
                                Row(Modifier.padding(bottom = 8.dp)) {
                                    Text("•", color = PicColors.Accent, fontSize = 14.sp, modifier = Modifier.padding(end = 10.dp))
                                    Text(line, color = PicColors.TextSecondary, fontSize = 14.sp, lineHeight = 21.sp)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                        is Item.Pic -> {
                            Illustration(item.art)
                            Spacer(Modifier.height(6.dp))
                            Text(item.caption, color = PicColors.TextSecondary.copy(alpha = 0.8f), fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
