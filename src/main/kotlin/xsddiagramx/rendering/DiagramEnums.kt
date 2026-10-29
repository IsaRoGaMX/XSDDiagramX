//    XSD Diagram - Kotlin/JavaFX port
//
//    Ports de los enums de XSDDiagrams\Rendering\ETS
//    (DiagramAlignement.cs, DiagramItemType.cs, DiagramItemGroupType.cs,
//    DiagramHitTestRegion.cs) + el holder del resultado del HitTest ya que
//    el C# usaba parámetros `out`.
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.rendering

/** DiagramAlignement: alineación vertical de los nodos en su caja contenedora. */
enum class DiagramAlignement { Near, Center, Far }

/** DiagramItemType: tipo del icono/caja del nodo del diagrama. */
enum class DiagramItemType(val value: String) {
    element("element"),
    type("type"),
    group("group")
}

/**
 * DiagramItemGroupType: `DiagramItemGroupType.Group` para un xs:group, y
 * Sequence / Choice / All para los compositores anidados.
 */
enum class DiagramItemGroupType { Group, Sequence, Choice, All }

/** DiagramHitTestRegion: qué parte del nodo fue pulsada. */
enum class DiagramHitTestRegion { None, Element, ChildExpandButton, BoundingBox }

/**
 * Sustituye los parámetros `out DiagramItem element, out DiagramHitTestRegion region`
 * de las llamadas original (Diagram.HitTest / DiagramItem.HitTest).
 */
class HitTestResult(
    var element: DiagramItem? = null,
    var region: DiagramHitTestRegion = DiagramHitTestRegion.None,
)
