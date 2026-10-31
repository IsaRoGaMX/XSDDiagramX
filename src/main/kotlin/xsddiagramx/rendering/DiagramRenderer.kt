//    XSD Diagram - Kotlin/JavaFX port
//    DiagramRenderer.kt: clase base abstracta de los renderers "de salida"
//    (TXT/SVG) que recorren el diagrama en profundidad y emiten a texto.
//    Origen C#: port de XSDDiagrams\Rendering\DiagramRenderer.cs.
//
//    Licencias del original: LGPL / MS-PL. Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización).
package xsddiagramx.rendering

/**
 * ≈ DiagramRenderer: base de los renderers "de salida" (TXT/SVG). A diferencia de
 * los renderers gráficos (DiagramGdiRenderer/DiagramJavaFxRenderer), éstos
 * recorren el diagrama en profundidad (Begin/Render/End) y escriben hacia un
 * Writer, no pintan sobre un contexto gráfico.
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
