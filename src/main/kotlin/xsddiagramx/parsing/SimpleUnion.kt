//    XSD Diagram - Kotlin/JavaFX port
//    SimpleUnion.kt: xs:union de un xs:simpleType (equivale a XMLSchema.union de
//    XMLSchema.cs en el original C#).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/** xs:union. */
class SimpleUnion(
    /** QNames de los memberTypes de la unión (lista vacía si no se declararon). */
    val memberTypes: List<QualifiedName?>,
) : SimpleTypeBody()
