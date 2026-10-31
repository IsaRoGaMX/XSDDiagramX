//    XSD Diagram - Kotlin/JavaFX port
//    element.kt: xs:element del modelo del esquema (equivale a topLevelElement /
//    localElement / narrowMaxMin de XMLSchema.cs, generado por xsd.exe).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/**
 * xs:element. Una sola clase para top-level y local (en C#: topLevelElement,
 * localElement y narrowMaxMin).
 * OJO: `abstract` es palabra reservada en Kotlin, por eso el nombre [isAbstract].
 */
class element : Annotated() {
    /** Nombre del elemento (o null si es una referencia pura xs:element ref="..."). */
    var name: String? = null
    /** QName del tipo declarado en el atributo type (o null). */
    var type: QualifiedName? = null
    /** QName de la referencia (attribute ref="...") si es global (o null). */
    var ref: QualifiedName? = null
    /** QName del atributo substitutionGroup si el XML lo declara (o null). */
    var substitutionGroup: QualifiedName? = null
    /** Atributo abstract="true" del XML (puede ser sustituido por subclases). */
    var isAbstract: Boolean = false
    /** Atributo nillable="true" del XML. */
    var nillable: Boolean = false
    /** Atributo minOccurs ("1" por defecto); String porque puede ser "0" o "1". */
    var minOccurs: String = "1"
    /** Atributo maxOccurs ("1" por defecto); String porque puede ser "unbounded". */
    var maxOccurs: String = "1"

    /** xs:element > xs:complexType inline (equivale a `element.Item`). */
    var complexType: complexType? = null

    /** xs:element > xs:simpleType inline. */
    var simpleType: simpleType? = null
}
