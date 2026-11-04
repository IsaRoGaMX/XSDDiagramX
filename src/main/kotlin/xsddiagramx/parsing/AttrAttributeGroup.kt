//    XSD Diagram - Kotlin/JavaFX port
//    AttrAttributeGroup.kt: subclase de [AttrItem] para xs:attributeGroup directo
//    dentro de xs:extension / xs:restriction / xs:attributeGroup
//    (equivale a XMLSchema.attributeGroup).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** Caso `attributeGroup` directo; envuelve el [attributeGroup] (ref o declaración). */
class AttrAttributeGroup(
    /** El xs:attributeGroup referenciado o declarado. */
    val value: attributeGroup,
) : AttrItem()
