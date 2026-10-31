//    XSD Diagram - Kotlin/JavaFX port
//    DiagramItemType.kt: enum del tipo de icono/caja que representa un nodo del diagrama.
//    Origen C#: XSDDiagrams\Rendering\DiagramItemType.cs.
//
//    Licencias del original: LGPL / MS-PL. Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización).
package xsddiagramx.rendering

/**
 * Tipo del icono/caja del nodo del diagrama: un elemento, un tipo complejo/simple
 * o un grupo/compositor.
 * ≈ `XSDDiagrams.Rendering.DiagramItemType` (DiagramItemType.cs, [Serializable]).
 */
enum class DiagramItemType(val value: String) {
    /** Caja rectangular de un xs:element (≈ element). */
    element("element"),

    /** Caja con bisel izquierdo de un xs:complexType / xs:simpleType (≈ type). */
    type("type"),

    /** Caja poligonal de un xs:group / compositor (sequence, choice, all) (≈ group). */
    group("group")
}
