//    XSD Diagram - Kotlin/JavaFX port
//    GroupAny.kt: subclase de [GroupItem] para xs:any dentro de xs:group /
//    compositores (equivale a XMLSchema.any de XMLSchema.cs).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** Caso `any` dentro de un grupo/compositor; envuelve el [any] hijo. */
class GroupAny(
    /** El xs:any (representado como [any]). */
    val value: any,
) : GroupItem()
