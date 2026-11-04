//    XSD Diagram - Kotlin/JavaFX port
//    XSDAttributeGroup.kt: envoltorio de un attributeGroup global del esquema
//    indexado (equivale a XSDAttributeGroup de Schema.cs en el original C#).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** Envoltorio de un attributeGroup global (equivale a XSDAttributeGroup). */
class XSDAttributeGroup(
    /** Fichero .xsd (local o descargado) donde se declaró el attributeGroup. */
    val filename: String = "",
    /** Nombre local del attributeGroup global (atributo name del XML). */
    val name: String = "",
    /** URI del targetNamespace de su esquema ("" si no tiene). */
    val nameSpace: String = "",
    /** Siempre "attributeGroup" (tipo de componente, tal como en el C#). */
    val type: String = "",
    /** Indica si el nodo es una referencia xs:attributeGroup ref="...". */
    val isReference: Boolean = false,
    /** El nodo XML original ([attributeGroup]) del que procede, o null. */
    val tag: attributeGroup? = null,
) {
    /** Clave de indexación en AttributesByName: "namespace:attributeGroup:name". */
    val fullName: String get() = "$nameSpace:attributeGroup:$name"

    /** Representación textual "nombre (namespace)" (≈ ToString() del C#). */
    override fun toString(): String = "$name ($nameSpace)"
}
