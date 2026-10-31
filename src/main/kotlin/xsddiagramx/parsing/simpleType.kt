//    XSD Diagram - Kotlin/JavaFX port
//    simpleType.kt: xs:simpleType del modelo del esquema (equivale a los tipos
//    topLevelSimpleType / localSimpleType de XMLSchema.cs, generado por xsd.exe).
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.parsing

/** xs:simpleType (top-level, local dentro de xs:element/xs:attribute y anónimo). */
open class simpleType : Annotated() {
    /** Nombre del tipo simple (o null si es anónimo). */
    var name: String? = null
    /** Cuerpo del tipo simple: xs:restriction, xs:list o xs:union (o null si no hay). */
    var item: SimpleTypeBody? = null
}
