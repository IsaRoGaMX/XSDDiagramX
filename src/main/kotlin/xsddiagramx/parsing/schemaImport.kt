//    XSD Diagram - Kotlin/JavaFX port
//    schemaImport.kt: xs:import del modelo del esquema (equivale a XMLSchema.import,
//    generado por xsd.exe en el original C#).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/** xs:import. (En Kotlin `import` es palabra reservada, por eso el sufijo). */
class schemaImport : Annotated() {
    /** URI (normalmente absoluta o relativa a baseUrl) del esquema importado. */
    var schemaLocation: String? = null
    /** targetNamespace del esquema importado (o null / cadena vacía para "sin namespace"). */
    var namespace: String? = null
}
