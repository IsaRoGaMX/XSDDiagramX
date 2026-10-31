//    XSD Diagram - Kotlin/JavaFX port
//    GroupAny.kt: subclase de [GroupItem] para xs:any dentro de xs:group /
//    compositores (equivale a XMLSchema.any de XMLSchema.cs).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/** Caso `any` dentro de un grupo/compositor; envuelve el [any] hijo. */
class GroupAny(
    /** El xs:any (representado como [any]). */
    val value: any,
) : GroupItem()
