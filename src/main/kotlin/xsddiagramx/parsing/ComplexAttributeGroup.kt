//    XSD Diagram - Kotlin/JavaFX port
//    ComplexAttributeGroup.kt: subclase de [ComplexItem] para xs:attributeGroup dentro
//    de xs:complexType (equivale a XMLSchema.attributeGroup en XMLSchema.cs).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** Caso `attributeGroup` dentro de un xs:complexType; envuelve el [attributeGroup] (ref o declaración). */
class ComplexAttributeGroup(
    /** El xs:attributeGroup referenciado o declarado. */
    val value: attributeGroup,
) : ComplexItem()
