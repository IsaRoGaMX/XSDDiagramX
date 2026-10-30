//    XSD Diagram - Kotlin/JavaFX port
//    Port de MainForm.cs (ventana principal del C#, 1.814 líneas) + los eventos
//    esenciales del Designer. Voluntariamente simplificado:
//      - Sin Settings persistente (fase 4) ni el menú Tools de Windows
//        Explorer: SettingsForm/Options queda para la fase 4.
//      - La toolbar usa texto en lugar de iconos (los PNG van en Resources.resx
//        del proyecto C#).
//      - La carga de esquema es asíncrona (diálogo de progreso + Cancelar).
//
//    Licencias del original: LGPL (MainForm) / MS-PL.
//    Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización),
//    Edu Serna (búsqueda)
package xsddiagramx.app

import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.Cursor
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.control.Separator
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.control.SplitPane
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.TextInputDialog
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToolBar
import javafx.scene.control.Tooltip
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DragEvent
import javafx.scene.input.KeyEvent
import javafx.scene.input.TransferMode
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import xsddiagramx.DiagramHelpers
import xsddiagramx.Options
import xsddiagramx.parsing.Schema
import xsddiagramx.parsing.SchemaLoader
import xsddiagramx.parsing.XSDAttribute
import xsddiagramx.parsing.XSDObject
import xsddiagramx.rendering.Diagram
import xsddiagramx.rendering.DiagramAlignement
import xsddiagramx.rendering.DiagramExporter
import xsddiagramx.rendering.DiagramHitTestRegion
import xsddiagramx.rendering.DiagramItem
import xsddiagramx.rendering.DiagramItemType
import xsddiagramx.rendering.DiagramPrinter
import xsddiagramx.rendering.KPoint
import xsddiagramx.rendering.KSize
import xsddiagramx.rendering.javafx.JavaFxDrawingContext
import xsddiagramx.tools.IconResources
import java.io.File
import java.net.URL
import javax.swing.Icon
import kotlin.math.max

// ---------------------------------------------------------------------------
// Filas de las tablas de la GUI (ListViewItem del C# era un string[])
// ---------------------------------------------------------------------------

/** Fila de la lista de elementos globales (listViewElements). */
data class ElementsRow(val xsdObject: XSDObject) {
    val name: String get() = xsdObject.name
    val type: String get() = xsdObject.type
    val nameSpace: String get() = xsdObject.nameSpace
}

/** Fila de la tabla de atributos (listViewAttributes). */
data class AttributesRow(
    val name: String,
    val type: String,
    val use: String,
    val defaultValue: String,
    val facets: String,
    /** XSDAttribute envelvente (≈ ListViewItem.Tag del C#: los handlers lo usan). */
    val tag: XSDAttribute? = null,
)

/** Fila del PropertyGrid del esquema (≈ propertyGridSchemaObject, tab "Element"). */
data class PropertyRow(val name: String, val value: String)

/** MainForm principal. */
class MainForm : BorderPane() {

    companion object {
        /** Referencia para los autotests (selftest de resize). */
        var lastForm: MainForm? = null
    }

    init {
        lastForm = this
    }

    // ------------------------------------------------------------------
    // Estado (≈ campos privados del C#)
    // ------------------------------------------------------------------

    private val originalTitle = "XSDDiagramX"
    private var currentStage: Stage? = null

    private val schema = Schema() // ≈ this.schema (typealias Schema = SchemaLoader)
    private val diagram = Diagram()
    private val panelDiagram = DiagramControlContainer()

    private lateinit var menuBar: MenuBar
    private lateinit var toolBar: ToolBar
    private lateinit var tabControlView: TabPane
    private lateinit var statusLabel: Label
    private lateinit var textBoxElementPath: TextField
    private lateinit var sidePanel: VBox
    private lateinit var mainSplit: SplitPane

    private lateinit var toolStripComboBoxSchemaElement: ComboBox<XSDObject>
    private lateinit var toolStripComboBoxZoom: ComboBox<String>
    private lateinit var toolStripComboBoxAlignement: ComboBox<String>
    private lateinit var toolStripTextBoxSearch: TextField
    private lateinit var toolStripButtonAddToDiagram: Button
    private lateinit var toolStripButtonAddAllToDiagram: Button
    private lateinit var toolStripButtonRemoveAllFromDiagram: Button
    private lateinit var toolStripButtonExpandOneLevel: Button
    private lateinit var toolStripButtonShowDocumentation: ToggleButton
    //private lateinit var toolStripButtonShowType: ToggleButton
    //private lateinit var toolStripButtonShowReferenceBoundingBox: ToggleButton
    //private lateinit var toolStripButtonTogglePanel: ToggleButton
    private lateinit var closeMenuItem: MenuItem
    private lateinit var saveDiagramMenuItem: MenuItem
    private lateinit var printPreviewMenuItem: MenuItem
    private lateinit var printMenuItem: MenuItem
    private lateinit var validateXMLMenuItem: MenuItem
    private lateinit var recentFilesMenu: Menu

    private lateinit var listViewElements: TableView<ElementsRow>
    private lateinit var listViewAttributes: TableView<AttributesRow>
    private lateinit var gridSchemaObject: TableView<PropertyRow>
    private lateinit var listViewEnumerate: ListView<String>
    private lateinit var tabControlElement: TabPane
    private lateinit var textBoxAnnotation: TextArea

    private val zoomValues = arrayOf(
        "10%", "50%", "60%", "75%", "80%", "85%", "90%", "95%", "100%", "105%",
        "110%", "115%", "120%", "125%", "140%", "150%", "175%", "200%", "300%"
    )

    init {
        buildMenu()
        buildToolbar()
        buildMainArea()
        buildStatusBar()

        // ≈ diagram_RequestAnyElement del C#: no añade elemento (sin ElementsForm).
        diagram.requestAnyElement = { null }

        // La lógica del C# pintaba "panelDiagram.DiagramControl" con this.diagram;
        // en JavaFX hay que conectar explícitamente el diagrama al canvas.
        panelDiagram.diagramControl.diagram = diagram

        // Conectar la interacción del canvas (≈ DiagramControl_MouseClick, etc).
        hookCanvasEvents()

        // ≈ MainForm_KeyUp: Ctrl+0 / Ctrl+F (KeyUp del WinForms en la ventana).
        addEventFilter(KeyEvent.KEY_RELEASED) { event -> mainForm_KeyUp(event) }

        // Bordes (≈ DockStyle): arriba menú+toolbar, abajo status bar.
        top = VBox(menuBar, toolBar)
    }

    // ------------------------------------------------------------------
    // Menús (≈ MainForm.Designer.cs)
    // ------------------------------------------------------------------

    private fun buildMenu() {
        recentFilesMenu = Menu("Recent Files")

        val fileMenu = Menu("_File")
        fileMenu.items.addAll(
            MenuItem("_Open...").apply { setOnAction { openSchema() } },
            MenuItem("Open _URL...").apply { setOnAction { openSchemaUrl() } },
            // ≈ validateXMLFileToolStripMenuItem del C# (validation con
            // javax.xml.validation; ver validateXMLFile()).
            MenuItem("Validate _XML File...").apply {
                setOnAction { validateXMLFile() }
            }.also { validateXMLMenuItem = it },
            recentFilesMenu,
            SeparatorMenuItem(),
            MenuItem("_Close").apply {
                setOnAction { cleanupUserInterface(true) }
            }.also { closeMenuItem = it },
            SeparatorMenuItem(),
            MenuItem("Save Diagram _As...").apply {
                setOnAction { saveDiagramAs() }
            }.also { saveDiagramMenuItem = it },
            SeparatorMenuItem(),
            // ≈ pageToolStripMenuItem / printPreviewToolStripMenuItem /
            //   printToolStripMenuItem del C# (el PageSetup no se deshabilita
            //   en UpdateActionsState del original).
            MenuItem("_Page...").apply {
                setOnAction { printPageSetup() }
            },
            MenuItem("Print Pre_view...").apply {
                setOnAction { printDiagramPreview() }
            }.also { printPreviewMenuItem = it },
            MenuItem("_Print...").apply {
                setOnAction { printDiagram(true) }
            }.also { printMenuItem = it },
            SeparatorMenuItem(),
            MenuItem("E_xit").apply { setOnAction { Platform.exit() } }
        )

        val windowMenu = Menu("_Window")
        windowMenu.items.addAll(
            MenuItem("Next _Tab").apply { setOnAction { cycleTab(+1) } },
            MenuItem("Previous _Tab").apply { setOnAction { cycleTab(-1) } }
        )

        // ≈ toolsToolStripMenuItem del C# (los ítems de registro en el Windows
        // Explorer se omiten: no aplican fuera de Windows).
        val toolsMenu = Menu("_Tools")
        toolsMenu.items.add(
            MenuItem("_Settings...").apply { setOnAction { showSettingsForm() } }
        )

        val helpMenu = Menu("_Help")
        helpMenu.items.addAll(
            MenuItem("_About XSDDiagramX").apply { setOnAction { showAbout() } }
        )

        menuBar = MenuBar(fileMenu, toolsMenu, windowMenu, helpMenu)
    }

    /** ≈ mruManager (crear en showOn; los ítems van bajo "Recent Files"). */
    private var mruManager: MRUManager? = null

    private fun addRecentFile(filenameOrUrl: String) {
        mruManager?.addRecentFile(filenameOrUrl)
    }

    // ------------------------------------------------------------------
    // Barra de herramientas (con texto en vez de los iconos del C#)
    // ------------------------------------------------------------------

    private fun buildToolbar() {
        val openButton = IconResources.toolbarButton("16px/folder_page_white.png", "Open a schema file") { openSchema() }

//        val openButton = Button("Open").apply {
//            tooltip = Tooltip("Open a schema file")
//            setOnAction { openSchema() }
//        }

        toolStripComboBoxSchemaElement = ComboBox<XSDObject>().apply {
            prefWidth = 250.0
            setOnAction {
                // ≈ toolStripComboBoxSchemaElement_SelectedIndexChanged: también
                // se dispara (con null) al deseleccionar desde el canvas.
                val xsdObject = selectionModel.selectedItem ?: return@setOnAction
                selectSchemaElement(xsdObject)
            }
        }

        toolStripButtonAddToDiagram = Button().apply {
            tooltip = Tooltip("Add the selected element to the diagram")
            graphic = IconResources.graphic("16px/add.png", 16.0)
            isDisable = true
            setOnAction {
                addToDiagram(toolStripComboBoxSchemaElement.selectionModel.selectedItem)
            }
        }

        toolStripButtonAddAllToDiagram = Button().apply {
            tooltip = Tooltip("Add all the elements to the diagram")
            graphic = IconResources.graphic("16px/chart_organisation_add.png", 16.0)
            isDisable = true
            setOnAction { addAllToDiagram() }
        }
        toolStripButtonRemoveAllFromDiagram = Button().apply {
            tooltip = Tooltip("Remove all the elements from the diagram")
            graphic = IconResources.graphic("16px/cross.png", 16.0)
            isDisable = true
            setOnAction { removeAllFromDiagram() }
        }

        toolStripButtonExpandOneLevel = Button().apply {
            tooltip = Tooltip("Expand all the elements one level")
            graphic = IconResources.graphic("16px/toggle_expand.png", 16.0)
            isDisable = true
            setOnAction {
                if (diagram.expandOneLevel()) updateDiagram()
            }
        }

        toolStripComboBoxZoom = ComboBox(FXCollections.observableArrayList(*zoomValues)).apply {
            prefWidth = 80.0
            tooltip = Tooltip("Zoom (Ctrl+rueda sobre el diagrama)")
            setOnAction { zoom_SelectedIndexChanged() }
        }

        toolStripComboBoxAlignement = ComboBox(FXCollections.observableArrayList("Top", "Center", "Bottom")).apply {
            prefWidth = 90.0
            tooltip = Tooltip("Alignment")
            setOnAction {
                // ≈ toolStripComboBoxAlignement_SelectedIndexChanged.
                when (selectionModel.selectedItem) {
                    "Top" -> diagram.alignement = DiagramAlignement.Near
                    "Center" -> diagram.alignement = DiagramAlignement.Center
                    "Bottom" -> diagram.alignement = DiagramAlignement.Far
                }
                // ≈ cs line 1198: Settings.Default.Alignement = SelectedIndex.
                val selectedIndex = selectionModel.selectedIndex
                if (selectedIndex >= 0) Settings.alignement = selectedIndex
                updateDiagram()
            }
        }

        toolStripButtonShowDocumentation = ToggleButton().apply {
            tooltip = Tooltip("Show the documentation")
            graphic = IconResources.graphic("16px/document_inspector.png", 16.0)
            setOnAction {
                diagram.showDocumentation = isSelected
                // ≈ cs line 1745: Settings.Default.ShowDocumentation = Checked.
                Settings.showDocumentation = isSelected
                updateDiagram()
            }
        }

//        toolStripButtonShowType = ToggleButton("Type").apply {
//            tooltip = Tooltip("Show the type of the node")
//            setOnAction {
//                diagram.showType = isSelected
//                updateDiagram()
//            }
//        }

//        toolStripButtonShowReferenceBoundingBox = ToggleButton("Box").apply {
//            tooltip = Tooltip("Show the reference bounding box")
//            setOnAction {
//                diagram.showBoundingBox = isSelected
//                updateDiagram()
//            }
//        }

//        toolStripButtonTogglePanel = ToggleButton().apply {
//            tooltip = Tooltip("Show or hide the schema panel")
//            //graphic = IconResources.graphic(if(isSelected) "16px/application_side_expand.png" else "16px/application_side_contract.png", 16.0)
//            isSelected = true
//            setOnAction {
//                if (isSelected) showSidePanel() else hideSidePanel()
//                // ≈ cs line 1031: Settings.Default.DisplayPanel = Checked.
//                Settings.displayPanel = isSelected
//                graphic = IconResources.graphic(if(isSelected) "16px/application_side_expand.png" else "16px/application_side_contract.png", 16.0)
//            }
//        }

        toolStripTextBoxSearch = TextField().apply {
            promptText = "Text to find"
            prefWidth = 180.0
            setOnAction { search() }
        }

        val searchButton = Button().apply {
            tooltip = Tooltip("Find text (Ctrl+F)")
            graphic = IconResources.graphic("16px/find.png", 16.0)
            setOnAction { search() }
        }

        toolBar = ToolBar(
            openButton, Label(" "),
            toolStripComboBoxSchemaElement,
            toolStripButtonAddToDiagram, toolStripButtonAddAllToDiagram,
            toolStripButtonRemoveAllFromDiagram, toolStripButtonExpandOneLevel,
            Separator(), Label(" Zoom "), toolStripComboBoxZoom,
            Label(" Align "), toolStripComboBoxAlignement,
            Separator(),
            toolStripButtonShowDocumentation, /*toolStripButtonShowType,*/
            /*toolStripButtonShowReferenceBoundingBox, toolStripButtonTogglePanel,*/
            Separator(),
            toolStripTextBoxSearch, searchButton
        )
    }

    // ------------------------------------------------------------------
    // Área principal (≈ splitContainerMain + splitContainerDiagramElement)
    // ------------------------------------------------------------------

    private fun buildMainArea() {
        // Panel izquierdo: pestaña "Diagram" + una pestaña por fichero .xsd.
        tabControlView = TabPane().apply {
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
        }
        tabControlView.tabs.add(Tab("Diagram", panelDiagram))

        // Panel derecho: elementos + tabs Attributes/Element + documentación.
        listViewElements = TableView<ElementsRow>().apply {
            columns.addAll(
                stringColumn("Name", 120.0) { it.name },
                stringColumn("Type", 80.0) { it.type },
                stringColumn("Namespace", 100.0) { it.nameSpace }
            )
            setOnMouseClicked { event ->
                val selected = selectionModel.selectedItem ?: return@setOnMouseClicked
                // ≈ listViewElement_Click / listViewElement_DoubleClick.
                if (event.clickCount == 2) {
                    addToDiagram(selected.xsdObject)
                } else {
                    selectSchemaElement(selected.xsdObject)
                }
            }
        }

        listViewAttributes = TableView<AttributesRow>().apply {
            columns.addAll(
                stringColumn("Name", 110.0) { it.name },
                stringColumn("Type", 110.0) { it.type },
                stringColumn("Default", 70.0) { it.defaultValue },
                stringColumn("Use", 70.0) { it.use },
                stringColumn("Facets", 110.0) { it.facets }
            )
            // ≈ listViewAttributes_SelectedIndexChanged: al seleccionar un
            // atributo se muestra su documentación y el tab "Element" se llena
            // con las enumeraciones de su simpleType.
            selectionModel.selectedItemProperty().addListener { _, _, row ->
                if (row != null) {
                    // row.tag = XSDAttribute; tag.tag = attribute del parsing (≈ .Tag del C#).
                    val attribute = row.tag?.tag
                    if (attribute?.annotation != null) {
                        showDocumentation(attribute.annotation)
                    } else {
                        showDocumentation(null)
                    }
                    showEnumerateOfAttribute(attribute)
                }
            }
        }

        listViewEnumerate = ListView<String>()

        // ≈ propertyGridSchemaObject (tab "Element" del C#): tabla
        // nombre/valor con las propiedades del objeto del esquema
        // seleccionado, orden alfabético (≈ PropertySort.Alphabetical).
        gridSchemaObject = TableView<PropertyRow>().apply {
            columns.addAll(
                stringColumn("Property", 110.0) { it.name },
                stringColumn("Value", 110.0) { it.value }
            )
            isEditable = false
        }

        // Campo para los ganchos de autotest (seleccionar el tab "Element").
        tabControlElement = TabPane().apply {
            // ≈ tabPageElementAttibutes ("Attributes") y tabPageElement
            // ("Element"; su contenido es el propertyGridSchemaObject).
            // listViewEnumerate va aparte, debajo de los tabs (Dock=Bottom en
            // el C#), como lista de enumeraciones.
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
            tabs.addAll(
                Tab("Attributes", listViewAttributes),
                Tab("Element", gridSchemaObject)
            )
        }

        textBoxAnnotation = TextArea().apply {
            isEditable = false
            isWrapText = true
        }

        val sideSplit = SplitPane(tabControlElement, listViewEnumerate, textBoxAnnotation).apply {
            orientation = Orientation.VERTICAL
            // ≈ tabs ~153/525 arriba, listViewEnumerate 91/525 abajo (Docks del C#).
            setDividerPosition(0, 0.58)
            setDividerPosition(1, 0.80)
        }

        // Panel superior de la zona lateral: lista de elementos globales y
        // panel inferior en un SplitPane vertical (≈ DockStyle.Fill del C#).
        // Antes iba en una VBox fija y la lista quedaba con altura constante,
        // sin forma de agrandarla cuando el esquema lo pedía.
        val sideVertical = SplitPane(listViewElements, sideSplit).apply {
            orientation = Orientation.VERTICAL
            // ≈ proporción visual del C# (listViewElements arriba, resto abajo).
            setDividerPosition(0, 0.30)
            // Alturas mínimas para que ningún bloque colapse del todo.
            listViewElements.minHeight = 48.0
            sideSplit.minHeight = 120.0
        }

        sidePanel = VBox(4.0).apply {
            padding = Insets(4.0)
            VBox.setVgrow(sideVertical, Priority.ALWAYS)
            children.add(sideVertical)
        }

        mainSplit = SplitPane(tabControlView, sidePanel).apply {
            // ≈ SplitterDistance 658/876 ≈ 0.75 del C#.
            setDividerPosition(0, 0.75)
            SplitPane.setResizableWithParent(sidePanel, false)
        }

        center = mainSplit

        // ≈ MainForm_DragEnter / MainForm_DragDrop (arrastrar ficheros/URLs).
        setupDragAndDrop()
    }

    /** ≈ ListView con ColumnHeader: propiedad string. */
    private fun <T> stringColumn(header: String, width: Double, value: (T) -> String): TableColumn<T, String> {
        val tableColumn = TableColumn<T, String>(header)
        tableColumn.prefWidth = width
        tableColumn.setCellValueFactory { row -> SimpleStringProperty(value(row.value)) }
        return tableColumn
    }

    private fun setupDragAndDrop() {
        setOnDragOver { event: DragEvent ->
            if (event.dragboard.hasFiles() || event.dragboard.hasUrl()) {
                event.acceptTransferModes(*TransferMode.ANY)
                event.consume()
            }
        }
        setOnDragDropped { event ->
            val board = event.dragboard
            when {
                board.hasFiles() && board.files.isNotEmpty() -> loadSchema(board.files[0].path)
                board.hasUrl() -> loadSchema(board.url)
            }
            event.isDropCompleted = true
            event.consume()
        }
    }

    // ------------------------------------------------------------------
    // Barra de estado (toolStripStatusLabel1 + textBoxElementPath)
    // ------------------------------------------------------------------

    private fun buildStatusBar() {
        statusLabel = Label("Ready")
        textBoxElementPath = TextField().apply {
            isEditable = false
        }
        bottom = HBox(6.0).apply {
            padding = Insets(2.0, 6.0, 2.0, 6.0)
            children.addAll(
                statusLabel,
                Separator(Orientation.VERTICAL),
                Label("Element path:"),
                textBoxElementPath
            )
            HBox.setHgrow(textBoxElementPath, Priority.ALWAYS)
        }
    }

    private fun setStatus(text: String) {
        statusLabel.text = text
    }

    // ------------------------------------------------------------------
    // Equivale a Application.Run(new MainForm())
    // ------------------------------------------------------------------

    fun showOn(stage: Stage) {
        currentStage = stage
        // ≈ ctor del C# (91-94): estado inicial del diagram desde Settings
        // (el C# encadenaba diagram.ShowDocumentation = toggle.Checked =
        // Options.ShowDocumentation | Settings.Default.ShowDocumentation).
        val showDocumentationSetting = Options.showDocumentation || Settings.showDocumentation
        toolStripButtonShowDocumentation.isSelected = showDocumentationSetting
        diagram.showDocumentation = showDocumentationSetting
        diagram.alwaysShowOccurence = Settings.alwaysShowOccurence
        diagram.showType = Settings.showType
        diagram.compactLayoutDensity = Settings.compactLayoutDensity

        // ≈ MainForm_Load (184-188): zoom/align/panel desde Settings (cambiado
        // del valor por defecto (8, 1) que había antes de fase 4).
        toolStripComboBoxZoom.selectionModel.select(Settings.zoom)
        toolStripComboBoxAlignement.selectionModel.select(Settings.alignement)
//        if (toolStripButtonTogglePanel.isSelected != Settings.displayPanel) {
//            toolStripButtonTogglePanel.isSelected = Settings.displayPanel
//        }
//        if (Settings.displayPanel) showSidePanel() else hideSidePanel()

        // ≈ MainForm_Load (181): MRUManager persistente ("Recent Files").
        mruManager = MRUManager(
            recentFilesMenu,
            "xsddiagramx",
            { filenameOrUrl -> loadSchema(filenameOrUrl) },
            { /* ≈ recentFilesToolStripMenuSubItemClearAll_Click (vacío en el C#) */ }
        )

        stage.title = originalTitle
        stage.scene = Scene(this, 1024.0, 768.0)
        stage.show()

        // ≈ MainForm_Load (192-223): esquema y opciones de la línea de comandos.
        val inputFile = Options.inputFile
        if (!inputFile.isNullOrEmpty()) {
            loadSchema(inputFile)
            for (rootElement in Options.rootElements) {
                var elementName = rootElement
                var elementNamespace: String? = null
                if (elementName.isNotEmpty()) {
                    val pos = rootElement.indexOf('@')
                    if (pos != -1) {
                        elementName = rootElement.substring(0, pos)
                        elementNamespace = rootElement.substring(pos + 1)
                    }
                }
                // ≈ for (element in schema.Elements): los indexados por nombre.
                for ((_, xsdObject) in schema.elementsByName) {
                    val element = xsdObject ?: continue
                    if ((elementNamespace != null && elementNamespace == element.nameSpace && element.name == elementName) ||
                        (elementNamespace == null && element.name == elementName)
                    ) {
                        diagram.add(element.tag, element.nameSpace)
                    }
                }
            }
            for (i in 0 until Options.expandLevel) {
                diagram.expandOneLevel()
            }
            updateDiagram()
        }
    }

    // ------------------------------------------------------------------
    // Carga (≈ LoadSchema / open / openURL / drag&drop)
    // ------------------------------------------------------------------

    private fun openSchema() {
        val chooser = FileChooser().apply {
            title = "Open XSD schema"
            extensionFilters.addAll(
                FileChooser.ExtensionFilter("xsd files (*.xsd)", "*.xsd"),
                FileChooser.ExtensionFilter("All files (*.*)", "*.*")
            )
        }
        val file = chooser.showOpenDialog(currentStage)
        if (file != null) loadSchema(file.path)
    }

    private fun openSchemaUrl() {
        // ≈ OpenURLForm (fase: TextInputDialog provisional).
        val dialog = TextInputDialog("").apply {
            title = "Open URL"
            headerText = "Open schema from a URL"
            contentText = "URL:"
        }
        val url = dialog.showAndWait().orElse(null)
        if (!url.isNullOrEmpty()) loadSchema(url)
    }

    /**
     * ≈ MainForm.LoadSchema(string), pero asíncrono: la descarga de dependencias
     * remotas corre en un hilo de fondo para no congelar la GUI, con un diálogo
     * modal de progreso (APPLICATION_MODAL) con botón "Cancelar".
     */
    private var isLoading = false
    private var downloadDialog: SchemaDownloadDialog? = null

    fun loadSchema(schemaFilename: String) {
        if (isLoading) return
        isLoading = true

        currentStage?.scene?.cursor = Cursor.WAIT
        addRecentFile(schemaFilename)
        cleanupUserInterface(false)
        updateTitle(schemaFilename)
        setStatus("Loading schema...")

        // Diálogo de progreso (el botón Cancelar pide abortar al cargador).
        val dialog = SchemaDownloadDialog(currentStage?.scene?.window) {
            schema.requestCancel()
        }
        downloadDialog = dialog
        schema.progressListener = object : SchemaLoader.SchemaLoadProgressListener {
            override fun onDownloadStart(url: String) = dialog.onDownloadStart(url)
            override fun onDownloadEnd(url: String, failed: Boolean) = dialog.onDownloadEnd(failed)
        }
        dialog.show()

        Thread {
            var cancelled = false
            var threw: String? = null
            try {
                schema.loadSchema(schemaFilename)
            } catch (_: SchemaLoader.SchemaLoadCancelledException) {
                cancelled = true
            } catch (ex: Exception) {
                threw = ex.toString()
            }

            // Leer el contenido de cada .xsd EN LÍNEAS para las pestañas AQUÍ
            // (hilo de fondo): en el FX Thread congelaba la ventana al poblar.
            var tabLines: MutableMap<String, List<String>>? = null
            if (!cancelled && threw == null) {
                tabLines = HashMap()
                for (filename in schema.xsdFilenames) {
                    dialog.log("Leyendo: $filename")
                    tabLines[filename] = readSchemaLines(filename)
                }
            }

            val loadCancelled = cancelled
            val loadError = threw
            val tabLinesFinal = tabLines
            Platform.runLater {
                isLoading = false
                currentStage?.scene?.cursor = Cursor.DEFAULT
                schema.progressListener = null

                // El diálogo se cierra AL FINAL, cuando la ventana ya está
                // poblada (antes se cerraba cuando el hilo de carga terminaba,
                // aparentando que seguía cargando con la GUI bloqueada).
                dialog.close()
                downloadDialog = null

                when {
                    loadCancelled -> {
                        // Descartar el estado a medias (≈ CleanupUserInterface).
                        schema.cleanup()
                        cleanupUserInterface(false)
                        setStatus("Open cancelled: schema not loaded")
                    }
                    loadError != null -> {
                        schema.cleanup()
                        showErrorReport(schemaFilename, listOf(loadError))
                        setStatus("Loading failed")
                    }
                    else -> {
                        finishSchemaLoad(schemaFilename, tabLinesFinal)
                        setStatus("Ready")
                    }
                }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    /** Rellena la GUI tras una carga correcta (parte sincrónica del C#). */
    private fun finishSchemaLoad(
        schemaFilename: String,
        tabLines: MutableMap<String, List<String>>?,
    ) {
        updateActionsState()

        for (xsdObject in schema.elements) {
            listViewElements.items.add(ElementsRow(xsdObject))
            toolStripComboBoxSchemaElement.items.add(xsdObject)
        }

        if (schema.loadErrors.isNotEmpty()) {
            showErrorReport(schemaFilename, schema.loadErrors)
        }

        diagram.elementsByName = schema.elementsByName
        val first = schema.firstElementOrNull()
        if (first != null) {
            toolStripComboBoxSchemaElement.selectionModel.select(first)
            selectSchemaElement(first)
        }

        // Pestañas con el contenido de los .xsd cargados (≈ tabControlView).
        // Las líneas las leyó el hilo de fondo (tabLines).
        //
        // IMPORTANTE: esto era un TextArea con el texto completo del .xsd y
        // ERA LA CAUSA del "no responde" al estirar/maximizar la ventana:
        // TextAreaSkin.computePrefWidth re-mide TODO el documento (layout de
        // glifos, O(documento)) en cada cambio de ancho — cfdv40.xsd tira
        // segundos por evento de resize. Un ListView es virtualizado: sólo
        // materializa/manda las filas visibles (O(visible)).
        for (filename in schema.xsdFilenames) {
            if (hasTabForFile(filename)) continue
            val shortName = filename
                .substringAfterLast('/')
                .substringAfterLast('\\')
                .substringBeforeLast('.')
            val linesView = javafx.scene.control.ListView<String>().apply {
                isEditable = false
                style = "-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px;"
                cellFactory = javafx.util.Callback {
                    object : javafx.scene.control.ListCell<String>() {
                        override fun updateItem(item: String?, empty: Boolean) {
                            super.updateItem(item, empty)
                            text = if (empty || item == null) null else item
                            // nowrap: preservar líneas largas sin envolver
                            isWrapText = false
                        }
                    }
                }
                items = javafx.collections.FXCollections.observableList(
                    tabLines?.get(filename) ?: emptyList()
                )
            }
            val tab = Tab(shortName, linesView)
            tab.userData = filename
            tabControlView.tabs.add(tab)
        }
    }

    private fun hasTabForFile(filename: String): Boolean =
        tabControlView.tabs.any { it.userData == filename }

    /** ≈ tabControlView_Selected: lee el fichero (local o URL) EN LÍNEAS al hilo de fondo. */
    private fun readSchemaLines(filename: String): List<String> =
        try {
            if (filename.startsWith("http://") || filename.startsWith("https://")) {
                URL(filename).openStream().use { inp ->
                    inp.bufferedReader(Charsets.UTF_8).readLines()
                }
            } else {
                java.io.File(filename).readLines()
            }
        } catch (ex: Exception) {
            listOf(ex.toString())
        }

    /** ≈ UpdateTitle. */
    private fun updateTitle(filename: String) {
        val title =
            if (filename.isNotEmpty()) "$originalTitle - $filename" else originalTitle
        currentStage?.title = title
    }

    /** ≈ UpdateActionsState. */
    private fun updateActionsState() {
        val isSchemaLoaded = schema.isLoaded()
        toolStripButtonAddToDiagram.isDisable = !isSchemaLoaded
        toolStripButtonAddAllToDiagram.isDisable = !isSchemaLoaded
        toolStripButtonRemoveAllFromDiagram.isDisable = !isSchemaLoaded
        toolStripButtonExpandOneLevel.isDisable = !isSchemaLoaded
        closeMenuItem.isDisable = !isSchemaLoaded
        // ≈ validateXMLFileToolStripMenuItem del C#.
        validateXMLMenuItem.isDisable = !isSchemaLoaded
        // ≈ saveDiagram/printPreview/print del C# (page no se deshabilita).
        saveDiagramMenuItem.isDisable = !isSchemaLoaded
        printPreviewMenuItem.isDisable = !isSchemaLoaded
        printMenuItem.isDisable = !isSchemaLoaded
    }

    // ------------------------------------------------------------------
    // Exportación / impresión (≈ SaveDiagramAs / Page / PrintPreview / Print)
    // ------------------------------------------------------------------

    /** ≈ _diagramPrinter del C# (se crea perezosamente en el primer uso). */
    private var diagramPrinter: DiagramPrinter? = null

    /**
     * ≈ saveDiagramToolStripMenuItem_Click: FileChooser con los mismos filtros
     * que el SaveFileDialog del C# (sin .emf: metafiles GDI+ omitidos) y export
     * con los parámetros de los settings (fase 4: TextOutputFields vacío).
     */
    private fun saveDiagramAs() {
        val chooser = FileChooser().apply {
            title = "Save diagram"
            extensionFilters.addAll(
                FileChooser.ExtensionFilter("SVG files (*.svg)", "*.svg"),
                FileChooser.ExtensionFilter("PNG files (*.png)", "*.png"),
                FileChooser.ExtensionFilter("JPG files (*.jpg)", "*.jpg"),
                FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt"),
                FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"),
                FileChooser.ExtensionFilter("All files (*.*)", "*.*")
            )
        }
        chooser.selectedExtensionFilter = chooser.extensionFilters[0]
        val file = chooser.showSaveDialog(currentStage) ?: return
        try {
            val exporter = DiagramExporter(diagram)
            exporter.export(
                file.path,
                JavaFxDrawingContext(),
                { title, message -> showSaveAlert(title, message) },
                // ≈ { "TextOutputFields", Options.TextOutputFields } del C#.
                mapOf<String, Any?>("TextOutputFields" to Options.textOutputFields)
            )
        } catch (ex: IllegalArgumentException) {
            // ≈ ArgumentException del GDI+ (imagen demasiado grande).
            showMessageBox("You have reach the system limit.\r\nPlease remove some element from the diagram to make it smaller.")
            println(ex)
        } catch (ex: Exception) {
            showMessageBox(ex.message ?: ex.toString())
            println(ex)
        }
    }

    /** ≈ SaveAlert: MessageBox.YesNo (sí = continuar). */
    private fun showSaveAlert(title: String, message: String): Boolean {
        val alert = Alert(Alert.AlertType.CONFIRMATION).apply {
            this.title = title
            headerText = null
            contentText = message
            initOwner(currentStage)
        }
        val buttonYes = javafx.scene.control.ButtonType.YES
        alert.buttonTypes.setAll(buttonYes, javafx.scene.control.ButtonType.NO)
        return alert.showAndWait().orElse(javafx.scene.control.ButtonType.NO) == buttonYes
    }

    /** ≈ MessageBox.Show(this, text) simple. */
    private fun showMessageBox(message: String) {
        Alert(Alert.AlertType.INFORMATION).apply {
            title = "XSDDiagramX"
            headerText = null
            contentText = message
            initOwner(currentStage)
        }.show()
    }

    /** ≈ pageToolStripMenuItem_Click. */
    private fun printPageSetup() {
        try {
            val printer = getDiagramPrinter()
            printer.pageSetup(currentStage)
        } catch (ex: Exception) {
            showMessageBox(ex.message ?: ex.toString())
        }
    }

    /** ≈ printPreviewToolStripMenuItem_Click. */
    private fun printDiagramPreview() {
        try {
            val printer = getDiagramPrinter()
            printer.printPreview(currentStage)
        } catch (ex: Exception) {
            showMessageBox(ex.message ?: ex.toString())
        }
    }

    /** ≈ printToolStripMenuItem_Click (el C# pasaba `Options.IsRunningOnMono` como showDialog=false en Mono). */
    private fun printDiagram(showDialog: Boolean) {
        try {
            val printer = getDiagramPrinter()
            printer.print(showDialog, currentStage)
        } catch (ex: Exception) {
            showMessageBox(ex.message ?: ex.toString())
        }
    }

    private fun getDiagramPrinter(): DiagramPrinter {
        var printer = diagramPrinter
        if (printer == null) {
            printer = DiagramPrinter(JavaFxDrawingContext())
            diagramPrinter = printer
        }
        printer.documentName = "XSD Document"
        printer.setDiagram(diagram)
        return printer
    }

    // ------------------------------------------------------------------
    // Settings (≈ settingsToolStripMenuItem_Click + ChangeSetting)
    // ------------------------------------------------------------------

    /** ≈ settingsForm (se crea y queda vivo hasta cerrar, no modal). */
    private var settingsForm: SettingsForm? = null

    /** ≈ settingsToolStripMenuItem_Click. */
    private fun showSettingsForm() {
        var form = settingsForm
        if (form == null) {
            val owner = currentStage
            if (owner == null) return
            form = SettingsForm(owner)
            // ≈ SettingsForm_FormClosed: liberar la referencia (el propertyGrid
            // del C# ponía SelectedObject = null).
            form.setOnCloseRequest {
                settingsForm = null
            }
            settingsForm = form
            form.initModality(javafx.stage.Modality.NONE)
            form.show()
        }
        form.toFront()
        form.requestFocus()
    }

    /** ≈ MainForm.ChangeSetting: aplica el setting editado en el SettingsForm. */
    internal fun changeSetting(settingName: String) {
        when (settingName) {
            "AlwaysShowOccurence" -> {
                diagram.alwaysShowOccurence = Settings.alwaysShowOccurence
                updateDiagram()
            }
            "ShowType" -> {
                diagram.showType = Settings.showType
                updateDiagram()
            }
            "CompactLayoutDensity" -> {
                diagram.compactLayoutDensity = Settings.compactLayoutDensity
                updateDiagram()
            }
        }
    }

    // ------------------------------------------------------------------
    // Validación XML (≈ validateXMLFileToolStripMenuItem_Click)
    // ------------------------------------------------------------------

    /** ≈ validationErrorMessages del C#. */
    private val validationErrorMessages = ArrayList<String>()

    /**
     * ≈ validateXMLFileToolStripMenuItem_Click: valida el .xml contra todos los
     * .xsd cargados. Java: javax.xml.validation (JDK, sin terceros).
     * Diferencia: ProcessInlineSchema/ProcessSchemaLocation/ProcessIdentity-
     * Constraints/ReportValidationWarnings dependen del parser del JDK y no
     * tienen los mismos flags conmutables que el XmlReaderSettings de .NET.
     */
    private fun validateXMLFile() {
        val chooser = FileChooser().apply {
            title = "Validate XML"
            extensionFilters.addAll(
                FileChooser.ExtensionFilter("xml files (*.xml)", "*.xml"),
                FileChooser.ExtensionFilter("All files (*.*)", "*.*")
            )
        }
        chooser.selectedExtensionFilter = chooser.extensionFilters[1]
        val file = chooser.showOpenDialog(currentStage) ?: return
        try {
            currentStage?.scene?.cursor = Cursor.WAIT
            val messages = validateXmlCore(file.path)
            currentStage?.scene?.cursor = Cursor.DEFAULT
            showErrorReport(file.path, messages)
        } catch (ex: IllegalStateException) {
            currentStage?.scene?.cursor = Cursor.DEFAULT
            // El núcleo ya preparó "Error while validating..., Message: ..." en
            // [validationErrorMessages] (≈ el catch del C# con su mensaje).
            showErrorReport(file.path, validationErrorMessages)
        } catch (ex: Exception) {
            currentStage?.scene?.cursor = Cursor.DEFAULT
            validationErrorMessages.add(
                "Error while validating ${file.path}, Message: ${ex.message ?: ex.toString()}"
            )
            showErrorReport(file.path, validationErrorMessages)
        }
    }

    /**
     * Núcleo de la validación ≈ el try/catch del C# (lo separa el port para
     * el autotest selftest=validate). Devuelve los mensajes y además los deja
     * en [validationErrorMessages].
     */
    fun validateXmlCore(xmlFilename: String): List<String> {
        validationErrorMessages.clear()
        val xmlSource = File(xmlFilename).readText()

            val factory = javax.xml.validation.SchemaFactory
                .newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI)
            // ≈ settings.Schemas.Add(null, schemaFilename) por cada .xsd
            // cargado (en orden inverso, como el C#). Aquí se compilan en una
            // sola llamada newSchema(sources); un .xsd mal formado aborta el
            // conjunto (el C# capturaba por fichero).
            val schemaFiles = schema.xsdFilenames.reversed()
            val schemaSource: javax.xml.validation.Schema = try {
                factory.newSchema(
                    schemaFiles.map { javax.xml.transform.stream.StreamSource(java.io.File(it)) }.toTypedArray()
                )
            } catch (ex: Exception) {
                // ≈ el mensaje por fichero del C# (aquí el conjunto entero falla
                // en una sola llamada; se apunta con el esquema atacado).
                validationErrorMessages.add(
                    "Error while parsing ${schemaFiles.joinToString()}, Message: ${ex.message ?: ex.toString()}"
                )
                throw IllegalStateException(
                    "Error while validating $xmlFilename, Message: ${ex.message ?: ex.toString()}", ex
                )
            }

            val validator = schemaSource.newValidator()
            // ≈ settings.ValidationEventHandler(ValidationHandler).
            validator.errorHandler = object : org.xml.sax.ErrorHandler {
                override fun warning(ex: org.xml.sax.SAXParseException) {
                    validationErrorMessages.add(
                        "Validation Warning: ${ex.message} (line ${ex.lineNumber})"
                    )
                }

                override fun error(ex: org.xml.sax.SAXParseException) {
                    validationErrorMessages.add(
                        "Validation Error: ${ex.message} (line ${ex.lineNumber})"
                    )
                }

                override fun fatalError(ex: org.xml.sax.SAXParseException) {
                    validationErrorMessages.add(
                        "Validation Fatal: ${ex.message} (line ${ex.lineNumber})"
                    )
                }
            }

            // ≈ XmlReader.Create(r, settings) + Read() hasta el final.
            validator.validate(javax.xml.transform.stream.StreamSource(java.io.StringReader(xmlSource)))
            return validationErrorMessages.toList()
        }
        // (fin del núcleo de validación)

    /** ≈ CleanupUserInterface(bool fullCleanup). */
    private fun cleanupUserInterface(fullCleanup: Boolean) {
        diagram.clear()
        panelDiagram.setVirtualSize(KSize(0, 0))
        panelDiagram.clear()
        listViewElements.items.clear()
        listViewAttributes.items.clear()
        listViewEnumerate.items.clear()
        toolStripComboBoxSchemaElement.selectionModel.clearSelection()
        toolStripComboBoxSchemaElement.items.clear()
        textBoxElementPath.text = ""
        textBoxAnnotation.text = null

        // Eliminar todas las pestañas menos "Diagram".
        while (tabControlView.tabs.size > 1) tabControlView.tabs.removeAt(1)
        panelDiagram.repaint()

        if (fullCleanup) {
            updateTitle("")
            schema.cleanup()
            updateActionsState()
        }
    }

    // ------------------------------------------------------------------
    // Diagrama: actualización / pintado (≈ UpdateDiagram / DiagramControl_Paint)
    // ------------------------------------------------------------------

    private fun updateDiagram() {
        if (diagram.rootElements.isNotEmpty()) {
            // ≈ Diagram.Layout(g): medición de textos vía JavaFX.
            diagram.layout(JavaFxDrawingContext())
            val bbSize = diagram.boundingBox.size + diagram.padding + diagram.padding
            panelDiagram.setVirtualSize(
                KSize(
                    (bbSize.width * diagram.scale).toInt(),
                    (bbSize.height * diagram.scale).toInt()
                )
            )
            if (System.getProperty("xsddiagramx.fxwatch") == "1") {
                println(
                    "[SCROLLDBG] updateDiagram: roots=${diagram.rootElements.size} " +
                        "bb=${diagram.boundingBox} scale=${diagram.scale}"
                )
                for (r in diagram.rootElements) {
                    println("[SCROLLDBG]   root ${r.name} ${r.itemType} box=${r.boundingBox}")
                }
            }
        } else {
            panelDiagram.setVirtualSize(KSize(0, 0))
        }
        panelDiagram.repaint()
    }

    // ------------------------------------------------------------------
    // Acciones de la toolbar (≈ toolStripButtonAddToDiagram, etc.)
    // ------------------------------------------------------------------

    /** ≈ toolStripButtonAddToDiagram_Click. */
    private fun addToDiagram(xsdObject: XSDObject?) {
        if (xsdObject != null) {
            val diagramItem = diagram.add(xsdObject.tag, xsdObject.nameSpace)
            if (diagramItem != null) {
                selectDiagramElement(diagramItem, true)
            } else {
                updateDiagram()
            }
        }
    }

    /** Gatillo para el autotest selftest=scroll (equivale a pulsar "Add all"). */
    fun selftestAddAll() = addAllToDiagram()

    /** Gatillo para el autotest: equivale a pulsar "Expand 1". */
    fun selftestExpandOne(): Boolean =
        if (diagram.expandOneLevel()) { updateDiagram(); true } else false

    /**
     * Gatillo para el autotest selftest=settings (fase 4): cambia Settings
     * (persistiendo en Preferences) + aplica por ChangeSetting y reporta los
     * valores antes/después; deja todo restaurado.
     */
    fun selftestSettings(): String {
        val before = "type=${diagram.showType} occ=${diagram.alwaysShowOccurence} " +
            "compact=${diagram.compactLayoutDensity} panel=${Settings.displayPanel} " +
            "zoom=${Settings.zoom} align=${Settings.alignement} doc=${Settings.showDocumentation}"

        Settings.showType = true
        changeSetting("ShowType")
        Settings.alwaysShowOccurence = true
        changeSetting("AlwaysShowOccurence")
        val applied = "type=${diagram.showType} occ=${diagram.alwaysShowOccurence}"

        // Restaurar y verificar que ChangeSetting aplica de nuevo.
        Settings.showType = false
        changeSetting("ShowType")
        Settings.alwaysShowOccurence = false
        changeSetting("AlwaysShowOccurence")
        val restored = "type=${diagram.showType} occ=${diagram.alwaysShowOccurence}"

        return "before=[$before] applied=[$applied] restored=[$restored] " +
            "(persistido en Preferences: zoom=${Settings.zoom} align=${Settings.alignement})"
    }

    /**
     * Gatillo para el autotest selftest=validate (fase 4): escribe un XML
     * válido y otro inválido para el esquema cargado y corre el núcleo de
     * validateXmlCore sobre ambos; devuelve el resumen de mensajes.
     */
    fun selftestValidate(): String? {
        if (!schema.isLoaded()) return null
        val tempOpencode = File(System.getProperty("java.io.tmpdir"), "opencode")
        tempOpencode.mkdirs()

        val valid = File(tempOpencode, "xsddiagramx-validate-ok.xml")
        valid.writeText(
            """<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <bean id="uno" class="java.lang.String">
    <property name="texto" value="hola"/>
  </bean>
</beans>"""
        )
        val invalid = File(tempOpencode, "xsddiagramx-validate-bad.xml")
        invalid.writeText(
            """<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <not-a-bean/>
</beans>"""
        )

        val report = StringBuilder()
        try {
            val okMessages = validateXmlCore(valid.path)
            report.append("ok=${okMessages.size} mensajes ")
        } catch (ex: Exception) {
            report.append("ok-ERROR=${ex.message} ")
        }
        try {
            val badMessages = validateXmlCore(invalid.path)
            report.append("bad=${badMessages.size} mensajes")
            if (badMessages.isNotEmpty()) report.append(" [${badMessages.first()}]")
        } catch (ex: Exception) {
            report.append("bad-ERROR=${ex.message ?: ex}")
        }
        return report.toString()
    }

    /**
     * Gatillo para el autotest selftest=export (fase 3): construye un diagrama
     * (Add all + expand), lo exporta a SVG/TXT/PNG junto a [basename] en la
     * carpeta temporal y devuelve el reporte de tamaños en bytes.
     */
    fun selftestExport(basename: String): String {
        selftestAddAll()
        for (i in 1..5) {
            if (!selftestExpandOne()) break
        }
        val tempOpencode = File(System.getProperty("java.io.tmpdir"), "opencode").apply { mkdirs() }

        val exporter = DiagramExporter(diagram)
        val context = JavaFxDrawingContext()
        val report = StringBuilder()
        for (ext in listOf("svg", "txt", "png")) {
            val target = File(tempOpencode, "$basename.$ext")
            try {
                val ok = exporter.export(
                    target.path,
                    context,
                    null,
                    mapOf<String, Any?>("TextOutputFields" to emptyList<String>())
                )
                val size = if (ok) target.length() else -1L
                report.append("$ext=")
                report.append(if (size >= 0) "${size}B" else "FAILED")
                report.append(" ")
            } catch (t: Throwable) {
                report.append("$ext=").append("ERROR:${t.javaClass.simpleName}").append(" ")
                println("SELFTEST-EXPORT $ext error:")
                t.printStackTrace()
            }
        }
        return report.toString().trim()
    }

    /** Gatillo para el autotest: selecciona el nodo del diagrama con ese
     *  nombre, abre el tab "Element" y reporta cuántas filas tiene el grid. */
    fun selftestShowElementTab(name: String) {
        fun walk(item: DiagramItem): DiagramItem? {
            if (item.name == name) return item
            for (child in item.childElements) walk(child)?.let { return it }
            return null
        }
        val node = diagram.rootElements.firstNotNullOfOrNull { walk(it) }
        if (node == null) {
            println("SELFTEST: node '$name' not found in diagram")
            return
        }
        selectDiagramElement(node, true)
        tabControlElement.selectionModel.select(1) // tab "Element" (≈ propertyGrid)
        println("SELFTEST: node '$name' selected; grid has ${gridSchemaObject.items.size} property rows")
    }

    /** Gatillo para el autotest: selecciona un atributo del tab "Attributes"
     *  (≈ listViewAttributes_SelectedIndexChanged) y reporta el tab "Element". */
    fun selftestSelectAttribute(name: String) {
        val row = listViewAttributes.items.firstOrNull { it.name == name }
        if (row == null) {
            println("SELFTEST: attribute '$name' not found (attrs=${listViewAttributes.items.joinToString { it.name }})")
            return
        }
        println("SELFTEST: selecting attribute '$name' (type=${row.type})")
        listViewAttributes.selectionModel.select(row)
        println("SELFTEST: Element tab now has ${listViewEnumerate.items.size} enumerable values")
    }

    /** ≈ toolStripButtonAddAllToDiagram_Click. */
    private fun addAllToDiagram() {
        if (System.getProperty("xsddiagramx.fxwatch") == "1") {
            val counts = schema.elementsByName.values
                .filterNotNull()
                .groupingBy { it.tag?.let { t -> t::class.simpleName } ?: "null" }
                .eachCount()
            println("[SCROLLDBG] elementsByName.non-null=${schema.elementsByName.values.count { it != null }} " +
                "all=${schema.elementsByName.size} tagTypes=$counts")
            for ((k, v) in schema.elementsByName) {
                println("[SCROLLDBG]   key='$k' tag=${v?.tag?.let { t -> t::class.simpleName }}")
            }
        }
        var firstDiagramItem: DiagramItem? = null
        for (xsdObject in schema.elementsByName.values) {
            if (xsdObject != null) {
                val diagramItem = diagram.add(xsdObject.tag, xsdObject.nameSpace)
                if (firstDiagramItem == null && diagramItem != null) {
                    firstDiagramItem = diagramItem
                }
            }
        }
        if (firstDiagramItem != null) {
            selectDiagramElement(firstDiagramItem, true)
        } else {
            updateDiagram()
        }
    }

    /** ≈ toolStripButtonRemoveAllFromDiagram_Click. */
    private fun removeAllFromDiagram() {
        toolStripComboBoxSchemaElement.selectionModel.clearSelection()
        diagram.removeAll()
        updateDiagram()
    }

    // ------------------------------------------------------------------
    // Zoom / paneles / tabs (≈ toolStripComboBoxZoom, toggle, Tabs)
    // ------------------------------------------------------------------

    private fun zoom_SelectedIndexChanged() {
        val zoomString = toolStripComboBoxZoom.selectionModel.selectedItem?.replace("%", "") ?: return
        val zoom = zoomString.toIntOrNull()?.div(100.0f) ?: return
        if (zoom < 0.10f || zoom > 10f) return

        // ≈ cs line 936: Settings.Default.Zoom = SelectedIndex (se persiste
        // al cerrar la app; el -1 de lista vacía se deja fuera por seguridad).
        val selectedIndex = toolStripComboBoxZoom.selectionModel.selectedIndex
        if (selectedIndex >= 0) Settings.zoom = selectedIndex

        // ≈ cs: preserva el centro virtual al escalar.
        val virtualCenter = KPoint(
            panelDiagram.getVirtualPoint().x + panelDiagram.diagramControl.clientRectangle.width / 2,
            panelDiagram.getVirtualPoint().y + panelDiagram.diagramControl.clientRectangle.height / 2
        )
        val oldSize = panelDiagram.getVirtualSize()
        diagram.scale = zoom
        updateDiagram()
        val newSize = panelDiagram.getVirtualSize()
        var newVirtualCenter = KPoint(
            (newSize.width.toDouble() / max(1, oldSize.width).toDouble() * virtualCenter.x).toInt(),
            (newSize.height.toDouble() / max(1, oldSize.height).toDouble() * virtualCenter.y).toInt()
        )
        if (newVirtualCenter.x > diagram.boundingBox.right) {
            newVirtualCenter = KPoint(diagram.boundingBox.right, newVirtualCenter.y)
        }
        if (newVirtualCenter.y > diagram.boundingBox.bottom) {
            newVirtualCenter = KPoint(newVirtualCenter.x, diagram.boundingBox.bottom)
        }
        panelDiagram.scrollTo(newVirtualCenter, true)
    }

    private fun hideSidePanel() {

        //mainSplit.items[1].isVisible = false
        mainSplit.setDividerPosition(0, 1.0)
        mainSplit.items.removeLast()
    }

    private fun showSidePanel() {
        //mainSplit.items[1].isVisible = true
        mainSplit.setDividerPosition(0, 0.75)
        mainSplit.items.add(sidePanel)
    }

    private fun cycleTab(delta: Int) {
        val count = tabControlView.tabs.size
        if (count < 2) return
        val current = tabControlView.selectionModel.selectedIndex
        tabControlView.selectionModel.select((current + delta + count) % count)
    }

    private fun showAbout() {
        Alert(Alert.AlertType.INFORMATION).apply {
            title = "About XSDDiagramX"
            headerText = "XSDDiagramX (Kotlin/JavaFX port del C# XSD Diagram)"
            contentText = """
                Port del XSD Diagram C# (Regis Cosnier, 2006-2011).
                Licencias del original: LGPL / MS-PL.

                Fase 1: capa de dominio (parsing + motor del diagrama).
                Fase 2: GUI JavaFX (este formulario).
            """.trimIndent()
        }.show()
    }

    private fun showErrorReport(schemaFilename: String, errors: List<String>) {
        Alert(Alert.AlertType.ERROR).apply {
            title = "Schema load errors"
            headerText = "There were problems loading '$schemaFilename'"
            contentText = errors.take(5).joinToString("\n\n") { it.take(400) }
        }.show()
    }

    // ------------------------------------------------------------------
    // Interacción con el canvas (≈ DiagramControl_MouseClick / Wheel / KeyDown)
    // ------------------------------------------------------------------

    private fun hookCanvasEvents() {
        panelDiagram.diagramMouseClick = { location ->
            // ≈ location.Offset(panelDiagram.VirtualPoint) del C#.
            val diagramPoint = KPoint(
                location.x + panelDiagram.getVirtualPoint().x,
                location.y + panelDiagram.getVirtualPoint().y
            )
            val result = diagram.hitTest(diagramPoint)
            if (result.region != DiagramHitTestRegion.None) {
                when {
                    result.region == DiagramHitTestRegion.ChildExpandButton -> {
                        val resultElement = result.element
                        if (resultElement != null && resultElement.hasChildElements) {
                            if (resultElement.childElements.isEmpty()) {
                                diagram.expandChildren(resultElement)
                                resultElement.showChildElements = true
                            } else {
                                diagram.clearSearch()
                                resultElement.showChildElements = !resultElement.showChildElements
                            }
                            selectDiagramElement(resultElement, true)
                        }
                    }
                    result.region == DiagramHitTestRegion.Element ->
                        selectDiagramElement(result.element)
                    else -> selectDiagramElement(null)
                }
            }
        }

        panelDiagram.diagramWheelZoom = { deltaSign ->
            val index = toolStripComboBoxZoom.selectionModel.selectedIndex + deltaSign
            if (index >= 0 && index < zoomValues.size) {
                toolStripComboBoxZoom.selectionModel.select(index)
            }
        }

        panelDiagram.diagramKeyPressed = { event -> diagramControl_KeyDown(event) }
    }

    /** ≈ DiagramControl_KeyDown. */
    private fun diagramControl_KeyDown(event: KeyEvent) {
        if (diagram.rootElements.isEmpty()) return

        when (event.code) {
            javafx.scene.input.KeyCode.SPACE, javafx.scene.input.KeyCode.ENTER ->
                expandCollapseElement(diagram.selectedElement, true)
            javafx.scene.input.KeyCode.DELETE -> {
                val selected = diagram.selectedElement ?: return
                val parentElement = selected.parent
                diagram.remove(selected)
                // DiagramControl_KeyDown del C#.
                if (parentElement != null) {
                    selectDiagramElement(parentElement, true)
                } else {
                    selectDiagramElement(null)
                }
            }
            javafx.scene.input.KeyCode.RIGHT, javafx.scene.input.KeyCode.LEFT,
            javafx.scene.input.KeyCode.DOWN, javafx.scene.input.KeyCode.UP,
            -> {
                val current = diagram.selectedElement
                if (current == null) {
                    selectDiagramElement(diagram.rootElements[0], true)
                } else {
                    when (event.code) {
                        javafx.scene.input.KeyCode.RIGHT -> {
                            if (current.hasChildElements) {
                                if (current.childElements.isEmpty()) {
                                    diagram.expandChildren(current)
                                    current.showChildElements = true
                                } else {
                                    current.showChildElements = true
                                }
                                if (current.childElements.isNotEmpty()) {
                                    selectDiagramElement(current.childElements[0], true)
                                }
                            }
                        }
                        javafx.scene.input.KeyCode.LEFT ->
                            current.parent?.let { selectDiagramElement(it, true) }
                        javafx.scene.input.KeyCode.DOWN -> {
                            val children = current.parent?.childElements ?: diagram.rootElements
                            val pos = children.indexOf(current)
                            if (pos + 1 < children.size) selectDiagramElement(children[pos + 1], true)
                        }
                        javafx.scene.input.KeyCode.UP -> {
                            val children = current.parent?.childElements ?: diagram.rootElements
                            val pos = children.indexOf(current)
                            if (pos - 1 >= 0) selectDiagramElement(children[pos - 1], true)
                        }
                        else -> {}
                    }
                }
            }
            else -> {}
        }
    }

    /** ≈ ExpandCollapseElement(element, scrollToElement). */
    private fun expandCollapseElement(element: DiagramItem?, scrollToElement: Boolean) {
        if (element != null && element.hasChildElements) {
            if (element.childElements.isEmpty()) {
                diagram.expandChildren(element)
                element.showChildElements = true
            } else {
                diagram.clearSearch()
                element.showChildElements = !element.showChildElements
            }
            updateDiagram()
            if (scrollToElement) {
                panelDiagram.scrollTo(diagram.scalePoint(element.location), true)
            }
        }
    }

    // ------------------------------------------------------------------
    // Selección / panel derecho (≈ SelectDiagramElement / SelectSchemaElement)
    // ------------------------------------------------------------------

    private fun selectDiagramElement(element: DiagramItem?) =
        selectDiagramElement(element, false)

    /** ≈ SelectDiagramElement(element, scrollToElement). */
    private fun selectDiagramElement(element: DiagramItem?, scrollToElement: Boolean) {
        textBoxElementPath.text = ""

        if (element == null) {
            toolStripComboBoxSchemaElement.selectionModel.clearSelection()
            cleanupSideSelection()
        } else {
            val xsdObject = schema.elementsByName[element.fullName]
            if (xsdObject != null) {
                toolStripComboBoxSchemaElement.selectionModel.select(xsdObject)
            } else {
                toolStripComboBoxSchemaElement.selectionModel.clearSelection()
            }

            selectSchemaElement(element)

            var path = "/" + element.name
            var parentElement = element.parent
            while (parentElement != null) {
                if (parentElement.itemType == DiagramItemType.element && parentElement.name.isNotEmpty()) {
                    path = "/" + parentElement.name + path
                }
                parentElement = parentElement.parent
            }
            textBoxElementPath.text = path
        }

        diagram.selectElement(element)
        updateDiagram()
        if (scrollToElement && element != null) {
            panelDiagram.scrollTo(diagram.scalePoint(element.location), true)
        }
    }

    private fun cleanupSideSelection() {
        listViewAttributes.items.clear()
        gridSchemaObject.items.clear()
        listViewEnumerate.items.clear()
        textBoxAnnotation.text = null
    }

    // ≈ propertyGridSchemaObject: tabla nombre/valor del objeto seleccionado.
    // ------------------------------------------------------------------

    /** ≈ propertyGridSchemaObject.SelectedObject = openAttrs. */
    private fun fillSchemaObjectGrid(openAttrs: Any?) {
        gridSchemaObject.items.clear()
        if (openAttrs == null) return

        // ≈ PropertySort.Alphabetical: propiedades del wrapper serializado
        // (clase + base class), ordenadas alfabéticamente.
        val props = LinkedHashMap<String, java.lang.reflect.Method>()
        var c: Class<*>? = openAttrs.javaClass
        while (c != null && c != Object::class.java) {
            for (m in c.declaredMethods) {
                val key = propertyNameOfGetter(m.name) ?: continue
                if (m.parameterCount == 0) props.putIfAbsent(key.lowercase(), m)
            }
            c = c.superclass
        }

        for ((_, getter) in props.entries.sortedBy { it.key }) {
            val key = propertyNameOfGetter(getter.name) ?: continue
            val value = try {
                getter.isAccessible = true
                getter.invoke(openAttrs)
            } catch (_: Exception) {
                null
            }
            gridSchemaObject.items.add(PropertyRow(key.lowerFirst(), describePropertyValue(value)))
        }
    }

    /** "getX" → "X", "isX" → "X"; null para lo que no es propiedad (getClass/hashCode). */
    private fun propertyNameOfGetter(method: String): String? {
        if (method == "getClass" || method == "hashCode") return null
        if (method.startsWith("get") && method.length > 3) return method.substring(3)
        if (method.startsWith("is") && method.length > 2) return method.substring(2)
        return null
    }

    /** ≈ el formateo del PropertyGrid de WinForms para el valor de una fila. */
    private fun describePropertyValue(value: Any?): String = when (value) {
        null -> ""
        is Boolean -> if (value) "True" else "False"
        is String -> value
        is Collection<*> -> "count = " + value.size
        // ≈ los tipos anidados del wrapper se muestran por su tipo
        // ("XMLSchema.annotation", "XMLSchema.localComplexType"...).
        else -> "XMLSchema." + value.javaClass.simpleName
    }

    private fun String.lowerFirst(): String =
        if (isEmpty()) this else substring(0, 1).lowercase() + substring(1)

    /** ≈ SelectSchemaElement(XSDObject). */
    private fun selectSchemaElement(xsdObject: XSDObject) {
        if (xsdObject == null) return
        selectSchemaElement(xsdObject.tag, xsdObject.nameSpace)
    }

    /** ≈ SelectSchemaElement(DiagramItem). */
    private fun selectSchemaElement(diagramBase: DiagramItem) =
        selectSchemaElement(diagramBase.tabSchema, diagramBase.nameSpace)

    /** ≈ SelectSchemaElement(XMLSchema.openAttrs, string nameSpace). */
    fun selectSchemaElement(openAttrs: Any?, nameSpace: String) {
        // ≈ propertyGridSchemaObject.SelectedObject = openAttrs.
        fillSchemaObjectGrid(openAttrs)
        showDocumentation(null)

        if (openAttrs is xsddiagramx.parsing.Annotated) {
            // Documentación del elemento.
            if (openAttrs.annotation != null) {
                showDocumentation(openAttrs.annotation)
            }

            // Atributos (listViewAttributes).
            listViewAttributes.items.clear()
            val listAttributes: List<XSDAttribute> =
                DiagramHelpers.getAnnotatedAttributes(schema, openAttrs, nameSpace).toMutableList()
            for (attribute in listAttributes) {
                // dgis fix github issue 2: texto de facetas del simpleType.
                var facetsText = ""
                val sItem = attribute.tag?.simpleType?.item
                if (sItem is xsddiagramx.parsing.SimpleRestriction) {
                    for (facet in sItem.facets) {
                        facetsText += "${facet.kind}(${facet.value ?: ""});"
                    }
                }
                listViewAttributes.items.add(
                    AttributesRow(
                        attribute.name,
                        attribute.type,
                        attribute.use ?: "",
                        attribute.defaultValue ?: "",
                        facetsText,
                        attribute
                    )
                )
            }

            // Enumeraciones (listViewEnumerate).
            showEnumerate(openAttrs)
        }
    }

    // ------------------------------------------------------------------
    // Enumeración y documentación (≈ ShowEnumerate / ShowDocumentation)
    // ------------------------------------------------------------------

    /** ≈ ShowEnumerate(XMLSchema.annotated). */
    private fun showEnumerate(annotated: xsddiagramx.parsing.Annotated?) {
        listViewEnumerate.items.clear()

        val element = annotated as? xsddiagramx.parsing.element ?: return
        val type = element.type ?: return
        val xsdObject = schema.elementsByName[
            DiagramHelpers.qualifiedNameToFullName("type", type)
        ]
        val tag = xsdObject?.tag
        if (tag is xsddiagramx.parsing.simpleType) {
            showEnumerateOfSimpleType(tag)
        }
    }

    /** ≈ ShowEnumerate(XMLSchema.attribute). (Botón derecho/menú en el original.) */
    fun showEnumerateOfAttribute(attribute: xsddiagramx.parsing.attribute?) {
        listViewEnumerate.items.clear()
        if (attribute != null) {
            if (attribute.type != null) {
                val xsdObject = schema.elementsByName[
                    DiagramHelpers.qualifiedNameToFullName("type", attribute.type!!)
                ]
                val tag = xsdObject?.tag
                if (tag is xsddiagramx.parsing.simpleType) {
                    showEnumerateOfSimpleType(tag)
                }
            } else {
                val inlineType = attribute.simpleType
                if (inlineType != null) showEnumerateOfSimpleType(inlineType)
            }
        }
    }

    /** ≈ ShowEnumerate(XMLSchema.simpleType): lista los facets de tipo enumeration. */
    private fun showEnumerateOfSimpleType(simpleType: xsddiagramx.parsing.simpleType) {
        val item = simpleType.item
        if (item is xsddiagramx.parsing.SimpleRestriction) {
            for (facet in item.facets) {
                if (facet.kind == "enumeration") {
                    listViewEnumerate.items.add(facet.value ?: "")
                }
            }
        }
    }

    /** ≈ ShowDocumentation(XMLSchema.annotation): pinta en el TextArea. */
    private fun showDocumentation(annotation: xsddiagramx.parsing.annotation?) {
        if (annotation == null) {
            textBoxAnnotation.text = null
        } else {
            textBoxAnnotation.text = DiagramHelpers.getAnnotationText(annotation)
        }
    }

    // ------------------------------------------------------------------
    // Búsqueda (≈ toolStripTextBoxSearch_KeyPress / Search)
    // ------------------------------------------------------------------

    private fun search() {
        val searchedItem = diagram.search(toolStripTextBoxSearch.text)
        if (searchedItem != null) {
            selectDiagramElement(searchedItem)
            updateDiagram()
            panelDiagram.scrollTo(diagram.scalePoint(searchedItem.location), true)
            setStatus(
                "Search: '${toolStripTextBoxSearch.text}' (${diagram.actualSearchHit} of ${diagram.searchHits} hits)"
            )
        } else {
            setStatus("Find: Can't find the text '${toolStripTextBoxSearch.text}'")
        }
    }

    /** ≈ MainForm_KeyUp: Ctrl+0 resetea el zoom, Ctrl+F enfoca el buscador. */
    private fun mainForm_KeyUp(event: KeyEvent) {
        if (event.isControlDown) {
            when (event.code) {
                javafx.scene.input.KeyCode.DIGIT0, javafx.scene.input.KeyCode.NUMPAD0 ->
                    toolStripComboBoxZoom.selectionModel.select(8)
                javafx.scene.input.KeyCode.F -> {
                    toolStripTextBoxSearch.requestFocus()
                    toolStripTextBoxSearch.selectAll()
                }
                else -> {}
            }
        }
    }

    init {
        // (El repintado al redimensionar lo hace DiagramControlContainer con sus
        // bindings de tamaño; aquí sería redundante y hoy es lo que congelaba.)
    }
}

/** Copia al portapapeles el texto indicado (≈ CopyLine/CopyList del C#). */
internal fun copyToClipboard(text: String) {
    val content = ClipboardContent().apply { putString(text) }
    Clipboard.getSystemClipboard().setContent(content)
}
