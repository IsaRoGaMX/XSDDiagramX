//    XSD Diagram - Kotlin/JavaFX port
//    RequestCredentialHandler.kt: función-callback que pide credenciales al usuario
//    cuando una URL responde 401/403 (port del delegado RequestCredentialEventHandler
//    de Schema.cs en el original C#).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/**
 * Callback que pide credenciales para una URL protegida.
 * Equivale al delegado `bool RequestCredentialEventHandler(url, realm, attemptCount,
 * out username, out password)` del C#: devuelve las credenciales, o null si el
 * usuario cancela.
 */
typealias RequestCredentialHandler = (url: String, realm: String, attemptCount: Int) -> Credentials?
