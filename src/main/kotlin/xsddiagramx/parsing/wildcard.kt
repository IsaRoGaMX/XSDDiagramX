//    XSD Diagram - Kotlin/JavaFX port
//    wildcard.kt: xs:anyAttribute del modelo del esquema (equivale a
//    XMLSchema.wildcard de XMLSchema.cs en el original C#).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** xs:anyAttribute (equivale a XMLSchema.wildcard). */
class wildcard : Annotated() {
    /** Valores permitidos de namespace del xs:anyAttribute tal cual el XML ("##any", "##other"...). */
    var namespace: String? = null
}
