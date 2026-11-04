//    XSD Diagram - Kotlin/JavaFX port
//    ComplexItem.kt: base sellada del contenido de xs:complexType (equivale a la
//    elección Items/ItemsElementName de XMLSchema.cs en el original C#).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/**
 * Contenido de xs:complexType. Los casos `group`/`all`/`choice`/`sequence`
 * envuelven un [group] (en C# todos mapeaban a XMLSchema.group con subclases).
 */
sealed class ComplexItem
