//    XSD Diagram - Kotlin/JavaFX port
//    Credentials.kt: credenciales para URLs protegidas con autenticación básica
//    HTTP (port del par `out username, password` del C# de Schema.cs).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** Credenciales para URLs protegidas (equivale a los `out username, password` del C#). */
data class Credentials(
    /** Nombre de usuario para la autenticación básica (opción -u en la consola). */
    val username: String,
    /** Contraseña para la autenticación básica (opción -p en la consola). */
    val password: String,
)
