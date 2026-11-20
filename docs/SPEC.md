# PicSwap: especificación funcional

Fuente de verdad funcional. Los requisitos están numerados; las preguntas abiertas, al final.

## 1. Qué es
App **fake** para un efecto de magia. Simula una galería de imágenes de móvil. El mago muestra una foto a pantalla
completa (p. ej. un papelito doblado sobre fondo negro), **arrastra el objeto fuera de la pantalla** y queda solo el
fondo plano: parece que ha sacado el objeto del móvil. Después cierra la app.

Plataformas: Android + iOS (un único código). Solo vertical. Idioma: español. **Todo local: sin red, sin cuentas.**

## 2. Galería (pantalla principal)
- R1. Galería **plana** (sin álbumes ni carpetas), cuadrícula de miniaturas. Aspecto genérico inspirado en una galería
  de Android, no en una marca concreta.
- R2. Solo se ve la foto: **sin fecha, sin nombre, sin menú de detalles**.
- R3. Barra superior con algún botón decorativo que **no hace nada** (solo atrezo).
- R4. **Añadir** foto: botón que ofrece *hacer una foto* o *elegir de la galería del móvil*. La imagen se copia al
  almacenamiento interno de la app **tal cual, sin procesar** (el fondo original se conserva).
- R4b. **Consejos antes de hacer una foto**: al elegir «Hacer una foto» aparece un aviso corto, «Para un buen recorte»,
  con un dibujo bueno/malo y cuatro consejos: superficie lisa y de un solo color; color muy distinto al del objeto; luz
  uniforme sin sombras duras ni reflejos; móvil quieto y objeto bien encuadrado. Botones «Cancelar» / «Hacer foto» e
  interruptor «No volver a mostrar» (también en los ajustes ocultos). El mismo contenido está en la guía de inicio (paso
  «Haz una buena foto») y en las instrucciones (bloque «Cómo hacer una buena foto»).
- R5. **Pulsación larga** sobre una miniatura: modo selección con acciones abajo.
  - «Eliminar» (con confirmación; permite varias fotos). Borra solo de la app, nunca de la galería del móvil.
  - «Quitar fondo» (solo con una foto seleccionada) → abre el editor (§4).
  - Si la foto ya tiene el fondo quitado, «Quitar fondo» pasa a ser «Restaurar original», con una confirmación
    («¿Restaurar la foto original?»); al aceptar vuelve a la foto original.
- R6a. **Guía de inicio**: ventana por encima de la app, con varios pasos y un dibujo en cada uno (efecto, añadir fotos,
  preparar la foto, quitar fondo, hacer el efecto, zoom y ajustes, listo). Aparece al abrir la app mientras esté
  activada (activada por defecto). Se puede saltar. En el último paso hay un interruptor «No mostrar esta guía al abrir la
  app» (refleja el ajuste actual).
- R6b. **Instrucciones**: página dentro de los ajustes con el texto detallado y dibujos: qué es la app y el efecto, la
  galería, el editor (color, tolerancia, desenfoque, cuentagotas, consejos de foto), el visor (arrastre, zoom), los
  ajustes ocultos y qué hacer si algo falla. Los dibujos se generan con Canvas (`Illustrations.kt`), sin imágenes.
- R6. **Ajustes ocultos**: pulsación larga en la esquina superior derecha de la galería (el «⋮»). Contienen:
  - Los **4 colores de fondo predefinidos** del editor (negro, blanco, verde croma y azul croma al arrancar). Se puede
    cambiar cualquiera con una paleta de color («si quiero el negro como marrón»), con botón «Restablecer».
  - El **modo de arrastre** del objeto (ver R9-R10).
  - **Ayuda**: abrir las Instrucciones, ver la guía de inicio, un interruptor «Mostrar la guía al abrir la app» y otro
    «Consejos antes de hacer una foto».
  - El aviso «Desarrollado por Wikicode, Adrián Semi, 2026». 

## 3. Visor de imagen
- R7. Al abrir una foto se muestra **a pantalla completa total**: sin barra de estado, sin barra de navegación, sin
  hora, sin ningún elemento.
- R8. Un **toque** en cualquier sitio alterna entre modo completo y modo normal. En modo normal aparece una barra
  superior estilo visor de imágenes con flecha **atrás** (vuelve a la galería) y varios iconos decorativos que no
  funcionan. Otro toque vuelve a pantalla completa. No hay botón secreto para salir.
- R9. **Efecto de arrastre**: en pantalla completa, el usuario arrastra la imagen con un dedo en **cualquier
  dirección** (arriba, abajo, izquierda, derecha o diagonal, libre). La imagen sigue al dedo y sale de la pantalla;
  debajo queda el fondo plano del mismo color. Un toque simple NO debe confundirse con un arrastre.
- R9b. **El arrastre solo empieza sobre el objeto visible**: si el dedo toca la transparencia (el fondo ya quitado),
  el arrastre no hace nada (ni se alterna la barra). Así se puede sujetar el móvil con otros dedos sobre la zona
  transparente. Se mira el canal alfa de la imagen mostrada, con un margen de ~10 dp para el borde del objeto. En una
  foto sin recortar, toda la imagen cuenta como objeto. Los dedos que tocan la transparencia se ignoran también
  mientras otro dedo hace el arrastre.
- R9c. **Zoom con pellizco** (dos dedos, ambos sobre el objeto): acercar y alejar (de 0,2x a 8x) y mover la imagen
  con los dos dedos. La imagen se queda con el zoom con el que se suelte, hasta salir del visor. El zoom funciona
  también en modo normal; el arrastre solo en pantalla completa. Si la imagen está ampliada o movida, el arrastre la
  saca entera de la pantalla.
- R10. **Modo de arrastre** (ajuste global, dos opciones):
  1. **Arrastrar y soltar**: al soltar, el objeto desaparece. Subopción:
     - *En cualquier momento*: desaparece al soltarlo en cualquier punto (tras un arrastre mínimo de ~12 dp).
     - *Solo en el borde de la pantalla*: solo desaparece si el dedo se suelta dentro de un marco pegado a los
       bordes (ancho ajustable, 30-200 dp, por defecto 80); si se suelta en otro sitio, vuelve a su posición.
  2. **Acompañar**: como pasar de foto en una galería. La imagen sigue al dedo y, al soltar, se va si el gesto es
     suficiente (arrastre ≥ 25 % del lado menor de la pantalla, o lanzamiento rápido en el mismo sentido); si no,
     vuelve. Siempre en la dirección en que el mago hace el gesto.
- R11. El fondo del visor es el color de esa foto (negro si no se le ha quitado el fondo), también en las franjas
  que deja una foto que no tiene la proporción de la pantalla.

## 4. Editor «Quitar fondo» (por foto, sin IA)
- R12. Pantalla tipo editor con la foto y **vista previa en vivo**. Se guarda con «Guardar»; la flecha atrás
  cancela sin cambios.
- R13. **Color** de relleno elegido por foto: los 4 predefinidos de los ajustes (R6) y un **quinto botón** que abre
  una paleta para elegir un color solo para esa foto. Se propone el último usado.
- R14. **Tolerancia** (1-100) ajustable en directo; se propone la última usada.
- R14b. **Desenfoque del borde**, con dos deslizadores (0-100) que solo afectan a la franja del borde del recorte:
  **Desenfoque** (cantidad: 0 = borde duro, 100 = degradado completo) y **Rango del borde** (ancho de la franja junto al
  borde donde se aplica; por defecto 10; proporcional al tamaño de la imagen, así la vista previa y el resultado final
  se ven igual). El resto de la imagen NO se toca: los píxeles del objeto conservan su color exacto y los de la
  transición toman el color del objeto, no el del fondo viejo (sin halo). Se proponen los últimos valores usados.
- R14c. **Tamaño de la foto (zoom)**: en el editor se puede pellizcar (y arrastrar) la vista previa para ajustar el
  tamaño y la posición. El recuadro simula la pantalla del visor (línea fina), así que es WYSIWYG. Se guarda por foto
  (zoom y posición en fracciones de pantalla, en el fichero `.bg`) y es la vista con la que se abre la foto en el visor.
  «Restablecer tamaño» la devuelve a como estaba. El cuentagotas sigue funcionando con zoom.
- R15. **Cuentagotas**: al activarlo y tocar una zona de la foto, ese color (media de 5 × 5 píxeles) es el fondo a
  retirar, y se parte también desde ese punto; hay un enlace para volver a la detección automática por los bordes.
- R16. Algoritmo propio en `commonMain` (`domain/BackgroundCutout.kt`): inundación desde los bordes por similitud de
  color, quitar motas, afinar 1 px y suavizar el borde. Resultado: PNG con transparencia; el fondo lo pinta la app con
  el color de la foto (así el fondo que queda al arrastrar es exactamente ese color).
- R17. Se conservan siempre el original (`.jpg`), el recorte (`.png`) y el color (`.bg`). Restaurar borra los dos
  últimos.

## 5. Fases
1. **Hecha**: galería, añadir, eliminar, visor con modo completo/normal, arrastre libre, persistencia.
2. **Hecha**: quitar fondo sin IA, ahora **manual y por foto** desde la galería (editor en vivo con color, tolerancia
   y cuentagotas; restaurar original).
3. Si el recorte automático no basta con fotos reales: segunda foto con el fondo propio o segmentación por IA
   (ML Kit / Vision). Decisión pendiente de validar con fotos reales.
4. **Hecho**: colores predefinidos editables + color personalizado por foto; modos de arrastre (arrastrar y soltar con
   subopción de borde; acompañar). A validar con uso real; se pueden añadir más modos.
5. **Hecho**: desenfoque del borde, guía de inicio e instrucciones con dibujos.
6. Futuro: carpetas; presets de desenfoque; capturas reales en la guía (ahora son dibujos).

## 6. Preguntas abiertas
- Umbrales del modo «acompañar» (25 % del lado menor / velocidad) y del borde (80 dp): ajustar tras probar en móvil.
- Si el color es negro puro en OLED, valorar el efecto frente al negro de la foto.
- Zoom: ¿hace falta poder pellizcar con los dedos fuera del objeto (ahora ambos dedos deben tocar el objeto)? ¿Un gesto para restablecer el zoom?
- Calidad del recorte con fotos reales (texturas, objeto y fondo de tono parecido).
