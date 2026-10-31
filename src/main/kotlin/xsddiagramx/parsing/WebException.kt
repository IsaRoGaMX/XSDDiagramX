//    XSD Diagram - Kotlin/JavaFX port
//    WebException.kt: excepción de fallo en la descarga HTTP de dependencias XSD
//    (port de System.Net.WebException de Schema.cs en el original C#).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

import java.io.IOException

/** Excepción de descarga (equivale a System.Net.WebException). */
class WebException(
    /** Código HTTP, o null si no se llegó a una respuesta (DNS, IO...). */
    val httpCode: Int? = null,
    /** Mensaje del error (incluye la URL y el código, si hay). */
    message: String,
    /** Causa original (excepción de red/IO subyacente), o null. */
    cause: Throwable? = null,
) : IOException(message, cause)
