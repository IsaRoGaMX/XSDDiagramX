//    XSD Diagram - Kotlin/JavaFX port
//    Annotated.kt: base de los componentes que pueden llevar xs:annotation
//    (equivale a XMLSchema.annotated en el original C#).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/** Equivale a XMLSchema.annotated: cualquier componente que puede llevar xs:annotation. */
open class Annotated : OpenAttrs() {
    /** xs:annotation asociada al componente (o null si el documento no la trae). */
    var annotation: annotation? = null
}
