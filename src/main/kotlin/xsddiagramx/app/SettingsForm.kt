//    XSD Diagram - Kotlin/JavaFX port
//    Port de SettingsForm.cs + SettingsForm.Designer.cs
//
//    Licencias del original: LGPL / MS-PL (véase LICENSE-LGPL.txt / LICENSE-MS-PL.txt)
//
//    Diferencias: el PropertyGrid del WinForms no existe en JavaFX. La ventana
//    muestra en una tabla Property/Value las mismas propiedades que quedaban
//    visibles en el PropertyGrid del C#: sólo las Browsable del Settings
//    (AlwaysShowOccurence / ShowType / CompactLayoutDensity; DisplayPanel,
//    Zoom, Alignement y ShowDocumentation tienen Browsable(false) y el
//    PropertyGrid del original tampoco las listaba). Cada cambio dispara
//    ChangeSetting en el MainForm (≈ propertyGrid1_PropertyValueChanged).
package xsddiagramx.app

import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import javafx.stage.Stage

/** Fila del PropertyGrid (≈ PropertyItem del WinForms, sólo booleans). */
class SettingRow(
    /** ≈ nombre de la propiedad (con él se llama a ChangeSetting). */
    val name: String,
    /** ≈ DisplayName del atributo del Settings. */
    val label: String,
    read: () -> Boolean,
    write: (Boolean) -> Unit,
) {
    /** Escritura al togglear (≈ el editor del grid). */
    val value = SimpleBooleanProperty(read()).apply {
        addListener { _, _, newValue -> write(newValue) }
    }
}

/**
 * ≈ SettingsForm: ventana no modal con el "PropertyGrid" del Settings
 * (FormStartPosition.CenterParent → se centra sobre el owner).
 */
class SettingsForm(private val owner: Stage) : Stage() {

    init {
        title = "Settings"
        isResizable = true

        val rows = listOf(
            SettingRow("AlwaysShowOccurence", "Always Show Occurence",
                read = { Settings.alwaysShowOccurence },
                write = {
                    Settings.alwaysShowOccurence = it
                    MainForm.lastForm?.changeSetting("AlwaysShowOccurence")
                }),
            SettingRow("ShowType", "Show Type",
                read = { Settings.showType },
                write = {
                    Settings.showType = it
                    MainForm.lastForm?.changeSetting("ShowType")
                }),
            SettingRow("CompactLayoutDensity", "CompactLayoutDensity",
                read = { Settings.compactLayoutDensity },
                write = {
                    Settings.compactLayoutDensity = it
                    MainForm.lastForm?.changeSetting("CompactLayoutDensity")
                }),
        )

        val nameColumn = TableColumn<SettingRow, String>("Property")
        nameColumn.cellValueFactory = PropertyValueFactory("label")
        nameColumn.prefWidth = 300.0

        val valueColumn = TableColumn<SettingRow, Boolean>("Value")
        // ≈ el editor booleano del PropertyGrid: CheckBox directo en la celda
        // (CheckBoxTableCell exige tabla editable; aquí el toggle es 1 clic).
        valueColumn.cellFactory = javafx.util.Callback {
            object : javafx.scene.control.TableCell<SettingRow, Boolean>() {
                val box = javafx.scene.control.CheckBox()
                init {
                    graphic = box
                    // El CheckBox no recibe el ratón (mouseTransparent): el
                    // toggle 1-clic lo hace la celda (≈ el PropertyGrid del C#,
                    // que editaba el boolean al clicar la celda).
                    box.isMouseTransparent = true
                    setOnMouseClicked {
                        val row = tableRow.item as? SettingRow
                        if (row != null) {
                            row.value.set(!row.value.get())
                            box.isSelected = row.value.get()
                        }
                    }
                }

                override fun updateItem(item: Boolean?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = null
                    // El item booleano de la celda no se usa (sin
                    // cellValueFactory): la autoridad es la fila (SettingRow).
                    val row = tableRow.item as? SettingRow
                    if (empty || row == null) {
                        box.isDisable = true
                        box.isSelected = false
                        return
                    }
                    box.isDisable = false
                    box.isSelected = row.value.get()
                }
            }
        }
        valueColumn.prefWidth = 90.0

        val grid = TableView<SettingRow>().apply {
            columns.addAll(nameColumn, valueColumn)
            items.addAll(rows)
            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
            placeholder = Label("No settings displayed")
        }

        val root = javafx.scene.layout.BorderPane(grid)
        root.padding = Insets(8.0)
        scene = Scene(root, 520.0, 438.0)
        initOwner(owner)
    }
}
