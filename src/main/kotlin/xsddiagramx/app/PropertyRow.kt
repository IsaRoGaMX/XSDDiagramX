//    XSD Diagram - Kotlin/JavaFX port
//    PropertyRow.kt: fila del PropertyGrid del esquema en el MainForm
//    (≈ propertyGridSchemaObject del MainForm.cs, tab "Element").
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.app

/**
 * Fila del PropertyGrid del esquema (≈ propertyGridSchemaObject, tab "Element")
 * con el par nombre/valor de una propiedad reflejada del objeto seleccionado.
 */
data class PropertyRow(val name: String, val value: String)
