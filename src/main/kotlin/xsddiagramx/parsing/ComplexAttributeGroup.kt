//    XSD Diagram - Kotlin/JavaFX port
//    ComplexAttributeGroup.kt: subclase de [ComplexItem] para xs:attributeGroup dentro
//    de xs:complexType (equivale a XMLSchema.attributeGroup en XMLSchema.cs).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/** Caso `attributeGroup` dentro de un xs:complexType; envuelve el [attributeGroup] (ref o declaración). */
class ComplexAttributeGroup(
    /** El xs:attributeGroup referenciado o declarado. */
    val value: attributeGroup,
) : ComplexItem()
