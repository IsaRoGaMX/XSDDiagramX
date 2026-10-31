//    XSD Diagram - Kotlin/JavaFX port
//    AttributesRow.kt: fila de la tabla de atributos del MainForm
//    (≈ ListViewItem del MainForm.cs con su .Tag en el último campo).
//
//    Licencias del original: LGPL (MainForm) / MS-PL.
package xsddiagramx.app

import xsddiagramx.parsing.XSDAttribute

/**
 * Fila del "Attributes" (listViewAttributes) del MainForm (≈ ListViewItem).
 * Los handlers leen [tag] para mostrar la documentación y las enumeraciones
 * del atributo seleccionado.
 */
data class AttributesRow(
    val name: String,
    val type: String,
    val use: String,
    val defaultValue: String,
    val facets: String,
    /** XSDAttribute envolvente (≈ ListViewItem.Tag del C#: los handlers lo usan). */
    val tag: XSDAttribute? = null,
)
