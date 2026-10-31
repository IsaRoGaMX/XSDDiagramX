//    XSD Diagram - Kotlin/JavaFX port
//    DrawingContext.kt: interfaz de contexto de medición, equivalente mínimo de
//    System.Drawing.Graphics (`Graphics g`) que el dominio recibía en
//    Layout(g) / GenerateMeasure(g) del original.
//
//    Cada renderer concreto lo implementará utilizando su propia API de medición
//    (JavaFX FontLoader y Text.layoutBounds, métricas del SVG...), de modo que el
//    dominio no dependa ni de GDI ni de JavaFX.
//
//    Licencias del original: LGPL / MS-PL. Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización).
package xsddiagramx.rendering

/**
 * Contexto de medición. Equivale al `Graphics g` que recibían Layout(g) y
 * GenerateMeasure(g) del original: sólo necesitan MeasureString.
 */
interface DrawingContext {

    /**
     * Mide el ancho/alto del texto con la fuente dada.
     * ≈ Graphics.MeasureString(text, font) del C#.
     */
    fun measureString(text: String, font: FontDescription): SizeF
}
