//    XSD Diagram - Kotlin/JavaFX port
//    SimpleTypeBody.kt: base sellada del cuerpo de un xs:simpleType (equivale a la
//    elección simpleDerivationType de XMLSchema.cs en el original C#).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** Cuerpo de un xs:simpleType: restriction ([SimpleRestriction]), list ([SimpleList]) o union ([SimpleUnion]). */
sealed class SimpleTypeBody
