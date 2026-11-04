//    XSD Diagram - Kotlin/JavaFX port
//    GroupElement.kt: subclase de [GroupItem] para xs:element dentro de xs:group /
//    compositores (equivale a XMLSchema.element en XMLSchema.cs).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** Caso `element` dentro de un grupo/compositor; envuelve el [element] hijo. */
class GroupElement(
    /** El xs:element declarado (o referenciado). */
    val value: element,
) : GroupItem()
