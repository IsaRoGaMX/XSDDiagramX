//    XSD Diagram - Kotlin/JavaFX port
//    Port de XSDDiagrams\Rendering\DiagramExporter.cs
//
//    Licencias del original: LGPL / MS-PL (véase LICENSE-LGPL.txt / LICENSE-MS-PL.txt)
//    Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización)
//
//    Diferencias:
//      - Formato .emf omitido (metafiles GDI+ de Windows; sin equivalente en
//        Java. El original también lo saltaba en Mono).
//      - PNG/JPG: canvas offscreen JavaFX → snapshot → BufferedImage → ImageIO
//        (JDK, sin dependencias de terceros). El C# pintaba en con Graphics GDI+.
package xsddiagramx.rendering

import xsddiagramx.rendering.javafx.DiagramJavaFxRenderer
import java.awt.image.BufferedImage
import java.io.File
import java.io.OutputStream
import javax.imageio.ImageIO

/** ≈ DiagramAlertHandler: confirmación para imágenes gigantes (true = continuar). */
typealias DiagramAlertHandler = (title: String, message: String) -> Boolean

/**
 * ≈ DiagramExporter. DEBE invocarse en el hilo de la aplicación JavaFX
 * (Layout/Snapshot rápidos y medición de textos están acoplados al FX Thread,
 * igual que el C# requería el hilo de GUI para Graphics).
 */
class DiagramExporter(private val diagram: Diagram) {

    /** ≈ Export(outputFilename, referenceGraphics, alerteDelegate, parameters). */
    fun export(
        outputFilename: String,
        referenceContext: xsddiagramx.rendering.javafx.JavaFxDrawingContext,
        alertDelegate: DiagramAlertHandler?,
        specificRendererParameters: Map<String, Any?> = emptyMap(),
    ): Boolean {
        var extension = outputFilename.substringAfterLast('.', "").lowercase()
        if (extension.isEmpty()) {
            extension = "svg"
            // ≈ el C# añade ".svg" al nombre en ese caso.
        }
        val sepFilename = if (outputFilename.contains('.')) outputFilename else "$outputFilename.svg"
        File(sepFilename).outputStream().use { stream ->
            return export(stream, extension, referenceContext, alertDelegate, specificRendererParameters)
        }
    }

    /** ≈ Export(Stream, extension, referenceGraphics, alerteDelegate, parameters). */
    fun export(
        stream: OutputStream,
        extension: String,
        referenceContext: xsddiagramx.rendering.javafx.JavaFxDrawingContext,
        alertDelegate: DiagramAlertHandler?,
        specificRendererParameters: Map<String, Any?> = emptyMap(),
    ): Boolean {
        val ext = extension.lowercase()
        val result: Boolean

        // Nota: .emf omitido (metafiles GDI+ de Windows; sin equivalente en Java.
        // El original también lo saltaba la rama Mono).
        result = when (ext) {
            "png", "jpg", "jpeg" -> {
                val bbox = diagram.scaleRectangle(diagram.boundingBox)
                var bypassAlert = true
                if (alertDelegate != null && (bbox.width > 10000 || bbox.height > 10000))
                    bypassAlert = alertDelegate(
                        "Huge image generation",
                        "Do you agree to generate a ${bbox.width}x${bbox.height} image?"
                    )
                if (bypassAlert) {
                    val format = if (ext == "png") "png" else "jpg"
                    savePngJpg(stream, bbox.width, bbox.height, format)
                    true
                } else false
            }

            "txt", "csv" -> {
                val scaleSave = diagram.scale
                try {
                    diagram.scale = 1.0f
                    diagram.layout(referenceContext)
                    stream.writer().use { sw ->
                        val renderer = DiagramTxtRenderer(sw)
                        renderer.isCSV = ext == "csv"
                        if (specificRendererParameters.containsKey("TextOutputFields")) {
                            val o = specificRendererParameters["TextOutputFields"]
                            if (o is List<*>) renderer.setTextOutputFields(o.filterIsInstance<String>())
                        }
                        if (specificRendererParameters.containsKey("DisplayAttributes")) {
                            val o = specificRendererParameters["DisplayAttributes"]
                            if (o is Boolean) renderer.setDisplayAttributes(o)
                        }
                        if (specificRendererParameters.containsKey("Schema")) {
                            val o = specificRendererParameters["Schema"]
                            renderer.setSchema(o as? xsddiagramx.parsing.Schema)
                        }
                        renderer.render(diagram)
                        sw.flush()
                    }
                    true
                } finally {
                    diagram.scale = scaleSave
                    diagram.layout(referenceContext)
                }
            }

            else -> { // ≈ ".svg"
                val scaleSave = diagram.scale
                try {
                    diagram.scale = 1.0f
                    diagram.layout(referenceContext)
                    stream.writer().use { sw ->
                        DiagramSvgRenderer(sw, referenceContext).render(diagram)
                        sw.flush()
                    }
                    true
                } finally {
                    diagram.scale = scaleSave
                    diagram.layout(referenceContext)
                }
            }
        }
        return result
    }

    /**
     * ≈ el trozo del .png/.jpg: canvas offscreen + renderer + ImageIO.
     * Debe correr en el FX Thread.
     */
    private fun savePngJpg(stream: OutputStream, width: Int, height: Int, format: String) {
        val canvas = javafx.scene.canvas.Canvas(width.toDouble(), height.toDouble())
        val g = canvas.graphicsContext2D
        // El exportado es un documento: siempre "papel" blanco (paleta clara),
        // aunque la app esté en tema Oscuro.
        g.fill = javafx.scene.paint.Color.WHITE
        g.fillRect(0.0, 0.0, canvas.width, canvas.height)
        DiagramJavaFxRenderer(g, xsddiagramx.AppTheme.LIGHT).render(diagram)

        // snapshot → BufferedImage (TYPE_INT_RGB para JPEG sin alpha).
        val snapshot = canvas.snapshot(null, null)
        val reader = snapshot.pixelReader
        val image = BufferedImage(
            width.coerceAtLeast(1), height.coerceAtLeast(1),
            if (format == "png") BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
        )
        for (y in 0 until height) {
            for (x in 0 until width) {
                image.setRGB(x, y, reader.getArgb(x, y))
            }
        }
        ImageIO.write(image, format, stream)
    }
}
