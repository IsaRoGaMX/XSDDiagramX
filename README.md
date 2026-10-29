# XSDDiagramX — Port Kotlin/JavaFX de XSD Diagram (fases 1-4: dominio + GUI + exportación/impresión + Settings/MRU/CLI/validación)

Port en **Kotlin + JavaFX** llamado **XSDDiagramX**, de [XSD Diagram](https://github.com/dgis/xsddiagram)
(C#, WinForms, .NET 2.0, Regis Cosnier — GPL/LGPL/MS-PL).

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

**Fase 3 completada**: exportación (Save Diagram As: SVG/TXT/CSV/PNG/JPG) e
impresión (Page/Print/Print preview) con los ports de `DiagramSvgRenderer`,
`DiagramTxtRenderer`, `DiagramExporter` y `DiagramPrinter`; icono de
aplicación en title bar/taskbar (Dock de macOS) vía `Stage.icons`.

**Fase 4 completada**: Settings persistentes (`java.util.prefs`), SettingsForm
(el "PropertyGrid" del Settings), MRUManager persistente (Recent Files),
parseo de la línea de comandos (`Options`) y validación de XML
(Validate XML Files...).

## Ejecutar

Requiere JDK 17+ (probado con JDK 21). Sin Gradle instalado: usar el wrapper.

```bat
gradlew.bat run            :: abre la ventana GUI
gradlew.bat test           :: tests de dominio + Options
```

Línea de comandos (GUI): `gradlew.bat run --args="[-r root@ns]... [-e N] [-d] [-z N] input.xsd"` — carga el esquema al arrancar, añade los root elements y expande el nivel indicado (los flags de export `-o`/`-os` se parsean pero se ejecutan en la fase 5, modo consola).

Nota: el proyecto se detecta la plataforma (win/mac/linux) para las dependencias
JavaFX de Maven Central; no se usa ningún plugin de terceros.

## Estructura (mapeo C# → Kotlin)

```
PROYECTO ORIGINAL (C#)                      ESTE PORT (Kotlin)
xsddiagram-master\                          
├── XSDDiagrams\ (núcleo, LGPL/MS-PL)       \src\main\kotlin\xsddiagram\
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
│       │   + DiagramExporter/Printer .cs   │       ├── DiagramSvgRenderer.kt / DiagramTxtRenderer.kt (fase 3)
│       │                                   │       ├── DiagramExporter.kt / DiagramPrinter.kt (fase 3)
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
```

Documentación del original: `../xsddiagram-master/ReadMe.txt` y diagramas UML en
`../xsddiagram-master/XSDDiagrams/Diagrams/*.cd`.

## Qué se portó (fases 1-2) y qué queda

Portado con tests de comportamiento equivalentes a los casos de `Tests\issues\`:

| Original (C#) | Port (Kotlin) | Notas del port |
|---|---|---|
| `XMLSchema.*` (xsd.exe, 2443 líneas) | `parsing/XmlSchema.kt` | Sólo el subconjunto parseado/usado: sin facetas de simpleType (sólo base+lista), sin `key/keyref/notation`. |
| `XmlSerializer` (deserialización) | `parsing/XmlSchemaReader.kt` | Parser DOM del JDK: resuelve QNames via `lookupNamespaceURI` y `xml:lang` via `getAttributeNS`. **No hay dependencias de terceros.** |
| `Schema.cs` + XSDObject/XSDAttribute/XSDAttributeGroup | `parsing/Schema.kt` | `SchemaLoader`: import recursivo include/import, descarga HTTP con credenciales 401/403, strip de `<!DOCTYPE...>`, anti-ciclos, indexación `ns:type:name` y `ns:attribute:name`. Los errores van a `loadErrors` (no lanza). |
| `Diagram.cs` | `rendering/Diagram.kt` | Expansión perezosa, layout, hit-test, búsqueda, `RequestAnyElement` (callback), herencia (`extension`/`restriction`) y grupos de substitución. |
| `DiagramItem.cs` | `rendering/DiagramItem.kt` | Measure/Location recursivos y `HitTest` (con `HitTestResult` en lugar de `out`). |
| `System.Drawing.Point/Size/Rectangle/Font/Graphics` | `rendering/Geometry.kt` + `rendering/DrawingContext.kt` | KPoint/KSize/KRectangle/SizeF + `DrawingContext` (interface de medición) y `FontDescription` (equivale a Font en el modelo). El render concreto (JavaFX/SVG/TXT) implementa `DrawingContext`. |
| `DiagramGdiRenderer.cs` | `rendering/javafx/DiagramJavaFxRenderer.kt` | Formas element/type/group (polígonos de 6 y 8 puntos), glifos Sequence/Choice/All, herencia dashed + triángulo, flecha de referencia, hatch (≈ HatchBrush BackwardDiagonal con líneas), expand-button, texto centrado, documentación con wrap y ocurrencias con `∞`. `fillPolygon/strokePolygon` requieren arrays x[]/y[] separados (helpers). |
| `JavaFxDrawingContext` (nuevo) | `rendering/javafx/JavaFxDrawingContext.kt` | ≈ `Graphics.MeasureString` con `javafx.scene.text.Text.layoutBounds`. |
| `DiagramControl.cs` | `app/DiagramControl.kt` | `Canvas` con `paint(virtualPoint)`, `clientSize`, `clientRectangle`. |
| `DiagramControlContainer.cs` | `app/DiagramControlContainer.kt` | Scrollbars sobre tamaño virtual (`max = virtualSize - clientSize`, ≈ `Maximum - LargeChange` de WinForms), pan arrastrando, rueda (`deltaY * 3` ≈ `e.Delta` 120/notch), Ctrl+rueda = zoom, `scrollTo(center)`. |
| `MainForm.cs` + `MainForm.Designer.cs` | `app/MainForm.kt` | Menús File/Window/Help (Recent Files en memoria, fase 4 lo persiste), toolbar con texto en vez de iconos (los PNG van en Resources.resx), pestañas por .xsd con `ListView<String>` virtualizado (monoespaciada; el fallback `TextBox` del original sin `WebBrowser` — ver fix del freeze abajo), panel derecho = lista de elementos + tabs Attributes/Element + documentación, status bar con path, drag & drop, ErrorReport con `Alert`, OpenURL con `TextInputDialog`, carga asíncrona con diálogo de descargas (ver fixes). Comportamiento 1:1: hit-test del click, expand/collapse (botón +/- y Espacio/Enter), navegación con flechas, Supr para eliminar, zoom por combo (índice 8 = "100%", igual que `Settings.Default`) y Ctrl+rueda, alineación Top/Center/Bottom = Near/Center/Far, toggles Doc/Type/Box + Panel, búsqueda con contador de hits, Ctrl+0 y Ctrl+F. |
| `XMLSchema.facet` (xsd.exe) | `parsing/XmlSchema.kt` (`class facet`) | Las facetas de `xs:restriction` ya se portan reales (`kind` + `value`): alimenta el tab "Element" (enumerations) y la columna "Facets" de la GUI. |
| `DiagramRenderer.cs` | `rendering/DiagramRenderer.kt` (fase 3) | Base abstracta Begin/Render/End de los renderers de salida (TXT/SVG). |
| `DiagramTxtRenderer.cs` | `rendering/DiagramTxtRenderer.kt` (fase 3) | TXT (tabulado) y CSV (`isCSV`): cabecera con los `TextOutputFields` y filas PATH/NAME/TYPE/NAMESPACE/COMMENT/SEQ/LASTCHILD/XSDTYPE; atributos con `DisplayAttributes` + `Schema` (reverse de la lista ≈ original). |
| `DiagramSvgRenderer.cs` | `rendering/DiagramSvgRenderer.kt` (fase 3) | SVG texto: líneas de herencia/referencia/grupos, `clipPath xsddoc{n}` para la documentación con `WrapText` medido vía `DrawingContext` (el C# usaba el `Graphics` de referencia). |
| `DiagramExporter.cs` | `rendering/DiagramExporter.kt` (fase 3) | Export por extensión: PNG/JPG con canvas offscreen JavaFX + `snapshot` → `BufferedImage` → `ImageIO`; TXT/CSV/SVG con `scale=1.0` + re-layout + restore en `finally`. **`.emf` omitido** (metafile GDI+ de Windows; el original también lo saltaba en Mono). Alerta Yes/No para imágenes >10000px (≈ `SaveAlert`). |
| `DiagramPrinter.cs` + PageSetup/Print/PrintPreview del WinForms | `rendering/DiagramPrinter.kt` (fase 3) | `PrinterJob` (showPageSetupDialog/showPrintDialog); paginación port de `OnPrintPage` (`DivRem` columnas/filas, clip+translate). PrintPreview: ventana propia con ScrollPane de páginas "papel" + botón Print (`PrintPreviewDialog` no existe en JavaFX). Ver trade-off del escalado abajo. |
| `Options.cs` | `Options.kt` (fase 4) | Parseo 1:1 de la línea de comandos (`-o/-os/-r/-e/-d/-z/-y/-u/-p/-f/-a/-no-gui`, normalización `--opt:params` y `/opt params` → `-opt params`). Diferencias: `IsRunningOnMono` siempre `false`; `init()` se llama con `Application.getParameters().raw` (el C# usaba `Environment.GetCommandLineArgs()`, que incluye el ejecutable en `[0]`). El modo batch (¿qué hacer con `-o`/`-os`?) queda para la fase 5. |
| `Settings.cs` (+ `Properties\Settings.Designer.cs`) | `app/Settings.kt` (fase 4) | Persistencia con `java.util.prefs.Preferences` (nodo `xsddiagram/settings`; en Windows `HKCU\Software\JavaSoft\Prefs\xsddiagram\settings`) en vez de `user.config`. Las 7 propiedades del C#: DisplayPanel/Zoom/Alignement/ShowDocumentation (Browsable(false), se persisten desde la toolbar) + AlwaysShowOccurence/ShowType/CompactLayoutDensity (Browsable, editables en el SettingsForm). Escritura `putX` inmediata por propiedad + `flush()` en `save()` (≈ `MainForm_FormClosed` → `Settings.Default.Save()`, que corre en `Application.stop()`). |
| `MRUManager.cs` | `app/MRUManager.kt` (fase 4) | Comportamiento 1:1: `AddRecentFile` sólo añade si no estaba (no reordena), claves numéricas "0","1",... y menú con "Clear list". Diferencia: almacén en Preferences (`xsddiagram/MRU`) en lugar del registro de Windows (`HKCU\Software\xsddiagram\MRU`). El callback de clear queda vacío en MainForm, igual que el original. |
| `SettingsForm.cs` / `SettingsForm.Designer.cs` | `app/SettingsForm.kt` (fase 4) | El PropertyGrid de WinForms no existe en JavaFX: ventana no modal (centrada sobre el owner) con tabla Property/Value que lista sólo las 3 propiedades Browsable del Settings (siempre las mismas que mostraba el grid del C#). El checkbox por celda (`mouseTransparent` + toggle en el clic de la celda) hace que 1 clic en cualquier punto de la columna Value conmute (≈ la edición del PropertyGrid). Cada cambio → `Settings.setX` + `MainForm.changeSetting(name)` (≈ `propertyGrid1_PropertyValueChanged`). |
| `validateXMLFileToolStripMenuItem_Click` (en `MainForm.cs`) | `app/MainForm.kt` `validateXMLFile()` + `validateXmlCore()` (fase 4) | Validación con `javax.xml.validation` del JDK: `newSchema(sources)` sobre **todos** los .xsd cargados en orden inverso (el C# añadía por fichero — simplificación: aquí un .xsd mal formado aborta el conjunto en una sola llamada), la validación acumula los mensajes en `validationErrorMessages` y los reporta con `showErrorReport` (el `ValidationEventHandler` portado como ErrorHandler SAX con línea). Núcleo extraído a `validateXmlCore()` para el autotest. Flags de `XmlReaderSettings` (ProcessInlineSchema/ProcessSchemaLocation/…) dependen del parser del JDK. |

### Simplificaciones deliberadas estructurales
- Menús Windows Explorer Registration / Generate Sample / Infer XSD from XML
  del original omitidos: registro de extensiones de Windows y web services
  que no existen/en desuso; no aplican en un port multiplataforma.
- La línea de comandos se parsea desde la fase 4 (`Options`), pero su modo
  batch (`-o`/`-os`/`-no-gui`: exportar sin abrir la GUI, `XSDDiagramConsole`)
  queda pendiente de la fase 5. La GUI sí respeta `inputFile`/`-r`/`-e`
  (≈ `MainForm_Load`: `XsdDiagramApp.start()` llama a `Options.init` y
  `showOn` carga el esquema + root elements + expand level).

### Fixes de la fase 2 (sesión de pulido, validados por el usuario)

- **Carga asíncrona + diálogo de descargas**: `MainForm.loadSchema` corre en un
  hilo de fondo con `SchemaDownloadDialog` (`APPLICATION_MODAL`, botón
  *Cancelar*). `SchemaLoader` incorpora `progressListener` (`onDownloadStart`/
  `onDownloadEnd`) + `requestCancel()` + `SchemaLoadCancelledException`; cancelar
  descarta el estado parcial (`schema.cleanup()`). El diálogo se cierra al final,
  cuando la ventana ya está poblada (la lectura de cada .xsd para las pestañas
  también corre en el hilo de fondo).
- **Freeze al redimensionar/maximizar (causa raíz)**: el `TextArea` de las
  pestañas re-medía el documento completo en cada `notifyResize`
  (`TextAreaSkin.computePrefWidth`, ~10 s con `cfdv40.xsd`) → "no responde" y la
  ventana sin cambiar de tamaño. Sustituido por `ListView<String>` virtualizado
  (monoespaciada). Además: caché de fuentes (`JavaFxFonts`), caché de mediciones
  (`TextMeasure`), repintado coalescido (`runLater`) para scroll/pan y **debounce
  150 ms** (`PauseTransition`) para los repaints impulsados por el resize.
- **Scrollbars**: estilo propio (`resources/xsddiagram/app/scrollbar.css`,
  track/thumb visibles) y `visibleAmount = max*client/virtual` para que el thumb
  represente la porción visible (≈ el thumb automático de WinForms); pan por
  arrastre con límites inclusivos y cursor **mano cerrada** solo durante el
  arrastre real (`Cursor.CLOSED_HAND` desde el primer `MOUSE_DRAGGED`, no en
  `MOUSE_PRESSED`, para no marcar los clics de selección de nodos; vuelve al
  cursor normal al soltar).
- **Rueda del mouse**: clamp explícito del destino
  `(value - deltaY*3).coerceIn(min, max)` — JavaFX no garante el clamp en la
  asignación directa a `ScrollBar.value`, y sin esto la rueda podía llevar el
  desplazamiento fuera de rango (thumb sin actualizar y zonas donde el diagrama
  deja de ser visible). Verificado inyectando eventos Win32
  `MOUSEEVENTF_WHEEL` reales → `[WHEELDBG]` con `-Dxsddiagramx.fxwatch=1`.
- **Tab "Element" (selectores de atributos)**: faltaba el port de
  `listViewAttributes_SelectedIndexChanged` — al seleccionar un atributo en el
  tab "Attributes", su documentación va al box de anotaciones y el tab
  "Element" se llena con las enumeraciones de su simpleType
  (`AttributesRow.tag` guarda el `XSDAttribute` ≈ `ListViewItem.Tag`).
- **PropertyGrid del tab "Element"** (≈ `propertyGridSchemaObject`): tabla
  Property/Value con las propiedades del objeto del esquema seleccionado
  (por reflexión sobre las clases wrapper, orden alfabético ≈
  `PropertySort.Alphabetical`; los objetos anidados se muestran por su tipo
  "XMLSchema.*"). Nota: lista solo las propiedades que las clases wrapper del
  port conservan (las funcionales); el C# serializaba unas pocas más sin uso
  (`form`, `final`, `id`...). `listViewEnumerate` quedó como panel separado
  debajo de los tabs (≈ `Dock=Bottom` del C#).
- **Herramientas de diagnóstico**: `app/FxWatchdog.kt` (detección de freezes del
  FX Thread midiendo el round-trip de `Platform.runLater` + dump de hilos a
  `%TEMP%\opencode\xsddiagram-fxwatch.log` con `-Dxsddiagramx.fxwatch=1`); modos
  de autotest (`-Dxsddiagramx.selftest=resize|maximize|scroll` + `.file=...`).

### Fase 3 (exportación + impresión + icono, validada con autotest)

- **Icono de aplicación**: JavaFX no lee `.ico`; inicialmente se usaron PNG
  (256/64/32) generados del `XSDDiagram.ico` original (`Stage.icons` en orden
  descendente → title bar/taskbar Windows/Linux, Dock macOS). En el pulido
  final se sustituyeron por un único `xsddiagram.png` (512×512) a petición del
  usuario. Capturas: `../screenshots/12-titlebar-icon.png` y
  `../screenshots/22-new-icon-titlebar.png`.
- **Save Diagram As**: `FileChooser` con los filtros del `SaveFileDialog`
  (SVG/PNG/JPG/TXT/CSV/ALL), export sobre el hilo FX (layout/snapshot/medición),
  errores mapeados del C# (`ArgumentException` → "You have reach the system
  limit...", resto → mensaje).
- **Page/Print preview/Print**: habilitados con `isSchemaLoaded` igual que el
  original (Page no se deshabilita, fiel a `UpdateActionsState`).
- **Trade-off de impresión**: el área imprimible de JavaFX viene en puntos
  (1/72") y se trata como los "centésimos de pulgada" (1/100") del C# → el
  diagrama queda a ~72/100 del tamaño del original impreso. Compromiso
  documentado en `DiagramPrinter.kt`.
- **Autotest** (`-Dxsddiagramx.selftest=export` + `.file=`): Add all + expand +
  export SVG/TXT/PNG a `%TEMP%\opencode\`; validado con
  `spring-beans-3.0.xsd` (svg=101 KB, txt=12 KB, png=76 KB con el árbol completo).

### Fase 4 (Settings + MRU + línea de comandos + validación XML, validada)

- **Bugfix scrollbars al encoger la ventana**: el `Canvas` (no-redimensionable)
  reportaba como mínimo su tamaño actual, así que al ENCOGER la ventana el
  `canvasHolder` no reducía su tamaño, no disparaba los listeners de
  `canvasHolder.width/height` y `layoutScrollBars()` nunca se re-ejecutaba →
  las scrollbars desaparecían aunque el diagrama ya no cupiera (el contenido
  quedaba cortado). Fix: `canvasHolder.minWidth/minHeight = 0`. Prueba con
  `cfdv40.xsd`: ventana 1040×807 → 525×400 y las scrollbars se re-dimensionan
  correctamente (`h.max=707`, `v.max=571` con virtual 939×795 y client
  232×224). Capturas: `../screenshots/25-scrollbars-shrink.png` (bug) y
  `26-scrollbars-fixed.png` / `26b-vscroll-zoom.png` (fix).

- **Persistencia con `java.util.prefs`**: los Settings del C# (`user.config`)
  y el MRU (registro `HKCU\Software\xsddiagram\MRU`) viven ahora en los nodos
  Preferences `xsddiagram/settings` y `xsddiagram/MRU`, con el mismo esquema
  de claves numéricas del MRU. Toggle probado end-to-end (clic en el
  SettingsForm → WM_CLOSE → `/Show/Type` en el registro). El flush ocurre en
  `Application.stop()` (≈ `MainForm_FormClosed` → `Settings.Default.Save()`).
  Diferencia: el registro HKCU sólo refleja tras cerrar la app; los `putX`
  son inmediatos en el nodo Preferences.
- **Menús fase 4**: `Validate _XML File...` después de "Open URL..." y menú
  `_Tools` (entre Window y Help) con `_Settings...`; el ítem de validación se
  deshabilita sin esquema cargado (`UpdateActionsState`); Tools/Settings no
  (fiel al original). El registro en Windows Explorer del C# se omite.
- **Settings aplicados al arrancar** (≈ ctor del C# líneas 91-94 +
  `MainForm_Load` 184-188): `showDocumentation = Options.showDocumentation ||
  Settings.showDocumentation`; zoom/align/panel leen `Settings` en vez de los
  índices codificados de la fase 2; los toggles/combos persisten en cada
  cambio (zoom ≈ línea 936 del C#, panel ≈ 1031, align ≈ 1198, doc ≈ 1745).
- **Línea de comandos (GUI)**: `Options.init(parameters.raw)` en `start()`;
  `showOn` carga `Options.inputFile` + los `-r root@ns` (búsqueda por
  elementsByName) + `-e expandLevel` después de `stage.show()` (≈ la lógica
  de `MainForm_Load` 192-223 del C#).
- **Validación XML**: `javax.xml.validation` con los .xsd cargados en orden
  inverso compilados en una sola `newSchema(sources)` (el C# añadía
  `Schemas.Add` por fichero; aquí un .xsd mal formado aborta el conjunto —
  simplificación documentada); mensajes del ErrorHandler SAX ("Validation
  Error: cvc-... (line N)") reportados via `showErrorReport`, fiel al C#.
  Núcleo extraído a `validateXmlCore()` para el autotest
  `-Dxsddiagramx.selftest=validate` + `.file=`: XML válido → 0 mensajes; XML
  inválido → 1 mensaje `cvc-complex-type.2.4.a` (línea 4). ✓
- **SettingsForm**: tabla Property/Value con las 3 propiedades Browsable
  (AlwaysShowOccurence/ShowType/CompactLayoutDensity); el checkbox es
  `mouseTransparent` y el toggle lo hace la celda — 1 clic en cualquier
  punto de la columna Value conmuta (≈ PropertyGrid; diagnosticado: el
  `CheckBoxTableCell` exige tabla editable y sin `cellValueFactory` el item
  de la celda es null). Cambios en vivo vía `MainForm.changeSetting` (≈
  `ChangeSetting`). Autotest `-Dxsddiagramx.selftest=settings` confirma el
  roundtrip y el wiring. Capturas: `../screenshots/14-tools-menu.png`,
  `15-settings-form.png`, `21-settings-celltoggle.png`.
- **Toggle panel lateral redimensionable** (pulido final, a petición del
  usuario): la lista de elementos y el panel inferior van en un `SplitPane`
  vertical (antes la `VBox` fijaba la altura de la lista sin poder agrandarla;
  se conservan los 2 divisores internos de tabs/enumeraciones/documentación).
- **Icono de aplicación**: PNGs del .ico reemplazados por el
  `xsddiagram.png` (512×512) en `resources/xsddiagram/app/`.
- **Test unitario**: `OptionsTest` valida el parseo
  (`-e 3 -d -z 150 -f PATH,NAME -a --o:out.png --no-gui input.xsd`, 8
  aserciones).

**Pendiente para la fase 5** (se apuntan junto con sus ficheros originales):

- Modo consola/batch (`Program.cs`/`XSDDiagramConsole`): exportar con `-o`/
  `-os` sin abrir la GUI (el parseo de `Options` ya está; falta el flujo
  batch: loadSchema + Diagram + export a fichero/stdout + usage).

## Decisiones/diferencias respecto al original (deliberadas)

1. **Windows + lower-case C#**: el C# tenía `<class Schema>` (cargador) y `XMLSchema.schema`
   (modelo) en namespaces distintos. En Kotlin ambos viven en `xsddiagram.parsing`, y en NTFS
   (case-insensitive) colisionarían (`Schema.class` vs `schema.class`). El cargador se
   renombró a `SchemaLoader` y hay un `typealias Schema = SchemaLoader` para que las
   siguientes fases usen el nombre original.
2. **`element.abstract`** es reservado en Kotlin → `element.isAbstract`.
3. **`import` de xs:import** es reservado en Kotlin → `schemaImport`.
4. Extensiones de contenido simple/complex comparten tipos (extensionType /
   restrictionType), igual que en C#.
5. **No se necesitan dependencias de terceros**: XML = `javax.xml.parsers`, HTTP = `java.net`,
   tests = `kotlin.test`. Todo el resto de código usado es del JDK o JavaFX (licencias libres).
6. Igual que el original, `TrustAllCerts` confia en cualquier certificado TLS. Marca FIXME
(security) en el código: es un port 1:1 de una decisión de 2006, revisar en fase 3.

## Tests

```text
DomainSmokeTest  → carga, indexación, includes, expansión (base + extensión),
                   substitución, layout, hit-test, búsqueda, atributos heredados,
                   documentación multi-lenguaje.
RealSchemasTest  → esquemas reales del original: spring-beans 3.0 (github issue 2),
                   COLLADASchema 1.4.1, domicoudin (github issue 15).
OptionsTest      → parseo de la línea de comandos (fase 4).
```

Autotests en runtime (además de la suite): `-Dxsddiagramx.selftest=resize|maximize|scroll|export|settings|validate` (+ `.file=` para cargar un esquema).
