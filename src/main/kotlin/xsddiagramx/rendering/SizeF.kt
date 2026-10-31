//    XSD Diagram - Kotlin/JavaFX port
//    SizeF.kt: tamaño flotante derivado de System.Drawing.SizeF del modelo original.
//    Origen C#: sustituye a System.Drawing.SizeF, resultado de Graphics.MeasureString
//    en DiagramItem.cs (medida de texto del layout).
//
//    Licencias del original: LGPL / MS-PL. Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización).
package xsddiagramx.rendering

/**
 * Tamaño flotante, resultado de la medición de texto de DrawingContext.measureString,
 * equivalente a System.Drawing.SizeF tal como lo usaba DiagramItem.cs.
 */
data class SizeF(
    /** Ancho medido en píxeles (≈ SizeF.Width). */
    val width: Double = 0.0,

    /** Alto medido en píxeles (≈ SizeF.Height). */
    val height: Double = 0.0,
) {
    /** Depuración, con el formato "C# ToString()" del struct SizeF original. */
    override fun toString(): String = "{Width=$width, Height=$height}"
}
