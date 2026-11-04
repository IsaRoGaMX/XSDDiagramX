//    XSD Diagram - Kotlin/JavaFX port
//    AttrAnyAttribute.kt: subclase de [AttrItem] para xs:anyAttribute directo dentro
//    de xs:extension / xs:restriction / xs:attributeGroup (equivale a
//    XMLSchema.wildcard en XMLSchema.cs).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** Caso `anyAttribute` directo; envuelve el [wildcard] hijo. */
class AttrAnyAttribute(
    /** El xs:anyAttribute (representado como [wildcard]). */
    val value: wildcard,
) : AttrItem()
