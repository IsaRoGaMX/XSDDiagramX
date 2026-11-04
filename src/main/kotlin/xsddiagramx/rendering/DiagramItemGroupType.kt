//    XSD Diagram - Kotlin/JavaFX port
//    DiagramItemGroupType.kt: enum del tipo de compositor/grupo que representa un nodo.
//    Origen C#: XSDDiagrams\Rendering\DiagramItemGroupType.cs.
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
//    Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización).
package xsddiagramx.rendering

/**
 * Tipo de compositor/grupo del nodo: `Group` para un xs:group referenciado o
 * definido, y Sequence / Choice / All para los compositores anidados (pattern
 * que en el C# se resolvía con Enum.Parse sobre ItemsElementName).
 * ≈ `XSDDiagrams.Rendering.DiagramItemGroupType` (DiagramItemGroupType.cs, [Serializable]).
 */
enum class DiagramItemGroupType {
    /** Nodo procedente de un xs:group (sin glifo específico; el C# tampoco lo dibuja). */
    Group,

    /** Compositor xs:sequence (glifo: línea horizontal con 3 bolas). */
    Sequence,

    /** Compositor xs:choice (glifo: ramas simétricas con 3 bolas). */
    Choice,

    /** Compositor xs:all (glifo: igual que choice pero con líneas rectas). */
    All
}
