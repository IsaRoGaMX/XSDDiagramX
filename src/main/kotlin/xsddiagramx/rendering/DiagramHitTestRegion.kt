//    XSD Diagram - Kotlin/JavaFX port
//    DiagramHitTestRegion.kt: enum de la parte del nodo que fue golpeada en el hit-test.
//    Origen C#: XSDDiagrams\Rendering\DiagramHitTestRegion.cs.
//
//    Licencias del original: LGPL / MS-PL. Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización).
package xsddiagramx.rendering

/**
 * Indica qué parte del nodo fue pulsada durante el hit-test.
 * ≈ `XSDDiagrams.Rendering.DiagramHitTestRegion` (DiagramHitTestRegion.cs, [Serializable]).
 */
enum class DiagramHitTestRegion {
    /** El punto no cayó sobre el nodo (≈ None). */
    None,

    /** El punto cayó dentro del bounding box, pero fuera de la caja del elemento (≈ BoundingBox). */
    BoundingBox,

    /** El punto cayó sobre la caja del elemento (≈ Element). */
    Element,

    /** El punto cayó sobre el botón de expandir/colapsar hijos (≈ ChildExpandButton). */
    ChildExpandButton
}
