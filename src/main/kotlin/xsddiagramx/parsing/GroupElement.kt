//    XSD Diagram - Kotlin/JavaFX port
//    GroupElement.kt: subclase de [GroupItem] para xs:element dentro de xs:group /
//    compositores (equivale a XMLSchema.element en XMLSchema.cs).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/** Caso `element` dentro de un grupo/compositor; envuelve el [element] hijo. */
class GroupElement(
    /** El xs:element declarado (o referenciado). */
    val value: element,
) : GroupItem()
