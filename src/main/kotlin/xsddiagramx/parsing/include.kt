//    XSD Diagram - Kotlin/JavaFX port
//    include.kt: xs:include del modelo del esquema (equivale a XMLSchema.include,
//    generado por xsd.exe en el original C#).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** xs:include. */
class include : Annotated() {
    /** URI (normalmente relativa) del esquema incluido; la usa SchemaLoader para la carga recursiva. */
    var schemaLocation: String? = null
}
