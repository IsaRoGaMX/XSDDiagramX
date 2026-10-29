//    XSD Diagram - Kotlin/JavaFX port
//    Port de MRUManager.cs (lista de ficheros recientes).
//
//    Licencias del original: LGPL / MS-PL (véase LICENSE-LGPL.txt / LICENSE-MS-PL.txt)
//
//    Diferencias: el C# usaba el registro de Windows
//    (HKCU\Software\xsddiagram\MRU, claves "0","1","2"...); aquí se usa
//    java.util.prefs.Preferences del JDK (nodo xsddiagram/MRU) con las mismas
//    claves numéricas. El comportamiento es 1:1:
//      - AddRecentFile sólo AÑADE el fichero si no está ya (no lo reordena,
//        igual que el original: el orden es el de primera apertura).
//      - RemoveRecentFile borra la entrada y el ítem "Clear list" del menú
//        limpia el almacén entero.
//      - El callback onClearRecentFilesClick del MainForm era vacío en el C#.
package xsddiagramx.app

import java.util.prefs.Preferences
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem

class MRUManager(
    private val parentMenuItem: Menu,
    nameOfProgram: String,
    private val onRecentFileClick: (String) -> Unit,
    private val onClearRecentFilesClick: () -> Unit,
) {

    private val node = Preferences.userRoot().node("xsddiagramx/$nameOfProgram/MRU")

    init {
        require(nameOfProgram.isNotEmpty() && !nameOfProgram.contains('\\')) { "Bad argument." }
        refreshRecentFilesMenu()
    }

    /** ≈ AddRecentFile: sólo añade si no está (no reordena). */
    fun addRecentFile(fileNameWithFullPath: String) {
        try {
            for (i in 0..Int.MAX_VALUE) {
                val value = node.get(i.toString(), null)
                if (value == null) {
                    node.put(i.toString(), fileNameWithFullPath)
                    break
                } else if (value == fileNameWithFullPath) {
                    break
                }
            }
        } catch (ex: Exception) {
            println(ex.toString())
        }
        refreshRecentFilesMenu()
    }

    /** ≈ RemoveRecentFile. */
    fun removeRecentFile(fileNameWithFullPath: String) {
        try {
            for (key in node.keys()) {
                if (node.get(key, "") == fileNameWithFullPath) {
                    node.remove(key)
                    refreshRecentFilesMenu()
                    break
                }
            }
        } catch (ex: Exception) {
            println(ex.toString())
        }
        refreshRecentFilesMenu()
    }

    /** ≈ _onClearRecentFiles_Click (el ítem "Clear list" del menú). */
    private fun onClearRecentFilesClickInternal() {
        try {
            for (key in node.keys()) {
                node.remove(key)
            }
            parentMenuItem.items.clear()
            parentMenuItem.isDisable = true
        } catch (ex: Exception) {
            println(ex.toString())
        }
        onClearRecentFilesClick()
    }

    /** ≈ _refreshRecentFilesMenu: ítems + "-" + "Clear list". */
    private fun refreshRecentFilesMenu() {
        try {
            parentMenuItem.items.clear()
            for (key in node.keys().sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }) {
                val value = node.get(key, null) ?: continue
                parentMenuItem.items.add(
                    MenuItem(value).apply { setOnAction { onRecentFileClick(value) } }
                )
            }
            if (parentMenuItem.items.isEmpty()) {
                parentMenuItem.isDisable = true
                return
            }
            parentMenuItem.items.add(SeparatorMenuItem())
            parentMenuItem.items.add(
                MenuItem("Clear list").apply { setOnAction { onClearRecentFilesClickInternal() } }
            )
            parentMenuItem.isDisable = false
        } catch (ex: Exception) {
            println("Cannot open recent files preferences key:")
            println(ex.toString())
        }
    }
}
