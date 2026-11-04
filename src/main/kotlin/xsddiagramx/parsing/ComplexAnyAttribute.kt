//    XSD Diagram - Kotlin/JavaFX port
//    ComplexAnyAttribute.kt: subclase de [ComplexItem] para xs:anyAttribute dentro de
//    xs:complexType (equivale a XMLSchema.wildcard en XMLSchema.cs).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** Caso `anyAttribute` dentro de un xs:complexType; envuelve el [wildcard] hijo. */
class ComplexAnyAttribute(
    /** El xs:anyAttribute (representado como [wildcard]). */
    val value: wildcard,
) : ComplexItem()
