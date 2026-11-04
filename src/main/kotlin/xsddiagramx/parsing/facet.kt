//    XSD Diagram - Kotlin/JavaFX port
//    facet.kt: xs:facet de los simpleType (enumeration, pattern, length...)
//    (equivale a XMLSchema.facet en XMLSchema.cs).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** xs:facet (enumeration, pattern, length...). El original usaba XMLSchema.facet. */
class facet(
    /** Tipo de faceta: el nombre local del elemento (p. ej. "enumeration", "pattern"...). */
    var kind: String = "",
    /** Atributo value de la faceta (p. ej. el valor enumerado o la expresión regular). */
    var value: String? = null,
) : Annotated()
