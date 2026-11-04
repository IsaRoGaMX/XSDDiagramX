//    XSD Diagram - Kotlin/JavaFX port
//    DiagramAlignement.kt: enum de alineación vertical de los nodos en su caja contenedora.
//    Origen C#: XSDDiagrams\Rendering\DiagramAlignement.cs.
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
//    Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización).
package xsddiagramx.rendering

/**
 * Alineación vertical de los nodos dentro de su caja contenedora.
 * ≈ `XSDDiagrams.Rendering.DiagramAlignement` (DiagramAlignement.cs, [Serializable]).
 */
enum class DiagramAlignement {
    /** El nodo se sitúa junto al borde superior de su caja contenedora (≈ Near). */
    Near,

    /** El nodo se centra verticalmente en su caja contenedora (≈ Center). */
    Center,

    /** El nodo se sitúa junto al borde inferior de su caja contenedora (≈ Far). */
    Far
}
