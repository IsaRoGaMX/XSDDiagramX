//    XSD Diagram - Kotlin/JavaFX port
//    ComplexSimpleContent.kt: subclase de [ComplexItem] para xs:simpleContent dentro
//    de xs:complexType (equivale a XMLSchema.simpleContent en XMLSchema.cs).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** Caso `simpleContent` dentro de un xs:complexType; envuelve el [simpleContent] hijo. */
class ComplexSimpleContent(
    /** El xs:simpleContent (extension o restriction simple). */
    val value: simpleContent,
) : ComplexItem()
