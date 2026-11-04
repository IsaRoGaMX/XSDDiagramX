//    XSD Diagram - Kotlin/JavaFX port
//    JavaFxFonts.kt: resolución de FontDescription a javafx.scene.text.Font con caché.
//    Origen C#: sustituye a la resolución de System.Drawing.Font (new Font(family, size, style))
//    que el renderer GDI hacía al pintar.
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
//    Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización).
package xsddiagramx.rendering.javafx

import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import xsddiagramx.rendering.FontDescription

/**
 * Conversión FontDescription (modelo) -> javafx.scene.text.Font (con caché).
 * ≈ la construcción directa de System.Drawing.Font del renderer GDI.
 */
object JavaFxFonts {

    // Crear un Font es caro (resolución de familia nativa); sin caché, un solo
    // pintado de un diagrama grande crea cientos de fuentes por frame.
    private val cache = HashMap<String, Font>()

    /**
     * Resuelve la fuente del modelo a una fuente JavaFX escalada.
     * ≈ Graphics.MeasureString requería el Font ya construido: aquí se
     * centraliza su creación, escalada por `scale` y con caché por clave.
     */
    fun toJavaFx(font: FontDescription, scale: Float = 1f): Font {
        val key = "${font.family}|${font.bold}|${font.sizePx}|$scale"
        val cached = cache[key]
        if (cached != null) return cached
        val created = Font.font(
            font.family,
            if (font.bold) FontWeight.BOLD else FontWeight.NORMAL,
            FontPosture.REGULAR,
            (font.sizePx * scale).toDouble()
        )
        cache[key] = created
        return created
    }
}
