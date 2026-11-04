//    XSD Diagram - Kotlin/JavaFX port
//    Diálogo de progreso de descarga de dependencias (xs:include / xs:import
//    remotas). Nuevo en este port: el C# cargaba en el hilo de la GUI y se
//    quedaba congelado; aquí la carga corre en un hilo de fondo y este diálogo
//    (APPLICATION_MODAL: impide volver a la ventana principal pero no congela
//    la aplicación) muestra las descargas y ofrece un botón "Cancelar" que
//    aborta la descarga y la apertura del fichero.
//
//    Licencias: LGPL (MainForm) / MS-PL para los ports 1:1.
package xsddiagramx.app

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.Window

/**
 * Diálogo modal de progreso. Métodos seguros para hilo de carga (los eventos
 * llegan del hilo de descarga): internamente hace [Platform.runLater].
 */
class SchemaDownloadDialog(
    owner: Window?,
    onCancelRequested: () -> Unit,
) {

    private val logListView = ListView<String>(FXCollections.observableArrayList())

    private val currentLabel = Label("(no hay descargas en curso)")

    private val progressLabel = Label("Dependencias descargadas: 0")

    private val progressIndicator = ProgressIndicator().apply {
        // progress = -1 (INDETERMINATE_PROGRESS) es el valor por defecto.
        prefWidth = 34.0
        prefHeight = 34.0
    }

    private val cancelButton = Button("Cancelar").apply {
        setOnAction {
            isDisable = true
            currentLabel.text = "Cancelando..."
            onCancelRequested()
        }
    }

    private val stage = Stage()

    private var downloadsDone = 0
    private val pendingEvents = ArrayList<String>()

    init {
        stage.initModality(Modality.APPLICATION_MODAL)
        if (owner != null) stage.initOwner(owner)
        stage.title = "Loading schema..."
        stage.isResizable = false

        val content = VBox(8.0).apply {
            padding = Insets(12.0)
            children.addAll(
                HBox(8.0).apply {
                    alignment = Pos.CENTER_LEFT
                    children.addAll(progressIndicator, currentLabel)
                },
                progressLabel,
                logListView.also { VBox.setVgrow(it, Priority.ALWAYS) },
                HBox().apply {
                    alignment = Pos.CENTER_RIGHT
                    children.add(cancelButton)
                }
            )
        }

        stage.scene = Scene(content, 620.0, 340.0)
        xsddiagramx.AppTheme.applyTo(stage.scene)
        // El usuario NO puede cerrarlo con la X: sólo con "Cancelar".
        stage.setOnCloseRequest { event -> event.consume() }
    }

    /** Muestra el diálogo (no bloquea el hilo: modality ≠ showAndWait). */
    fun show() {
        stage.show()
        val events = synchronized(pendingEvents) {
            pendingEvents.toList().also { pendingEvents.clear() }
        }
        for (line in events) logListView.items.add(line)
    }

    /** Encola un mensaje del hilo de descarga o lo muestra ya si el diálogo está visible. */
    private fun queue(line: String) {
        // Llega del hilo de carga: encola; si el diálogo ya está en pantalla,
        // lo refleja en el hilo de la GUI.
        val shown = stage.isShowing
        if (!shown) {
            synchronized(pendingEvents) { pendingEvents.add(line) }
        } else {
            Platform.runLater { logListView.items.add(line) }
        }
    }

    /** ≈ DownloadProgressChanged: muestra el recurso que se está bajando. */
    fun onDownloadStart(url: String) {
        Platform.runLater {
            currentLabel.text = "Descargando: $url"
            progressIndicator.isVisible = true
        }
    }

    /** Marca una descarga terminada (con o sin error) y repinta el progreso. */
    fun onDownloadEnd(failed: Boolean) {
        Platform.runLater {
            downloadsDone++
            progressLabel.text = "Dependencias descargadas: $downloadsDone" +
                if (failed) " (con errores)" else ""
            currentLabel.text = "(no hay descargas en curso)"
        }
    }

    /** ≈ logEntry: agrega una línea al registro visible de progreso. */
    fun log(line: String) = queue(line)

    /** Cierra el diálogo (se llama solo en el FX Thread). */
    fun close() {
        stage.hide()
    }
}
