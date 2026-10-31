//    XSD Diagram - Kotlin/JavaFX port
//    IconResources.kt: utilidad para cargar los iconos 16px del MainForm
//    (los PNG van en Resources.resx del proyecto C#, aquí en /icons).
//
//    Licencias del original: LGPL (MainForm) / MS-PL.
package xsddiagramx.tools

import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ToggleButton
import javafx.scene.control.Tooltip
import javafx.scene.image.Image
import javafx.scene.image.ImageView

/** Caché de iconos de la barra de herramientas (≈ las imágenes del Resources
 *  del MainForm: ToolStripButton.Image y el texto como fallback). */
object IconResources {
    /** Crea un gráfico con el tamaño solicitado y un glifo de reserva si falta el recurso. */
    fun graphic(path: String, size: Double, fallback: String = ""): Node {
        val resource = IconResources::class.java.getResource("/icons/$path")
        if (resource == null) {
            return Label(fallback).apply {
                minWidth = size
                minHeight = size
                style = "-fx-font-size: " + (size * 0.7) + "px; -fx-alignment: center;"
            }
        }
        return ImageView(Image(resource.toExternalForm())).apply {
            fitWidth = size
            fitHeight = size
            isPreserveRatio = true
            isSmooth = true
        }
    }

    /** Construye un botón de barra de herramientas sin texto y con su tooltip. */
    fun toolbarButton(path: String, tooltip: String, action: (Button) -> Unit): Button =
        Button().apply {
            graphic = IconResources.graphic(path, 16.0, tooltip)
            this.tooltip = Tooltip(tooltip)
            accessibleText = tooltip
            setOnAction { action(this) }
        }

    /** ≈ toolbarToggleButton del MainForm: toggle de la toolbar sólo con icono. */
    fun toolbarToggleButton(path: String, tooltip: String, action: (Boolean) -> Unit): ToggleButton =
        ToggleButton().apply {
            graphic = IconResources.graphic(path, 16.0, tooltip)
            this.tooltip = Tooltip(tooltip)
            accessibleText = tooltip
            setOnAction { action(isSelected) }
        }
}