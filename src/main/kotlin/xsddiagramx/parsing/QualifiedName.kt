//    XSD Diagram - Kotlin/JavaFX port
//    QualifiedName.kt: nombre cualificado XML (namespace + nombre local) usado por
//    todo el modelo del esquema (equivale a System.Xml.XmlQualifiedName en C#).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/**
 * Equivale a System.Xml.XmlQualifiedName en el C#. El namespace es la URI resuelta
 * (no el prefijo); para nombres sin prefijo el namespace es la cadena vacía "",
 * igual que ocurría en el C# (los atributos sin prefijo no tienen namespace).
 */
data class QualifiedName(
    /** URI del namespace resuelto; "" o null si el nombre no tiene namespace. */
    val namespace: String? = null,
    /** Nombre local (sin el prefijo). */
    val name: String? = null,
) {
    /** Devuelve "namespace:name", o sólo "name" si no hay namespace (como en el C#). */
    override fun toString(): String =
        if (namespace.isNullOrEmpty()) (name ?: "") else "$namespace:$name"
}
