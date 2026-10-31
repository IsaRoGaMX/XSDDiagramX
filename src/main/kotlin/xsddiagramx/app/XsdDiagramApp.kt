//    XSD Diagram - Kotlin/JavaFX port
//    Port de Program.cs (Application.Run(new MainForm())) para la rama GUI.
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.app

import javafx.application.Application
import javafx.stage.Stage
import xsddiagramx.Options

/**
 * Aplicación JavaFX (≈ Program.cs): parsea la línea de comandos en Options,
 * asigna el icono de la ventana/taskbar/Dock y muestra el MainForm. Al cerrar
 * persiste los Settings (≈ MainForm_FormClosed).
 */
class XsdDiagramApp : Application() {

    /** Ventana principal (referencia para el stop). */
    private var stage: Stage? = null

    /** ≈ Main(): punto de entrada de la GUI. */
    override fun start(stage: Stage) {
        this.stage = stage
        // ≈ Options (parseo de la línea de comandos del C# con
        //   Environment.GetCommandLineArgs()).
        Options.init(parameters.raw)

        // Equivale a Application.Run(new MainForm()) del C#... pero antes, el
        // icono del MainForm (Form Icon property con XSDDiagram.ico):
        // xsddiagram.png (512x512) en resources; JavaFX no lee .ico y
        // Stage.icons lo usa en title bar/taskbar Windows-Linux y Dock de macOS.
        XsdDiagramApp::class.java.getResource("/xsddiagramx/app/xsddiagram.png")?.let { res ->
            stage.icons.add(javafx.scene.image.Image(res.toExternalForm()))
        }
        MainForm().showOn(stage)
    }

    /**
     * ≈ MainForm_FormClosed: persistir Settings al salir (Application.stop
     * corre al cerrar la última ventana).
     */
    override fun stop() {
        Settings.save()
        super.stop()
    }
}

/** Lanzador estándar (necesario porque la app no hereda de Program). */
fun main(args: Array<String>) {
    Application.launch(XsdDiagramApp::class.java, *args)
}
