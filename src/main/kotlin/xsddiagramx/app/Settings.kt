//    XSD Diagram - Kotlin/JavaFX port
//    Port de Settings.cs (ApplicationSettingsBase del C#) + Properties\Settings.Designer.cs
//
//    Licencias del original: LGPL / MS-PL (véase LICENSE-LGPL.txt / LICENSE-MS-PL.txt)
//
//    Diferencias: el C# persistía en user.config (machine/user.config por
//    empresa/producto); aquí se usa java.util.prefs.Preferences del JDK
//    (HKCU\Software\JavaSoft\Prefs en Windows, ~/.java/.userPrefs en Linux,
//    plists en macOS) — sin dependencias de terceros. Los valores se escriben
//    al asignarlos y Settings.save() hace el flush (≈ FormClosed → Save()).
package xsddiagramx.app

import java.util.prefs.Preferences

/**
 * ≈ Settings.Default. Las propiedades con [Browsable(false)] del C#
 * (DisplayPanel, Zoom, Alignement, ShowDocumentation) no aparecen en el
 * SettingsForm pero sí se persisten desde la UI principal.
 */
object Settings {

    private val node = Preferences.userRoot().node("xsddiagramx/settings")

    /** ≈ DisplayPanel (Category UI, Default true). */
    var displayPanel: Boolean = node.getBoolean("DisplayPanel", true)
        set(value) {
            field = value
            node.putBoolean("DisplayPanel", value)
        }

    /** ≈ Zoom (índice en el combo; Default 8 = "100%"). */
    var zoom: Int = node.getInt("Zoom", 8)
        set(value) {
            field = value
            node.putInt("Zoom", value)
        }

    /** ≈ Alignement (índice en el combo; Default 1 = "Center"). */
    var alignement: Int = node.getInt("Alignement", 1)
        set(value) {
            field = value
            node.putInt("Alignement", value)
        }

    /** ≈ ShowDocumentation (Default false). */
    var showDocumentation: Boolean = node.getBoolean("ShowDocumentation", false)
        set(value) {
            field = value
            node.putBoolean("ShowDocumentation", value)
        }

    /** ≈ AlwaysShowOccurence (Default false). Editable en el SettingsForm. */
    var alwaysShowOccurence: Boolean = node.getBoolean("AlwaysShowOccurence", false)
        set(value) {
            field = value
            node.putBoolean("AlwaysShowOccurence", value)
        }

    /** ≈ ShowType (Default false). Editable en el SettingsForm. */
    var showType: Boolean = node.getBoolean("ShowType", false)
        set(value) {
            field = value
            node.putBoolean("ShowType", value)
        }

    /** ≈ CompactLayoutDensity (Default false). Editable en el SettingsForm. */
    var compactLayoutDensity: Boolean = node.getBoolean("CompactLayoutDensity", false)
        set(value) {
            field = value
            node.putBoolean("CompactLayoutDensity", value)
        }

    /** Tema de la aplicación ("Claro"/"Oscuro"). Editable en el SettingsForm;
     *  lo consume xsddiagramx.AppTheme (no existía en el C#: adición del port). */
    var theme: String = node.get("Theme", "Claro")
        set(value) {
            field = value
            node.put("Theme", value)
        }

    /** ≈ Settings.Default.Save() (se llama al cerrar el MainForm). */
    fun save() {
        try {
            node.flush()
        } catch (ex: Exception) {
            println(ex.toString())
        }
    }
}
