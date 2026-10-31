//    XSD Diagram - Kotlin/JavaFX port
//    ComplexAttribute.kt: subclase de [ComplexItem] para xs:attribute dentro de
//    xs:complexType (equivale a XMLSchema.attribute en XMLSchema.cs).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/** Caso `attribute` dentro de un xs:complexType; envuelve el [attribute] hijo. */
class ComplexAttribute(
    /** El xs:attribute declarado. */
    val value: attribute,
) : ComplexItem()
