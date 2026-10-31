//    XSD Diagram - Kotlin/JavaFX port
//    schema.kt: raíz del documento (xs:schema) del modelo de datos que espeja el
//    metamodelo de XML Schema (equivale a XMLSchema.schema, generado por xsd.exe
//    en el original, archivo XMLSchema.cs).
//
//    Licencias del original: LGPL / MS-PL.
//
//    Nota de contexto del port (compartía cabecera con el resto del modelo):
//    las clases del paquete se escriben a mano y se rellenan mediante
//    XmlSchemaReader (parser DOM del JDK) en lugar de XmlSerializer; se portó el
//    SUBCONJUNTO que la lógica de negocio (Schema, Diagram, DiagramItem,
//    DiagramHelpers) usa realmente. Los nombres en minúsculas (schema, element,
//    group...) se conservan tal como los generaba xsd.exe para facilitar la
//    comparación con el C#.
package xsddiagramx.parsing

// ---------------------------------------------------------------------------
// Raíz del documento: xs:schema
// ---------------------------------------------------------------------------

/** xs:schema (equivale a XMLSchema.schema). */
class schema : OpenAttrs() {
    /** URI del targetNamespace del esquema (o null si el documento no lo declara). */
    var targetNamespace: String? = null
    /** Atributo version del documento (o null). */
    var version: String? = null
    /** Atributo finalDefault tal cual ("", "final", "final list"...). */
    var finalDefault: String = ""
    /** Atributo blockDefault tal cual ("", "block", "block list"...). */
    var blockDefault: String = ""

    /** Primer grupo de hijos: annotation, import, include, redefine (document order). */
    val items: MutableList<OpenAttrs> = ArrayList()

    /** Segundo grupo de hijos: declaraciones globales (element, type, group, attribute...). */
    val items1: MutableList<OpenAttrs> = ArrayList()
}
