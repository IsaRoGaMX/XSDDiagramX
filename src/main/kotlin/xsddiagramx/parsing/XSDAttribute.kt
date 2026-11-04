//    XSD Diagram - Kotlin/JavaFX port
//    XSDAttribute.kt: envoltorio de un atributo global del esquema indexado
//    (equivale a XSDAttribute de Schema.cs en el original C#).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** Envoltorio de un atributo global (equivale a XSDAttribute). */
class XSDAttribute(
    /** Fichero .xsd (local o descargado) donde se declaró el atributo. */
    val filename: String = "",
    /** Nombre local del atributo global (atributo name del XML). */
    val name: String = "",
    /** URI del targetNamespace de su esquema ("" si no tiene). */
    val nameSpace: String = "",
    /** Siempre "attribute" (tipo de componente, tal como en el C#). */
    val type: String = "",
    /** Indica si el nodo es una referencia xs:attribute ref="..." (la ref apunta a otro). */
    val isReference: Boolean = false,
    /** Atributo default del XML (valor por defecto; o null). */
    val defaultValue: String? = null,
    /** "optional" (defecto) | "required" | "prohibited" del `use`. */
    var use: String? = null,
    /** El nodo XML original ([attribute]) del que procede, o null. */
    val tag: attribute? = null,
) {
    /** Clave de indexación en AttributesByName: "namespace:attribute:name". */
    val fullName: String get() = "$nameSpace:attribute:$name"

    /** Representación textual "nombre (namespace)" (≈ ToString() del C#). */
    override fun toString(): String = "$name ($nameSpace)"
}
