//    XSD Diagram - Kotlin/JavaFX port
//    ComplexComplexContent.kt: subclase de [ComplexItem] para xs:complexContent dentro
//    de xs:complexType (equivale a XMLSchema.complexContent en XMLSchema.cs).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** Caso `complexContent` dentro de un xs:complexType; envuelve el [complexContent] hijo. */
class ComplexComplexContent(
    /** El xs:complexContent (extension o restriction). */
    val value: complexContent,
) : ComplexItem()
