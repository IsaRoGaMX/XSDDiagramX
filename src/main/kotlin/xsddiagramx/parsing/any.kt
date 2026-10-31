//    XSD Diagram - Kotlin/JavaFX port
//    any.kt: xs:any del modelo del esquema (equivale a XMLSchema.any, que en C#
//    derivaba de wildcard, en XMLSchema.cs).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/** xs:any (equivale a XMLSchema.any, que deriva de wildcard). */
class any : Annotated() {
    /** Valores permitidos de namespace tal cual el XML ("##any", "##other"...). */
    var namespace: String? = null
    /** Atributo minOccurs ("1" por defecto). */
    var minOccurs: String = "1"
    /** Atributo maxOccurs ("1" por defecto; puede ser "unbounded"). */
    var maxOccurs: String = "1"
}
