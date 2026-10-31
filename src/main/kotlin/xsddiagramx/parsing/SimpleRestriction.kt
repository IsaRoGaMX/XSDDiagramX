//    XSD Diagram - Kotlin/JavaFX port
//    SimpleRestriction.kt: xs:restriction de un xs:simpleType (equivale a
//    XMLSchema.restriction + facet de XMLSchema.cs; sólo se conservan base y facetas).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/** xs:restriction simple. Sólo se necesita el base + lista de facetas (sin procesar). */
class SimpleRestriction(
    /** QName del tipo base restringido (o null si el XML no lo trae). */
    val base: QualifiedName?,
) : SimpleTypeBody() {
    /** Facetas de la restricción (enumeration, pattern, length...), en document order. */
    val facets: MutableList<facet> = ArrayList()
}
