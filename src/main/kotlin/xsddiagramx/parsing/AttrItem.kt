//    XSD Diagram - Kotlin/JavaFX port
//    AttrItem.kt: base sellada del contenido de xs:extension / xs:restriction /
//    xs:attributeGroup (equivale a la elección Items de XMLSchema.cs en el original).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** Contenido de atributos: un [attribute], un [attributeGroup] o un [wildcard] (xs:anyAttribute). */
sealed class AttrItem
