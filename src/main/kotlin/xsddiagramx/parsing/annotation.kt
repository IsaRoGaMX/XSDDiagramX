//    XSD Diagram - Kotlin/JavaFX port
//    annotation.kt: xs:annotation del modelo del esquema (equivale a
//    XMLSchema.annotation generado por xsd.exe en el original C#).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/** xs:annotation. */
class annotation : OpenAttrs() {
    /** Bloques xs:documentation de la anotación (xs:appinfo no se conserva: no se usaba). */
    val items: MutableList<documentation> = ArrayList()
}
