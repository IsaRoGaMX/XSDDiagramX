//    XSD Diagram - Kotlin/JavaFX port
//    ContentBody.kt: base sellada del cuerpo de complexContent/simpleContent
//    (equivale a extensionType/restrictionType de XMLSchema.cs en el original C#).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** xs:extension / xs:restriction dentro de contenido simple o complejo. */
sealed class ContentBody : Annotated()
