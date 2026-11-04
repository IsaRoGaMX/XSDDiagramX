//    XSD Diagram - Kotlin/JavaFX port
//    ComplexAttribute.kt: subclase de [ComplexItem] para xs:attribute dentro de
//    xs:complexType (equivale a XMLSchema.attribute en XMLSchema.cs).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** Caso `attribute` dentro de un xs:complexType; envuelve el [attribute] hijo. */
class ComplexAttribute(
    /** El xs:attribute declarado. */
    val value: attribute,
) : ComplexItem()
