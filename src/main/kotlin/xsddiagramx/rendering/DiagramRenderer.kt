//    XSD Diagram - Kotlin/JavaFX port
//    Port de XSDDiagrams\Rendering\DiagramRenderer.cs
//
//    Licencias del original: LGPL / MS-PL (véase LICENSE-LGPL.txt / LICENSE-MS-PL.txt)
//    Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización)
//
//    Base de los renderers "de salida" (TXT/SVG). A diferencia de los
//    renderers gráficos (DiagramGdiRenderer/DiagramJavaFxRenderer), éstos
//    recorren el diagrama en profundidad y escriben hacia un Writer.
package xsddiagramx.rendering

/**
 * ≈ DiagramRenderer: renderer abstracto que recorre el diagrama
 * (Begin/Render/End) y emite hacia un destino de texto.
 */
abstract class DiagramRenderer {

    /** ≈ Name (identificador del formato, p.ej. "TXT"/"SVG"). */
    abstract val name: String

    /** ≈ BeginItemsRender: cabecera del formato (una vez por Render(diagram)). */
    abstract fun beginItemsRender()

    /** ≈ Render(Diagram): recorre el diagrama (raíces) y emite cada ítem. */
    abstract fun render(diagram: Diagram)

    /** ≈ Render(DiagramItem): emite un ítem. */
    abstract fun render(drawingItem: DiagramItem)

    /** ≈ EndItemsRender: pie/cierre del formato. */
    abstract fun endItemsRender()
}
