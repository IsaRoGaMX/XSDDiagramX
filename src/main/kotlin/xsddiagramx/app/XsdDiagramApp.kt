//    XSD Diagram - Kotlin/JavaFX port
//    Port de Program.cs (Application.Run(new MainForm())) para la rama GUI.
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.app

import javafx.application.Application
import javafx.stage.Stage
import xsddiagramx.Options

class XsdDiagramApp : Application() {

    private var stage: Stage? = null

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

        // Watchdog de diagnóstico (-Dxsddiagramx.fxwatch=1): vuelca el stack del
        // FX Thread a %TEMP%/opencode/xsddiagramx-fxwatch.log si el hilo se
        // queda congelado >750 ms (p.ej. durante un drag de resize nativo).
        FxWatchdog.install(stage)

        // Autodiagnóstico: -Dxsddiagramx.selftest=resize|maximize programa un
        // resize/maximizado por código tras arrancar. Opcionalmente
        // -Dxsddiagramx.selftest.file=<xsd> carga un esquema antes de actuar.
        val selftest = System.getProperty("xsddiagramx.selftest")
        if (selftest == "resize" || selftest == "maximize" || selftest == "scroll" ||
            selftest == "export" || selftest == "settings" || selftest == "validate" ||
            selftest == "theme"
        ) {
            val schemaFile = System.getProperty("xsddiagramx.selftest.file")
            val mainForm = MainForm.lastForm ?: return
            Thread {
                Thread.sleep(3000)
                if (schemaFile != null) {
                    println("SELFTEST: loading schema $schemaFile")
                    javafx.application.Platform.runLater { mainForm.loadSchema(schemaFile) }
                    // Esperar a que la carga (con dependencias) termine.
                    Thread.sleep(15000)
                }
                try {
                    if (selftest == "maximize") {
                        println("SELFTEST: maximizing at ${System.currentTimeMillis()}")
                        javafx.application.Platform.runLater { stage.isMaximized = true }
                        println("SELFTEST: maximize requested")
                    } else if (selftest == "export") {
                        // Autotest de la fase 3: Add all + expand y exportar
                        // svg/txt/png a la carpeta temporal del entorno.
                        println("SELFTEST: add all + expand to build a diagram")
                        javafx.application.Platform.runLater {
                            val out = mainForm.selftestExport("xsddiagramx-selftest")
                            println("SELFTEST-EXPORT: $out")
                            javafx.application.Platform.exit()
                        }
                    } else if (selftest == "settings") {
                        // Autotest fase 4: Settings + ChangeSetting.
                        println("SELFTEST: settings roundtrip")
                        javafx.application.Platform.runLater {
                            println("SELFTEST-SETTINGS: ${mainForm.selftestSettings()}")
                            javafx.application.Platform.exit()
                        }
                    } else if (selftest == "validate") {
                        // Autotest fase 4: validar XMLs contra el esquema cargado.
                        println("SELFTEST: xml validation")
                        javafx.application.Platform.runLater {
                            val result = mainForm.selftestValidate()
                            if (result == null) println("SELFTEST-VALIDATE: schema not loaded")
                            else println("SELFTEST-VALIDATE: $result")
                            javafx.application.Platform.exit()
                        }
                    } else if (selftest == "theme") {
                        // Autotest tema: cambio en caliente Claro→Oscuro→Claro
                        // y popups de los combos (abiertos por código: en los
                        // popups es donde Modena fallaba al alternar el tema).
                        println("SELFTEST: theme roundtrip + popups de combos")
                        try {
                            println("SELFTEST-THEME: ${mainForm.selftestTheme()}")
                        } catch (t: Throwable) {
                            println("SELFTEST ERROR: $t")
                        }
                        javafx.application.Platform.runLater { javafx.application.Platform.exit() }
                    } else if (selftest == "scroll") {
                        println("SELFTEST: add root + expand levels to make a big diagram")
                        javafx.application.Platform.runLater {
                            mainForm.selftestAddAll()
                            // Expandir varios niveles (≈ pulsar "Expand 1" x5).
                            for (i in 1..5) {
                                if (mainForm.selftestExpandOne()) println("SELFTEST: expanded level $i")
                            }
                            // Autotest del tab "Element" (enumeraciones de un atributo).
                            mainForm.selftestSelectAttribute("Moneda")
                            // Autotest del grid del tab "Element" (nodo Concepto).
                            mainForm.selftestShowElementTab("Concepto")
                        }
                        println("SELFTEST: expand sequence requested")
                    } else {
                        println("SELFTEST: starting incremental resize")
                        // ~100 pasos, como un drag real de esquina.
                        for (i in 1..100) {
                            Thread.sleep(16)
                            javafx.application.Platform.runLater {
                                stage.width = stage.width + 4.0
                                stage.height = stage.height + 3.0
                                if (i % 25 == 0) println("SELFTEST: step $i, width=${stage.width}")
                            }
                        }
                        println("SELFTEST: resize loop done")
                    }
                } catch (t: Throwable) {
                    println("SELFTEST ERROR: $t")
                }
            }.apply { isDaemon = true; start() }
        }
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

fun main(args: Array<String>) {
    Application.launch(XsdDiagramApp::class.java, *args)
}
