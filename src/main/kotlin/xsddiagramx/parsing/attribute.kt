//    XSD Diagram - Kotlin/JavaFX port
//    attribute.kt: xs:attribute del modelo del esquema (equivale a attribute /
//    attributeRef de XMLSchema.cs, generado por xsd.exe).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/** xs:attribute (top-level y local). El `use` original era un enum; aquí es String tal cual. */
class attribute : Annotated() {
    /** Nombre del atributo (o null si es sólo una referencia). */
    var name: String? = null
    /** QName del tipo simple del atributo (o null si usa xs:simpleType inline). */
    var type: QualifiedName? = null
    /** QName de la referencia (xs:attribute ref="...") si es global (o null). */
    var ref: QualifiedName? = null
    /** Atributo default del XML (valor por defecto; o null). */
    var defaultValue: String? = null
    /** Atributo fixed del XML (valor fijo; o null). */
    var fixed: String? = null
    /** "optional" (defecto) | "required" | "prohibited" (equivale a attributeUse.ToString()). */
    var use: String? = null

    /** xs:attribute > xs:simpleType inline (equivale a localSimpleType). */
    var simpleType: simpleType? = null
}
