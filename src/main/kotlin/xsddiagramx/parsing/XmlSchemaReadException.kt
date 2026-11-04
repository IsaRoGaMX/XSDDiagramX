//    XSD Diagram - Kotlin/JavaFX port
//    XmlSchemaReadException.kt: error de lectura/parseo de un .xsd
//    (equivale a los errores de XmlSerializer/InvalidOperationException del C#).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

import java.io.IOException

/** Errores de parseo (equivale a los errores de XmlSerializer/InvalidOperationException). */
class XmlSchemaReadException(message: String, cause: Throwable? = null) : IOException(message, cause)
