//    XSD Diagram - Kotlin/JavaFX port
//    GroupCompositor.kt: subclase de [GroupItem] para un compositor anidado
//    (xs:sequence/xs:choice/xs:all/xs:group) dentro de otro grupo
//    (equivale a explicitGroup : group de XMLSchema.cs).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** Caso `group`/`all`/`choice`/`sequence` dentro de un grupo/compositor; envuelve el [group] anidado. */
class GroupCompositor(
    /** El compositor o referencia xs:group anidada. */
    val value: group,
) : GroupItem()
