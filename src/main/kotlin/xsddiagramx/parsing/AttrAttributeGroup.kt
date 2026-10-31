//    XSD Diagram - Kotlin/JavaFX port
//    AttrAttributeGroup.kt: subclase de [AttrItem] para xs:attributeGroup directo
//    dentro de xs:extension / xs:restriction / xs:attributeGroup
//    (equivale a XMLSchema.attributeGroup).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/** Caso `attributeGroup` directo; envuelve el [attributeGroup] (ref o declaración). */
class AttrAttributeGroup(
    /** El xs:attributeGroup referenciado o declarado. */
    val value: attributeGroup,
) : AttrItem()
