//    XSD Diagram - Kotlin/JavaFX port
//    JavaFxDrawingContext.kt: implementación JavaFX de DrawingContext para calcular
//    el layout del diagrama (Diagram.Layout).
//    Origen C#: implementaría ≈ System.Drawing.Graphics.MeasureString, que el C#
//    pasaba como `Graphics g` a Layout / GenerateMeasure.
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
//    Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización).
package xsddiagramx.rendering.javafx

import xsddiagramx.rendering.DrawingContext
import xsddiagramx.rendering.FontDescription
import xsddiagramx.rendering.SizeF

/**
 * ≈ Graphics.MeasureString. Debe invocarse en el Event Thread de JavaFX
 * (MainForm.UpdateDiagram lo hace desde un manejador de eventos).
 */
class JavaFxDrawingContext : DrawingContext {

    /** Mide el texto vía TextMeasure (caché de layoutBounds de nodos TEMP). */
    override fun measureString(text: String, font: FontDescription): SizeF =
        SizeF(TextMeasure.width(text, font), TextMeasure.height(font))
}
