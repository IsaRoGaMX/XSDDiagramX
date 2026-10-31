//    XSD Diagram - Kotlin/JavaFX port
//    ElementsRow.kt: fila de la lista de elementos globales del MainForm
//    (≈ ListViewItem del MainForm.cs; el C# usaba un string[] por ítem).
//
//    Licencias del original: LGPL (MainForm) / MS-PL.
package xsddiagramx.app

import xsddiagramx.parsing.XSDObject

/**
 * Fila de [listViewElements] del MainForm (≈ ListViewItem del C#).
 * Envuelve el [XSDObject] global del esquema y expone sus tres columnas.
 */
data class ElementsRow(val xsdObject: XSDObject) {
    /** Columna "Name" (≈ SubItems[0]). */
    val name: String get() = xsdObject.name

    /** Columna "Type" (≈ SubItems[1]). */
    val type: String get() = xsdObject.type

    /** Columna "Namespace" (≈ SubItems[2]). */
    val nameSpace: String get() = xsdObject.nameSpace
}
