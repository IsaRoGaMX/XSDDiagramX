//    XSD Diagram - Kotlin/JavaFX port
//    ComplexAnyAttribute.kt: subclase de [ComplexItem] para xs:anyAttribute dentro de
//    xs:complexType (equivale a XMLSchema.wildcard en XMLSchema.cs).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/** Caso `anyAttribute` dentro de un xs:complexType; envuelve el [wildcard] hijo. */
class ComplexAnyAttribute(
    /** El xs:anyAttribute (representado como [wildcard]). */
    val value: wildcard,
) : ComplexItem()
