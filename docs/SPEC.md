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
- R5. **Pulsación larga** sobre una miniatura: modo selección con acciones abajo.
  - «Eliminar» (con confirmación; permite varias fotos). Borra solo de la app, nunca de la galería del móvil.
  - «Quitar fondo» (solo con una foto seleccionada) → abre el editor (§4).
  - Si la foto ya tiene el fondo quitado, «Quitar fondo» pasa a ser «Restaurar original», con una confirmación
    («¿Restaurar la foto original?»); al aceptar vuelve a la foto original.
- R6. **Ajustes ocultos**: pulsación larga en la esquina superior derecha de la galería (el «⋮»). Contienen:
  - Los **4 colores de fondo predefinidos** del editor (negro, blanco, verde croma y azul croma al arrancar). Se puede
    cambiar cualquiera con una paleta de color («si quiero el negro como marrón»), con botón «Restablecer».
  - El **modo de arrastre** del objeto (ver R9-R10).
  - El aviso «Desarrollado por Wikicode, Adrián Semi, 2026». Más adelante: instrucciones.

## 3. Visor de imagen
- R7. Al abrir una foto se muestra **a pantalla completa total**: sin barra de estado, sin barra de navegación, sin
  hora, sin ningún elemento.
- R8. Un **toque** en cualquier sitio alterna entre modo completo y modo normal. En modo normal aparece una barra
  superior estilo visor de imágenes con flecha **atrás** (vuelve a la galería) y varios iconos decorativos que no
  funcionan. Otro toque vuelve a pantalla completa. No hay botón secreto para salir.
- R9. **Efecto de arrastre**: en pantalla completa, el usuario arrastra la imagen con un dedo en **cualquier
  dirección** (arriba, abajo, izquierda, derecha o diagonal, libre). La imagen sigue al dedo y sale de la pantalla;
  debajo queda el fondo plano del mismo color. Un toque simple NO debe confundirse con un arrastre.
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
5. Futuro: carpetas, instrucciones en ajustes ocultos.

## 6. Preguntas abiertas
- Umbrales del modo «acompañar» (25 % del lado menor / velocidad) y del borde (80 dp): ajustar tras probar en móvil.
- Si el color es negro puro en OLED, valorar el efecto frente al negro de la foto.
- Calidad del recorte con fotos reales (texturas, objeto y fondo de tono parecido).
