//    XSD Diagram - Kotlin/JavaFX port
//    attributeGroup.kt: xs:attributeGroup del modelo del esquema (equivale a
//    attributeGroupRef / namedAttributeGroup de XMLSchema.cs, generado por xsd.exe).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

/** xs:attributeGroup (local y top-level; en C#: attributeGroupRef y namedAttributeGroup). */
open class attributeGroup : Annotated() {
    /** Nombre del grupo de atributos (sólo top-level; en refs va a null). */
    var name: String? = null
    /** QName de la referencia (xs:attributeGroup ref="...") si es global (o null). */
    var ref: QualifiedName? = null

    /** xs:attribute | xs:attributeGroup directos. */
    val items: MutableList<AttrItem> = ArrayList()
}
