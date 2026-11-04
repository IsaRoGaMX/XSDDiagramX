//    XSD Diagram - Kotlin/JavaFX port
//    group.kt: xs:group y los compositores (xs:all, xs:choice, xs:sequence comparten
//    clase, como en el C# original con explicitGroup : group de XMLSchema.cs).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** xs:group / compositor (tnxs:all, xs:choice, xs:sequence y xs:group comparten clase). */
open class group : Annotated() {
    /** Nombre del grupo (sólo top-level; los compositores anónimos van a null). */
    var name: String? = null

    /** Determinado sólo en referencias (xs:group ref="..."). */
    var ref: QualifiedName? = null
    /** Atributo minOccurs del compositor ("1" por defecto, tal cual el XML: puede ser "unbounded"). */
    var minOccurs: String = "1"
    /** Atributo maxOccurs del compositor ("1" por defecto, tal cual el XML: puede ser "unbounded"). */
    var maxOccurs: String = "1"

    /** Contenido del compositor/o grupo; paralelo a [itemsElementName]. */
    val items: MutableList<GroupItem> = ArrayList()
    /** Etiqueta (valores de [ItemsChoiceType2]) en document order, paralela a [items]. */
    val itemsElementName: MutableList<ItemsChoiceType2> = ArrayList()
}
