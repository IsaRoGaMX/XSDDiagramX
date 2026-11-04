//    XSD Diagram - Kotlin/JavaFX port
//    XSDObject.kt: envoltorio de un componente global del esquema indexado
//    (equivale a XSDObject de Schema.cs en el original C#).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/**
 * Envoltorio de un componente global del esquema (equivale a XSDObject).
 *
 * [type] es "element" | "group" | "simpleType" | "complexType".
 * [fullName] es la clave de indexación en ElementsByName, con la convención
 * `"namespace:fullNameType:name"` (fullNameType = "type" para los tipos, o el
 * propio type). Esta convención es la "clave primaria" con la que el diagrama
 * resuelve referencias.
 */
class XSDObject(
    /** Fichero .xsd (local o descargado) donde se declaró el componente. */
    val filename: String = "",
    /** Nombre local del componente (atributo name del XML). */
    val name: String = "",
    /** URI del targetNamespace de su esquema ("" si no tiene). */
    val nameSpace: String = "",
    /** "element" | "group" | "simpleType" | "complexType". */
    val type: String = "",
    /** El nodo XML original del modelo ([OpenAttrs]) del que procede, o null. */
    val tag: OpenAttrs? = null,
) {
    /** Discriminador de la clave: "type" para simpleType/complexType, o [type] en el resto. */
    val fullNameType: String =
        if (type == "simpleType" || type == "complexType") "type" else type

    /** Clave de indexación en ElementsByName: "namespace:fullNameType:name". */
    val fullName: String get() = "$nameSpace:$fullNameType:$name"

    /** Representación textual "tipo: nombre (namespace)" (≈ ToString() del C#). */
    override fun toString(): String = "$type: $name ($nameSpace)"
}
