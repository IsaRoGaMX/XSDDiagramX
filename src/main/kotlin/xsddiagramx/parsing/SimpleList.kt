//    XSD Diagram - Kotlin/JavaFX port
//    SimpleList.kt: xs:list de un xs:simpleType (equivale a XMLSchema.list de
//    XMLSchema.cs en el original C#).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** xs:list. */
class SimpleList(
    /** QName del tipo simple de los ítems de la lista (o null si no se declaró). */
    val itemType: QualifiedName?,
) : SimpleTypeBody()
