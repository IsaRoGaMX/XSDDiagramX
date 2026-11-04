//    XSD Diagram - Kotlin/JavaFX port
//    AttrAttribute.kt: subclase de [AttrItem] para xs:attribute directo dentro de
//    xs:extension / xs:restriction / xs:attributeGroup (equivale a XMLSchema.attribute).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** Caso `attribute` directo; envuelve el [attribute] hijo. */
class AttrAttribute(
    /** El xs:attribute declarado (o referenciado). */
    val value: attribute,
) : AttrItem()
