//    XSD Diagram - Kotlin/JavaFX port
//    extensionType.kt: xs:extension dentro de xs:complexContent o xs:simpleContent
//    (equivale a XMLSchema.extensionType de XMLSchema.cs en el original C#).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/**
 * xs:complexContent > xs:extension (equivale a XMLSchema.extensionType).
 */
class extensionType(
    /** QName del tipo base que se extiende (o null si el XML no lo trae). */
    base: QualifiedName? = null,
) : ContentBody() {
    /** QName del tipo base extendido. */
    var base: QualifiedName? = base

    /** El original tenía propiedades separadas (group/sequence/choice/all): se conservan. */
    var group: group? = null
    /** xs:sequence hijo del extension (si existe; sinónimo de [group] según el original). */
    var sequence: group? = null
    /** xs:choice hijo del extension (si existe; sinónimo de [group] según el original). */
    var choice: group? = null
    /** xs:all hijo del extension (si existe; sinónimo de [group] según el original). */
    var all: group? = null

    /** xs:attribute | xs:attributeGroup directos. */
    val items: MutableList<AttrItem> = ArrayList()
}
