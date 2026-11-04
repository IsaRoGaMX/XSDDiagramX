//    XSD Diagram - Kotlin/JavaFX port
//    restrictionType.kt: xs:restriction dentro de xs:complexContent o xs:simpleContent
//    (equivale a XMLSchema.restrictionType de XMLSchema.cs en el original C#).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** xs:complexContent > xs:restriction (equivale a XMLSchema.restrictionType). */
class restrictionType(
    /** QName del tipo base restringido (o null si el XML no lo trae). */
    base: QualifiedName? = null,
) : ContentBody() {
    /** QName del tipo base restringido. */
    var base: QualifiedName? = base

    /** Partículas del restriction (usadas por el fix del "github issue 2"). */
    val items: MutableList<ComplexItem> = ArrayList()
    /** Etiqueta (valores de [ItemsChoiceType4]) en document order, paralela a [items]. */
    val itemsElementName: MutableList<ItemsChoiceType4> = ArrayList()

    /** xs:attribute | xs:attributeGroup del restriction (equivale a Items1). */
    val items1: MutableList<AttrItem> = ArrayList()
}
