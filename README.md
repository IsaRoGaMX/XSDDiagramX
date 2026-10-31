# XSDDiagramX - Port Kotlin/JavaFX de XSD Diagram (fases 1-4: dominio + GUI + exportación + Settings/MRU/CLI/validación; fase 5: formulario Settings + tema Claro/Oscuro; impresión eliminada a petición del usuario)

> Attribution notice: Este repositorio es un port derivado del proyecto original
> [XSD Diagram](https://github.com/dgis/xsddiagram), desarrollado por
> **Regis Cosnier** y otros contribuidores del proyecto original.
> El código original está licenciado bajo **GPL / LGPL / MS-PL** según indican
> los archivos de licencia de ese proyecto y su `ReadMe.txt`.
> Este repositorio no reemplaza ni elimina la autoría original.
>
> Este port se publica con atribución clara al proyecto fuente y se recomienda
> mantener su licencia compatible para evitar conflictos legales.
>
> Repositorio original: https://github.com/dgis/xsddiagram
> Autor original: Regis Cosnier
> Contribuidores citados en el proyecto original: Mathieu Malaterre,
> Paul Selormey, Edu Serna, TCH68k y otros autores mencionados en el `ReadMe.txt`.

Port en **Kotlin + JavaFX** llamado **XSDDiagramX**, de [XSD Diagram](https://github.com/dgis/xsddiagram)
(C#, WinForms, .NET 2.0, Regis Cosnier — GPL/LGPL/MS-PL).

## Origen y licencia

Este repositorio es un port derivado del proyecto original
[XSD Diagram](https://github.com/dgis/xsddiagram), desarrollado por
**Regis Cosnier** y otros contribuidores del proyecto original.

El proyecto original está licenciado bajo **GPL / LGPL / MS-PL** según
se indica en sus archivos de licencia y en su `ReadMe.txt`.
Este repositorio no reemplaza la autoría original ni pretende
presentarse como un proyecto independiente del original.

Este port se distribuye como trabajo derivado del proyecto original,
manteniendo la atribución correspondiente al repositorio fuente y a
sus autores. Por seguridad jurídica y por compatibilidad con la obra
original, se recomienda publicar este repositorio bajo la licencia
**GPL-2.0-or-later**.

Esto significa que:
- se mantiene la atribución al proyecto original;
- se reconoce la autoría de Regis Cosnier y de los contribuyentes
  originales;
- la licencia del port es compatible con el origen del código;
- no se oculta que este proyecto es un port de XSD Diagram.

Repositorio original:
- https://github.com/dgis/xsddiagram

Autor original:
- Regis Cosnier

Contribuidores citados en el proyecto original:
- Mathieu Malaterre
- Paul Selormey
- Edu Serna
- TCH68k
- y otros autores mencionados en `ReadMe.txt`

> Este repositorio es una adaptación/port a Kotlin/JavaFX del proyecto
> original y no sustituye ni elimina la autoría original.

**Fase 1 completada**: estructuración del proyecto + port 1:1 de la capa de dominio
(parsing + motor del diagrama). Los tests de dominio cargan los esquemas reales
de la carpeta `Tests\` del original.

**Fase 2 completada**: GUI JavaFX — `MainForm` con menús/toolbar/paneles,
`DiagramControlContainer` + `DiagramControl` (scroll virtual + pan + rueda) y
renderer sobre `Canvas` (port de `DiagramGdiRenderer.cs`). Reproduce la
aplicación original (menú File/Window/Help, combo de zoom, alineación,
documentación/tipo/bounding-box, panel derecho con elementos/atributos/
enumeraciones + documentación, pestañas con el contenido de cada .xsd, barra
de estado con el path del elemento, drag & drop de ficheros).

**Fase 3 completada**: exportación (Save Diagram As: SVG/TXT/CSV/PNG/JPG) con
los ports de `DiagramSvgRenderer`, `DiagramTxtRenderer` y `DiagramExporter`;
icono de aplicación en title bar/taskbar (Dock de macOS) vía `Stage.icons`.
(Nota: impresión/Page/Print Preview se ELIMINARON a petición del usuario.)

**Fase 4 completada**: Settings persistentes (`java.util.prefs`), SettingsForm
(el "PropertyGrid" del Settings), MRUManager persistente (Recent Files),
parseo de la línea de comandos (`Options`) y validación de XML
(Validate XML Files...).

**Fase 5 en curso (mejoras de la GUI)**:

- **SettingsForm como formulario**: a petición del usuario, la ventana
  Settings pasó de la tabla Property/Value (equivalente del `PropertyGrid`)
  a un formulario (`GridPane` con un control por setting). Cambia
  `app/SettingsForm.kt`; el MainActivity.ChangeSetting del port
  (`MainForm.changeSetting`) se sigue usando tal cual.
- **Tema Claro / Oscuro** (adición del port, no existe en el C#): campo
  "Tema" en Settings; el valor se persiste (nodo Preferences,
  clave `Theme`). El claro reproduce el esquema del original (diagrama
  blanco con tinta negra, selección 0xA6CAF0; UI Modena por defecto). El
  oscuro usa una paleta neutra de contraste moderado: `xsddiagramx/AppTheme.kt`
  define la paleta de dibujo del diagrama y `theme-dark.css`/`scrollbar-dark.css`
  (resources) sólo cambian las variables de color de Modena — los controles
  conservan su forma. Cambio en caliente: al elegir el tema se re-aplican los
  stylesheets a la ventana principal, al SettingsForm y al diálogo de carga de
  esquema, y el diagrama se repinta (`changeSetting("Theme")`). Además se
  instala como user agent stylesheet vía `StyleManager` (JavaFX interno; flag
  `--add-exports javafx.graphics/com.sun.javafx.css=ALL-UNNAMED` en el build,
  ya configurado) para que los POPUPS de combos/menús —Escenes aparte que no
  heredan los stylesheets de la ventana— reciban el tema y no salten warnings
  del `modena.bss`. Las exportaciones PNG/JPG y SVG/TXT dibujan SIEMPRE con la
  paleta clara ("papel" blanco).
  Capturas: `screenshots/28...33` (formulario y ventana en ambos temas) y
  `37-popup-ua-dark.png` (popup del combo en oscuro). «Las opciones Page/
  Print Preview/Print se ELIMINARON a petición del usuario.»

## Ejecutar

Requiere JDK 17+ (probado con JDK 21). Sin Gradle instalado: usar el wrapper.

```bat
gradlew.bat run            :: abre la ventana GUI
gradlew.bat test           :: tests de dominio + Options
```

Línea de comandos (GUI): `gradlew.bat run --args="[-r root@ns]... [-e N] [-d] [-z N] input.xsd"` — carga el esquema al arrancar, añade los root elements y expande el nivel indicado (los flags [...]

Nota: el proyecto se detecta la plataforma (win/mac/linux) para las dependencias
JavaFX de Maven Central; no se usa ningún plugin de terceros.

## Estructura (mapeo C# → Kotlin)

```
PROYECTO ORIGINAL (C#)                      ESTE PORT (Kotlin)
xsddiagram-master\                          
src\main\kotlin\xsddiagram\
│   ├── Parsing\XmlSchema.cs  <- xsd.exe    │   ├── parsing\XmlSchema.kt       (modelo de datos)
│   │       (XmlSerializer)                 │   ├── parsing\XmlSchemaReader.kt (parser DOM del JDK)
│   ├── Parsing\Schema.cs                   │   ├── parsing\Schema.kt          (SchemaLoader + XSDObject...)
│   ├── DiagramHelpers.cs                   │   └── DiagramHelpers.kt
│   └── Rendering\                          │   └── rendering\
│       ├── DiagramItemType/GroupType/      │       ├── DiagramEnums.kt
│       │   Alignement/HitTestRegion .cs    │       │
│       ├── (System.Drawing)                │       ├── Geometry.kt (Point/Size/Rectangle + operadores)
│       │                                   │       ├── DrawingContext.kt (medición desacoplada)
│       ├── DiagramItem.cs                  │       ├── DiagramItem.kt
│       ├── Diagram.cs                      │       └── Diagram.kt
│       ├── DiagramRenderer/Gdi/Svg/Txt.cs   │       ├── DiagramRenderer.kt (base, fase 3)
│       │   + DiagramExporter.cs            │       ├── DiagramSvgRenderer.kt / DiagramTxtRenderer.kt (fase 3)
│       │                                   │       ├── DiagramExporter.kt (fase 3)
│       │                                   │       └── javafx\DiagramJavaFxRenderer.kt (≈ GDI, fase 2)
│       │                                   │
├── MainForm.cs / DiagramControl*.cs        ├── app\MainForm.kt, DiagramControl(Container).kt (fase 2)
│                                           │
├── Options.cs                              ├── Options.kt (fase 4, parseo de la línea de comandos)
├── Settings.cs (+ Properties\Settings.*)   ├── app\Settings.kt (fase 4)
├── MRUManager.cs                           ├── app\MRUManager.kt (fase 4)
├── SettingsForm.cs / SettingsForm.Designer.├── app\SettingsForm.kt (fase 4)
│                                           │
├── XSDDiagramConsole\                      │    (consola: pendiente en fase 5; la lógica batch vive en Program.cs)
└── Tests\...*.xsd                          \src\test\resources\schemas\    (copias de Tests\)
```

Documentación del original: `../xsddiagram-master/ReadMe.txt` y diagramas UML en
`../xsddiagram-master/XSDDiagrams/Diagrams/*.cd`.

## Qué se portó (fases 1-2) y qué queda

Portado con tests de comportamiento equivalentes a los casos de `Tests\issues\`:

| Original (C#) | Port (Kotlin) | Notas del port |
|---|---|---|
| `XMLSchema.*` (xsd.exe, 2443 líneas) | `parsing/XmlSchema.kt` | Sólo el subconjunto parseado/usado: sin facetas de simpleType (sólo base+lista), sin `key/keyref/notation`. |
| `XmlSerializer` (deserialización) | `parsing/XmlSchemaReader.kt` | Parser DOM del JDK: resuelve QNames via `lookupNamespaceURI` y `xml:lang` via `getAttributeNS`. **No hay dependencias de terceros** |
| `Schema.cs` + XSDObject/XSDAttribute/XSDAttributeGroup | `parsing/Schema.kt` | `SchemaLoader`: import recursivo include/import, descarga HTTP con credenciales 401/403, strip de `<!DOCTYPE...>`, etc. |
| `Diagram.cs` | `rendering/Diagram.kt` | Expansión perezosa, layout, hit-test, búsqueda, `RequestAnyElement` (callback), herencia (`extension`/`restriction`) y grupos de substitución. |
| `DiagramItem.cs` | `rendering/DiagramItem.kt` | Measure/Location recursivos y `HitTest` (con `HitTestResult` en lugar de `out`). |
| `System.Drawing.Point/Size/Rectangle/Font/Graphics` | `rendering/Geometry.kt` + `rendering/DrawingContext.kt` | KPoint/KSize/KRectangle/SizeF + `DrawingContext` (interface de medición) y `FontD...` |
| `DiagramGdiRenderer.cs` | `rendering/javafx/DiagramJavaFxRenderer.kt` | Formas element/type/group (polígonos de 6 y 8 puntos), glifos Sequence/Choice/All, herencia dashed + triángulo, etc. |
| `JavaFxDrawingContext` (nuevo) | `rendering/javafx/JavaFxDrawingContext.kt` | ≈ `Graphics.MeasureString` con `javafx.scene.text.Text.layoutBounds`. |
| `DiagramControl.cs` | `app/DiagramControl.kt` | `Canvas` con `paint(virtualPoint)`, `clientSize`, `clientRectangle`. |
| `DiagramControlContainer.cs` | `app/DiagramControlContainer.kt` | Scrollbars sobre tamaño virtual (`max = virtualSize - clientSize`, ≈ `Maximum - LargeChange` de WinForms), pan arrastrando, etc. |
| `MainForm.cs` + `MainForm.Designer.cs` | `app/MainForm.kt` | Menús File/Window/Help (Recent Files en memoria, fase 4 lo persiste), toolbar con texto en vez de iconos (los PNG van en Resources...). |
| `XMLSchema.facet` (xsd.exe) | `parsing/XmlSchema.kt` (`class facet`) | Las facetas de `xs:restriction` ya se portan reales (`kind` + `value`): alimenta el tab "Element" y la columna de enumeraciones. |
| `DiagramRenderer.cs` | `rendering/DiagramRenderer.kt` (fase 3) | Base abstracta Begin/Render/End de los renderers de salida (TXT/SVG). |
| `DiagramTxtRenderer.cs` | `rendering/DiagramTxtRenderer.kt` (fase 3) | TXT (tabulado) y CSV (`isCSV`): cabecera con los `TextOutputFields` y filas PATH/NAME/TYPE/NAMESPACE/COMMENT/SEQ/LASTCHILD/... |
| `DiagramSvgRenderer.cs` | `rendering/DiagramSvgRenderer.kt` (fase 3) | SVG texto: líneas de herencia/referencia/grupos, clipping docs, etc. |
| `DiagramExporter.cs` | `rendering/DiagramExporter.kt` (fase 3) | Export por extensión: PNG/JPG con canvas offscreen JavaFX. |
| `DiagramPrinter.cs` + PageSetup/Print/PrintPreview del WinForms | — (ELIMINADO a petición del usuario) | El menú File ya no ofrece Page/Print/Print Preview (antes fase 3, `PrinterJob`). |
| `Options.cs` | `Options.kt` (fase 4) | Parseo 1:1 de la línea de comandos (`-o/-os/-r/-e/-d/-z/-y/-u/-p/-f/-a/-no-gui`...). |
| `Settings.cs` (+ `Properties\Settings.Designer.cs`) | `app/Settings.kt` (fase 4) | Persistencia con `java.util.prefs.Preferences` (nodo `xsddiagram/settings`). |
| `MRUManager.cs` | `app/MRUManager.kt` (fase 4) | Comportamiento 1:1 del MRU. |
| `SettingsForm.cs` / `SettingsForm.Designer.cs` | `app/SettingsForm.kt` (fase 4; formulario desde fase 5) | Formulario `GridPane` (antes tabla Property/Value); añade el campo "Tema" del port. |
| `validateXMLFileToolStripMenuItem_Click` (en `MainForm.cs`) | `app/MainForm.kt` `validateXMLFile()` + `validateXmlCore()` (fase 4) | Validación con `javax.xml.validation` del JDK. |

### Simplificaciones deliberadas estructurales
- Menús Windows Explorer Registration / Generate Sample / Infer XSD from XML
  del original omitidos: registro de extensiones de Windows y web services
  que no existen/en desuso; no aplican en un port multiplataforma.
- La línea de comandos se parsea desde la fase 4 (`Options`), pero su modo
  batch (`-o`/`-os`/`-no-gui`: exportar sin abrir la GUI, `XSDDiagramConsole`)
  queda pendiente de la fase 5.

## Tests

```text
DomainSmokeTest  → carga, indexación, includes, expansión (base + extensión),
                   substitución, layout, hit-test, búsqueda, atributos heredados,
                   documentación multi-lenguaje.
RealSchemasTest  → esquemas reales del original: spring-beans 3.0, COLLADASchema 1.4.1,
                   domicoudin, etc.
OptionsTest      → parseo de la línea de comandos (fase 4).
```

Autotests en runtime: `-Dxsddiagramx.selftest=resize|maximize|scroll|export|settings|validate`.

## Licencia recomendada para este repositorio

Recomendación para este repo, por compatibilidad con el proyecto original:

- **GPL-2.0-or-later**

Esto mantiene el origen del proyecto, reconoce la obra derivada y reduce el riesgo de
incompatibilidades con el código original (GPL/LGPL/MS-PL). Si se decide una licencia distinta,
se debe revisar la compatibilidad con el proyecto original y, en caso necesario, pedir permiso
explícito a los autores.

## Atribución al proyecto original

- Repositorio original: https://github.com/dgis/xsddiagram
- Autor original: Regis Cosnier
- Contribuciones originales: Mathieu Malaterre, Paul Selormey, Edu Serna, TCH68k y demás autores citados en el `ReadMe.txt` del proyecto original.
- Este repositorio es una adaptación/port a Kotlin/JavaFX del proyecto original y no reemplaza ni sustituye su autoría original.

## Créditos extra

Este port se basa en el trabajo original de **Regis Cosnier** y otros contribuidores del
proyecto XSD Diagram. Cualquier modificación adicional realizada en este repositorio debe
mantener esta atribución y respetar las condiciones de la licencia original.

