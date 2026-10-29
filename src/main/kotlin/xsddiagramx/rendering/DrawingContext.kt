//    XSD Diagram - Kotlin/JavaFX port
//
//    Abstracción mínima de System.Drawing.Graphics que el modelo de dominio
//    necesita para calcular el layout (Diagram.Layout / DiagramItem.GenerateMeasure
//    en el original): sólo medir texto y resolver la Bandeja de fuentes que
//    el diagrama definía con "Arial" (10px bold / 9px normal / 10px normal).
//
//    Cada renderer concreto lo implementará utilizando su propia API de medición
//    (AWT Graphics2D, JavaFX FontLoader, métricas del SVG...), de modo que el
//    dominio no dependa ni de GDI ni de JavaFX.
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.rendering

/**
 * Descripción de una fuente. Sustituye a System.Drawing.Font en el modelo;
 * el renderer concreto la resolverá a una fuente real (AWT/JavaFX/CSS).
 */
data class FontDescription(
    /** Equivale a fontName; el original usaba "Arial". */
    val family: String = "Arial",
    /** Tamaño en píxeles (GraphicsUnit.Pixel, como en el original). */
    val sizePx: Float = 10f,
    /** Equivale a FontStyle.Bold. */
    val bold: Boolean = false,
)

/**
 * Contexto de medición. Equivale al `Graphics g` que recibían Layout(g) y
 * GenerateMeasure(g) del original: sólo necesitan MeasureString.
 */
interface DrawingContext {
    fun measureString(text: String, font: FontDescription): SizeF
}
