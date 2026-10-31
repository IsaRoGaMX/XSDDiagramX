//    XSD DiagramX - esquema de colores de la aplicación (temas Claro / Oscuro).
//
//    El tema CLARO reproduce el esquema del C# original: diagrama blanco con
//    tinta negra, selección 0xA6CAF0 y UI con el gris claro de Modena. El tema
//    OSCURO usa una paleta neutra de grises de bajo contraste; los controles
//    conservan su forma (sólo cambian los colores, vía theme-dark.css y esta
//    paleta para el diagrama).
package xsddiagramx

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.paint.Color
import xsddiagramx.app.Settings

object AppTheme {

    /** ≈ la lista "Tema" del SettingsForm. */
    enum class Mode(val label: String) {
        LIGHT("Claro"),
        DARK("Oscuro");

        companion object {
            /** Traduce la etiqueta persistida en Settings al modo del tema. */
            fun fromLabel(label: String): Mode =
                entries.firstOrNull { it.label == label } ?: LIGHT
        }
    }

    /** Paleta de dibujo del diagrama (≈ los campos de color de DiagramGdiRenderer). */
    data class Palette(
        val background: Color,
        val foreground: Color,
        val selectedBackground: Color,
        val expandBoxBackground: Color,
        val disabled: Color,
    )

    /** Tema claro = esquema de colores del original. */
    val LIGHT = Palette(
        background = Color.WHITE,
        foreground = Color.BLACK,
        selectedBackground = Color(0xA6 / 255.0, 0xCA / 255.0, 0xF0 / 255.0, 1.0),
        expandBoxBackground = Color.WHITE,
        disabled = Color.GRAY,
    )

    /**
     * Tema oscuro: grises neutros, texto claro difuso (no blanco puro) y
     * selección azul acero apagado; sin contrastes extremos.
     */
    val DARK = Palette(
        background = Color.rgb(0x30, 0x32, 0x35),
        foreground = Color.rgb(0xD4, 0xD6, 0xD9),
        selectedBackground = Color.rgb(0x42, 0x5D, 0x7E),
        expandBoxBackground = Color.rgb(0x3B, 0x3D, 0x41),
        disabled = Color.rgb(0x88, 0x8B, 0x8E),
    )

    private const val THEME_CSS = "/xsddiagramx/app/theme-dark.css"
    private const val THEME_CSS_NAME = "theme-dark.css"

    /** Flag: theme-dark.css ya instalado como user agent stylesheet. */
    @Volatile
    private var userAgentDarkAdded = false

    /** ≈ Settings.Theme (persistido en Preferences). */
    fun currentMode(): Mode = Mode.fromLabel(Settings.theme)

    /** Paleta activa (Claro = esquema del original; Oscuro = tema del port). */
    fun currentPalette(): Palette = if (currentMode() == Mode.DARK) DARK else LIGHT

    /** Relleno de la Scene: en el tema oscuro cubre los huecos de layout. */
    fun sceneFill(): Color =
        if (currentMode() == Mode.DARK) Color.rgb(0x2E, 0x30, 0x33) else Color.WHITE

    /**
     * Aplica el tema actual a una Scene (stylesheet theme-dark.css + fill).
     * En "Claro" no deja stylesheet (Modena por defecto). Puede llamarse en
     * caliente sobre una Scene ya mostrada (≈ ChangeSetting("Theme")).
     */
    fun applyTo(scene: Scene) {
        scene.stylesheets.removeAll { it.contains(THEME_CSS_NAME) }
        if (currentMode() == Mode.DARK) {
            AppTheme::class.java.getResource(THEME_CSS)?.let {
                val url = it.toExternalForm()
                scene.stylesheets.add(url)
                installUserAgentDark(url)
            }
        } else {
            resetUserAgent()
        }
        scene.fill = sceneFill()
    }

    /**
     * Instala theme-dark.css como "user agent stylesheet" (JavaFX interno,
     * StyleManager.addUserAgentStylesheet por reflection).
     *
     * Los POPUPS (dropdowns de los combos, menús, tooltips) son Scenes aparte
     * que NO heredan los stylesheets ni las variables ".root" de la ventana:
     * el tema sólo por Scene dejaba los popups claros en oscuro y generaba los
     * warnings de Modena (ladder de "-fx-text-background-color" + ClassCast).
     * El user agent stylesheet alcanza a todas las Scenes, popups incluidos.
     * Se DEJA además de Modena (se agrega después en la misma capa, gana por
     * orden): Application.setUserAgentStylesheet(url) NO sirve porque
     * reemplazaría a Modena entera.
     *
     * Requiere --add-exports javafx.graphics/com.sun.javafx.css=ALL-UNNAMED
     * (lo agregan las tareas run/IDE del build). Sin el flag: no-op y en
     * pantalla los popups quedan claros (fallback por Scene).
     */
    private fun installUserAgentDark(url: String) {
        if (userAgentDarkAdded) return
        try {
            val managerClass = Class.forName("com.sun.javafx.css.StyleManager")
            val manager = managerClass.getMethod("getInstance").invoke(null)
            managerClass
                .getMethod("addUserAgentStylesheet", String::class.java)
                .invoke(manager, url)
            userAgentDarkAdded = true
        } catch (ex: Throwable) {
            println("AVISO (tema oscuro): no se instaló el stylesheet user agent; " +
                "los popups de combos/menús quedarán claros. Falta el flag " +
                "--add-exports javafx.graphics/com.sun.javafx.css=ALL-UNNAMED en el run. ($ex)")
        }
    }

    /** Al volver a "Claro": restaura el user agent por defecto (Modena). */
    private fun resetUserAgent() {
        if (!userAgentDarkAdded) return
        try {
            Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA)
            userAgentDarkAdded = false
        } catch (ex: Throwable) {
            println("AVISO (tema): no se pudo restaurar el user agent stylesheet ($ex)")
        }
    }
}
