//    XSD Diagram - Kotlin/JavaFX port
//    FontDescription.kt: descripción de fuente que el modelo usa en lugar de System.Drawing.Font.
//    Origen C#: sustituye a System.Drawing.Font y a las propiedades Font/SmallFont/
//    DocumentationFont de Diagram.cs y DiagramItem.cs.
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
//    Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización).
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
