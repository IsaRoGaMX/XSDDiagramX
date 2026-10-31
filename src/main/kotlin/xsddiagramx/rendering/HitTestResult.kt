//    XSD Diagram - Kotlin/JavaFX port
//    HitTestResult.kt: holder del resultado del hit-test entre Diagram/DiagramItem y la GUI.
//    Origen C#: no existe como clase; sustituye a los parámetros `out` de
//    Diagram.HitTest / DiagramItem.HitTest en Diagram.cs y DiagramItem.cs.
//
//    Licencias del original: LGPL / MS-PL. Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización).
package xsddiagramx.rendering

/**
 * Sustituye los parámetros `out DiagramItem element, out DiagramHitTestRegion region`
 * de las llamadas originales (Diagram.HitTest / DiagramItem.HitTest en sus .cs):
 * el hit-test recursivo va escribiendo aquí el elemento y la región encontrados.
 */
class HitTestResult(
    /** Nodo del diagrama golpeado (≈ parámetro `out element` del C#). */
    var element: DiagramItem? = null,

    /** Región del nodo golpeada (≈ parámetro `out region` del C#). */
    var region: DiagramHitTestRegion = DiagramHitTestRegion.None,
)
