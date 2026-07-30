# Linkora — versión nativa Android

Port completo de la PWA v1.5.0 a Kotlin + Jetpack Compose (Material 3).

## Cómo compilar e instalar

1. **Abrir en Android Studio**: `File → Open` y selecciona la carpeta `linkora-android`.
2. Studio descargará Gradle y las dependencias en el primer *sync* (unos minutos).
   Si te propone actualizar AGP o Kotlin, **acepta**: las versiones fijadas aquí son
   conservadoras a propósito.
3. Conecta el móvil con **depuración USB** activada.
4. Pulsa *Run* (▶). Se instala directamente, sin pasar por Play Store.

Para generar un APK que puedas pasar a otros:
`Build → Build Bundle(s)/APK(s) → Build APK(s)`
El archivo sale en `app/build/outputs/apk/debug/app-debug.apk`.
El APK de debug se instala en cualquier móvil con "orígenes desconocidos" permitido.

## Estructura

| Fichero | Contenido |
|---|---|
| `data/Model.kt` | Entidades Room (`LinkItem`, `Category`), DAO y base de datos |
| `data/Files.kt` | Copia de archivos compartidos y generación de miniaturas |
| `data/Meta.kt` | Metadatos Open Graph con Jsoup (sin CORS, sin proxies) |
| `data/Brands.kt` | Reconocimiento de plataforma por dominio |
| `data/Backup.kt` | Copia de seguridad en ZIP (data.json + archivos reales) |
| `vm/MainViewModel.kt` | Estado, acciones, entrada de compartición, purga |
| `vm/Undo.kt` | Pila de deshacer/rehacer (5 acciones) |
| `ui/*` | Tema, tarjetas, pantallas, hojas modales, visor de imágenes |
| `MainActivity.kt` | Barra inferior, pager con swipe, orquestación |

## Equivalencias con la PWA

| PWA | Nativo |
|---|---|
| IndexedDB | Room (SQLite) |
| Blobs en IndexedDB | Archivos en `filesDir/media`, ruta en la base de datos |
| Service worker + share target | `intent-filter` de `ACTION_SEND` |
| `noembed` / `microlink` | Jsoup directo sobre la página |
| Gate de instalación | Innecesario: ya es una app |
| Cache versionado | Innecesario: ya no hay shell que cachear |
| Copia en JSON con base64 | ZIP con los archivos reales |

## Diferencias de comportamiento

- **Miniaturas**: se leen los metadatos Open Graph directamente, así que funcionan en
  sitios donde los proxies fallaban (Instagram, LinkedIn, prensa con muros blandos).
- **Abrir archivos**: las imágenes se ven en un visor propio con zoom; los PDF los abre
  el visor del sistema. Si tienes app por defecto, ya no aparece el selector.
- **Almacenamiento**: Android no puede purgarlo como hacía con el navegador.
- **Datos**: la app nativa no lee los datos de la PWA. Para migrar, exporta desde la PWA
  e impórtalo aquí (el formato de importación acepta el ZIP nuevo; para el JSON antiguo
  hay que añadir un conversor si te hace falta).

## Versión

Un único sitio: la constante `BuildConfigVersion` al final de `MainActivity.kt`,
y `versionName` en `app/build.gradle.kts`. Mantenlas iguales.

## Nota

Este código no se ha compilado todavía. Es normal que el primer *build* saque algún
error de import o de versión de dependencia; pásame el mensaje de Studio y lo corrijo.
