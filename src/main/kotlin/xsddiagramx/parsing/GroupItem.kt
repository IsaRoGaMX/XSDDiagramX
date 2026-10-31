//    XSD Diagram - Kotlin/JavaFX port
//    GroupItem.kt: base sellada del contenido de xs:group / compositores (equivale a
//    la elección Items/ItemsElementName de XMLSchema.cs en el original C#).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/** Contenido de un xs:group o compositor: un [element], un [any] o un compositor anidado ([group]). */
sealed class GroupItem
