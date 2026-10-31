//    XSD Diagram - Kotlin/JavaFX port
//    ComplexComplexType.kt: subclase de [ComplexItem] para un xs:complexType anidado
//    dentro de otro (equivale a los casos nestedcomplexType de XMLSchema.cs).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/** Caso `complexType` (anidado, no top-level) dentro de un xs:complexType; envuelve el [complexType] hijo. */
class ComplexComplexType(
    /** El xs:complexType anidado. */
    val value: complexType,
) : ComplexItem()
