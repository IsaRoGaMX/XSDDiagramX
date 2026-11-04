//    XSD Diagram - Kotlin/JavaFX port
//    GroupItem.kt: base sellada del contenido de xs:group / compositores (equivale a
//    la elección Items/ItemsElementName de XMLSchema.cs en el original C#).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** Contenido de un xs:group o compositor: un [element], un [any] o un compositor anidado ([group]). */
sealed class GroupItem
