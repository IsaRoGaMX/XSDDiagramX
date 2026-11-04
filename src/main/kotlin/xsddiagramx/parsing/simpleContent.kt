//    XSD Diagram - Kotlin/JavaFX port
//    simpleContent.kt: xs:simpleContent del modelo del esquema (equivale a
//    XMLSchema.simpleContent de XMLSchema.cs en el original C#).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** xs:simpleContent. */
class simpleContent : Annotated() {
    /** Cuerpo: xs:extension o xs:restriction (una instancia de [extensionType] o [restrictionType]). */
    var item: ContentBody? = null
}
