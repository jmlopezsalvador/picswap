# PicSwap: app móvil Android + iOS

App móvil para **Android y iOS** con un único código en Kotlin Multiplatform + Compose Multiplatform.
El usuario habla español: responde y comenta el código en español.

## Mapa de carpetas
| Carpeta | Para qué sirve |
|---|---|
| `shared/` | Módulo KMP con TODO el código: lógica pura, interfaz Compose, ajustes. `commonMain` (común), `androidMain` / `iosMain` (solo lo del sistema, `expect`/`actual`), `commonTest` (tests). |
| `app/` | Solo la envoltura Android (`MainActivity`, manifiesto, recursos, icono). |
| `iosApp/` | Envoltura iOS en SwiftUI + `project.yml` (XcodeGen genera el `.xcodeproj` en CI; no se versiona). |
| `.github/workflows/ios.yml` | Compila en un Mac de GitHub y publica un `.ipa` sin firmar. Solo `workflow_dispatch`. |
| `docs/` | `ARQUITECTURA.md` (base técnica, fuente de verdad de versiones y reglas) y `SPEC.md` (especificación funcional, se crea al inicializar). |
| `data/` | Material de referencia que aporta el usuario (capturas, icono, recursos). Se puede leer. |
| `data/android_build/` | APK descargados/generados. No se versionan. |
| `data/ios_build/` | `.ipa` descargado de Actions. No se versiona. |
| `ignore/` | **Ignorar por completo** (ver reglas). |

## Reglas para Claude
- **Ignorar por completo la carpeta `ignore/`**: no leerla, no listarla, no buscar dentro, no usarla como referencia. Excluirla
  también de búsquedas (Glob/Grep) y del control de versiones. Es el cajón del usuario para material suelto.
- La base técnica es **`docs/ARQUITECTURA.md`** (versiones, estructura, Gradle, CI de iOS, reglas de código). Si algo choca con él, consultar al usuario.
- **Antes de lanzar el workflow de iOS en GitHub Actions, preguntar siempre al usuario** (cuesta cuota de macOS; se acumulan mejoras).
  Por defecto se lanza **sin tests** (el usuario prueba en su iPhone); solo con tests si él lo pide.
- No inventar funcionalidad: la fuente de verdad es `docs/SPEC.md` y lo que pida el usuario. Ante la duda, preguntar.
- **No hacer commit ni push sin que el usuario lo pida.** No commitear si la compilación falla.
- Datos solo en local (SharedPreferences / UserDefaults y ficheros internos). Sin red ni cuentas salvo que el SPEC lo diga.
- Mantener este `CLAUDE.md` al día: al cambiar de forma relevante la app, el flujo o las decisiones, actualizarlo en la misma tarea.

## Qué hace la app
App **fake** para un efecto de magia: simula una galería de imágenes. El mago abre una foto (p. ej. un papelito sobre
fondo negro) a pantalla completa total y **arrastra el objeto fuera de la pantalla en cualquier dirección**; queda solo
el fondo plano, como si lo hubiera sacado del móvil. Detalle completo en `docs/SPEC.md`.
- Galería plana de miniaturas (solo la foto, sin fechas ni nombres), botones decorativos que no funcionan, añadir
  (cámara o galería del móvil, **sin procesar**). Solo vertical, español, todo local.
- Pulsación larga en una foto: selección con acciones abajo (Eliminar; y, con una sola foto, «Quitar fondo» o
  «Restaurar original» si ya está recortada).
- «Quitar fondo» abre un editor por foto (vista previa en vivo): color de relleno, tolerancia y cuentagotas. Procesado
  propio sin IA. Se guarda original `.jpg` + recorte `.png` con alfa + color `.bg`; la app pinta el color debajo.
- Visor: pantalla completa; un toque alterna con modo normal (barra superior con flecha atrás e iconos de atrezo);
  arrastre libre en cualquier dirección que deja solo el color de fondo de la foto.
- Ajustes ocultos: pulsación larga en la esquina superior derecha de la galería: 4 colores de fondo editables (paleta), modo de arrastre (arrastrar y soltar [en cualquier momento / solo en el borde] o acompañar) y créditos de Wikicode.
- Editor: los 4 colores vienen de los ajustes + un quinto botón con paleta para un color solo de esa foto.
- Paquete `com.picswap`. Repo: https://github.com/jmlopezsalvador/picswap.git. Icono en `data/icon.png` (ya aplicado a Android e iOS; se regeneró con un script de Pillow).

## Reglas de código para que compile en iOS
- En `commonMain`: nada de `java.*` ni `android.*`. Usar `kotlinx-datetime` (0.6.1), `kotlin.math.*` y, si hace falta aritmética
  grande, `bignum` (`com.ionspin.kotlin:bignum:0.3.10`). Sin `String.format`, `Locale`, `SimpleDateFormat`, `Thread`, `synchronized`.
- Sin recursos de Android (`R.string`) en común; textos en el código.
- Iconos dibujados con `Canvas`/`Path` de Compose (no `material-icons-extended`).
- Lógica de dominio pura y sin UI, con tests en `commonTest` (y reloj inyectable si depende del tiempo).
- Lo del sistema (sensores, vibración, galería, portapapeles…): `expect` en `commonMain/.../ui/Platform.kt` + `actual` en `androidMain`/`iosMain`.
- `Platform.ios.kt` lleva `@file:OptIn(ExperimentalForeignApi::class)` (cinterop de Kotlin/Native).
- El fallo de iOS solo se ve en Actions (~20 min): revisar con cuidado antes de lanzarlo.
- Con scripts: en esta máquina los heredocs de Bash con contenido complejo fallan; escribe los `.py` con la herramienta Write y ejecútalos.

## Compilar y probar (Windows)
- Kotlin 2.0.21, Compose Multiplatform 1.7.1, AGP 8.7.3, Gradle 8.9, JDK 17, minSdk 26, target/compileSdk 35, iOS mínimo 15.0.
- `JAVA_HOME` apunta al JDK 17: `C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot`.
- SDK de Android en `C:\Users\jmlop\AppData\Local\Android\Sdk`: crear `local.properties` con `sdk.dir=...` (ignorado por git).
- `./gradlew.bat :shared:testDebugUnitTest assembleDebug` → APK en `app/build/outputs/apk/debug/app-debug.apk`.
  Copiar el APK a `data/android_build/` si se quiere conservar.
- Emulador: suele haber uno ya arrancado (`emulator-5554`, 1080x2400); AVDs disponibles: calculadora-emu, chronix-emu, omniweb-emu. `adb` en `C:\Users\jmlop\AppData\Local\Android\Sdk\platform-tools`; en Git Bash usar `MSYS_NO_PATHCONV=1` para rutas `/sdcard`. Tras cada avance: instalar (`adb install -r`), reiniciar (`am force-stop` + `am start -n com.picswap/.MainActivity`) y comprobar con capturas. Si se arranca uno nuevo: poner la zona horaria `Europe/Madrid` (arranca en GMT). Gboard puede salir en negro (es del emulador).
- `gh` en Windows: `C:\Program Files\GitHub CLI\gh.exe` (hace falta `gh auth login` una vez).
- En Windows los destinos de iOS se ignoran (`kotlin.native.ignoreDisabledTargets=true`); Android compila y testea igual.

## Flujo de trabajo
1. Desarrollar y probar en Android/JVM en local (emulador).
2. Commit y push cuando el usuario lo pida.
3. **Preguntar antes de lanzar Actions** y acumular mejoras: `gh workflow run ios.yml --ref main` (opción `-f tests=true` solo si se pide).
4. Descargar el artefacto a `data/ios_build/`: `gh run download <id> -n <Nombre>-ios-sin-firmar -D data/ios_build`.
5. Instalar el `.ipa` con Sideloadly/AltStore (Apple ID gratis; caduca a los 7 días; activar Modo desarrollador en el iPhone).

## Lecciones de iOS ya aprendidas
- `gradlew` debe estar en git con permiso de ejecución (`git update-index --chmod=+x gradlew`).
- En iOS, `UIDevice.proximityState` se lee por sondeo; apagar y reactivar la monitorización deja el estado pegado. Mantenerla activa.
- Barra de estado e indicador de inicio se ocultan desde SwiftUI (`ContentView.swift`); el bloqueo automático con `idleTimerDisabled`.

## Instrucciones para el usuario (si la app las necesita)
Si la app lleva una página de «Instrucciones» de cara al usuario final, mantenerla al día en la misma tarea en que se cambie la función.

## Estado de las fases (ver `docs/SPEC.md` §5)
- **Fase 1 hecha (Android verificado en emulador):** galería, añadir (galería/cámara), eliminar por pulsación larga, visor a pantalla completa con arrastre libre, ajustes ocultos (color de fondo).
- **iOS (`Platform.ios.kt`): escrito pero sin compilar** (solo se ve en Actions). Revisar si falla: `Surface`/`drawImageRect` de Skia, `UIImagePickerController` y las conversiones `NSData`.
- **Fase 2 hecha (Android verificado): quitar fondo sin IA, manual y por foto** (editor en vivo + restaurar original).
  Algoritmo en `domain/BackgroundCutout.kt` (con tests). El color y la tolerancia ya no son globales: se eligen por foto
  (los últimos usados se proponen por defecto).
- **Colores y arrastre hechos (Android verificado):** colores predefinidos editables, color personalizado por foto y dos modos
  de arrastre (reglas puras en `domain/DragRules.kt`, color en `domain/ColorMath.kt`; el gesto en `ViewerScreen.kt`).
- **Siguiente:** validar con fotos reales del usuario; si no basta el recorte, segunda foto o IA (fase 3). Ajustar umbrales
  del arrastre tras probar en móvil.
