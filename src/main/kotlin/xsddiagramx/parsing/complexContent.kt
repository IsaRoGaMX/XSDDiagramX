//    XSD Diagram - Kotlin/JavaFX port
//    complexContent.kt: xs:complexContent del modelo del esquema (equivale a
//    XMLSchema.complexContent de XMLSchema.cs en el original C#).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/** xs:complexContent. */
class complexContent : Annotated() {
    /** Cuerpo: xs:extension o xs:restriction (una instancia de [extensionType] o [restrictionType]). */
    var item: ContentBody? = null
}
