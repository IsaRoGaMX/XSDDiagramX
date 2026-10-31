//    XSD Diagram - Kotlin/JavaFX port
//    Port de SettingsForm.cs + SettingsForm.Designer.cs
//
//    Licencias del original: LGPL / MS-PL (véase LICENSE-LGPL.txt / LICENSE-MS-PL.txt)
//
//    Diferencias: el PropertyGrid del WinForms no existe en JavaFX; a petición
//    del usuario la ventana es un FORMULARIO (GridPane) con un control por
//    setting en vez de la tabla Property/Value anterior. Contiene las mismas
//    propiedades Browsable del C# (AlwaysShowOccurence / ShowType /
//    CompactLayoutDensity) más el campo "Tema" (adición del port, consume
//    xsddiagramx.AppTheme). Cada cambio dispara ChangeSetting en el MainForm
//    (≈ propertyGrid1_PropertyValueChanged).
//
//    Tema: al cambiar entre Claro/Oscuro se aplican en caliente los
//    stylesheets al propio formulario y a la ventana principal.
package xsddiagramx.app

import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.stage.Stage
import xsddiagramx.AppTheme

/**
 * ≈ SettingsForm: ventana no modal con el formulario del Settings.
 * (FormStartPosition.CenterParent → se centra sobre el owner.)
 */
class SettingsForm(private val owner: Stage) : Stage() {

    init {
        title = "Settings"
        isResizable = true

        // ≈ el editor del PropertyGrid: al cambiar un control, persistir el
        // setting y notificar al MainForm (ChangeSetting).
        fun onSetting(name: String) {
            MainForm.lastForm?.changeSetting(name)
        }

        val chkAlwaysShowOccurence = CheckBox("Always show the occurrence").apply {
            isSelected = Settings.alwaysShowOccurence
            setOnAction {
                Settings.alwaysShowOccurence = isSelected
                onSetting("AlwaysShowOccurence")
            }
        }

        val chkShowType = CheckBox("Show the type").apply {
            isSelected = Settings.showType
            setOnAction {
                Settings.showType = isSelected
                onSetting("ShowType")
            }
        }

        val chkCompactLayoutDensity = CheckBox("Compact layout density").apply {
            isSelected = Settings.compactLayoutDensity
            setOnAction {
                Settings.compactLayoutDensity = isSelected
                onSetting("CompactLayoutDensity")
            }
        }

        // Campo "Tema": Claro (esquema del original) / Oscuro (grises neutros).
        val comboTheme = ComboBox(FXCollections.observableArrayList(AppTheme.Mode.entries)).apply {
            setCellFactory { _ -> ThemeListCell() }
            setButtonCell(ThemeListCell())
            value = AppTheme.currentMode()
            maxWidth = Double.MAX_VALUE
            setOnAction {
                val mode = selectionModel.selectedItem ?: return@setOnAction
                if (mode == AppTheme.currentMode()) return@setOnAction
                Settings.theme = mode.label
                // Cambio en caliente: este formulario y la ventana principal.
                AppTheme.applyTo(scene)
                onSetting("Theme")
            }
        }

        val grid = GridPane().apply {
            hgap = 12.0
            vgap = 10.0
            padding = Insets(14.0)
            columnConstraints.addAll(
                ColumnConstraints().apply { minWidth = 190.0 },
                ColumnConstraints(200.0).apply { hgrow = Priority.SOMETIMES }
            )
            addRow(0, Label("Always Show Occurence"), chkAlwaysShowOccurence)
            addRow(1, Label("Show Type"), chkShowType)
            addRow(2, Label("CompactLayoutDensity"), chkCompactLayoutDensity)
            addRow(3, Label("Tema"), comboTheme)
            GridPane.setHgrow(chkAlwaysShowOccurence, Priority.ALWAYS)
            GridPane.setHgrow(chkShowType, Priority.ALWAYS)
            GridPane.setHgrow(chkCompactLayoutDensity, Priority.ALWAYS)
            GridPane.setHgrow(comboTheme, Priority.ALWAYS)
        }

        val root = javafx.scene.layout.BorderPane(grid)
        root.padding = Insets(8.0)
        scene = Scene(root, 460.0, 260.0)
        AppTheme.applyTo(scene)
        initOwner(owner)
    }

    /** Celda del combo "Tema": muestra la etiqueta del modo (Claro/Oscuro). */
    private class ThemeListCell : javafx.scene.control.ListCell<AppTheme.Mode>() {
        override fun updateItem(item: AppTheme.Mode?, empty: Boolean) {
            super.updateItem(item, empty)
            text = item?.label
        }
    }
}
