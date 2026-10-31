//    XSD Diagram - Kotlin/JavaFX port
//    documentation.kt: xs:documentation del modelo del esquema (equivale a
//    XMLSchema.documentation generado por xsd.exe en el original C#).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/**
 * xs:documentation. El contenido mixto (equivale a documentation.Any[0].Value en C#)
 * se reduce a la propiedad [text].
 */
class documentation : OpenAttrs() {
    /** Texto plano del contenido mixto del xs:documentation (equivale a Any[0].Value en C#). */
    var text: String? = null
    /** Atributo xml:lang (reservado del namespace XML; o null si no está). */
    var lang: String? = null
    /** Atributo source (URI opcional del xs:documentation). */
    var source: String? = null
}
