//    XSD Diagram - Kotlin/JavaFX port
//    ComplexComplexType.kt: subclase de [ComplexItem] para un xs:complexType anidado
//    dentro de otro (equivale a los casos nestedcomplexType de XMLSchema.cs).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** Caso `complexType` (anidado, no top-level) dentro de un xs:complexType; envuelve el [complexType] hijo. */
class ComplexComplexType(
    /** El xs:complexType anidado. */
    val value: complexType,
) : ComplexItem()
