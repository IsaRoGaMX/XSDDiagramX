//    XSD Diagram - Kotlin/JavaFX port
//    Implementación JavaFX de DrawingContext (≈ System.Drawing.Graphics.MeasureString
//    en el C#). Consumida por Diagram.Layout para calcular el layout.
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.rendering.javafx

import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import xsddiagramx.rendering.DrawingContext
import xsddiagramx.rendering.FontDescription
import xsddiagramx.rendering.SizeF

/** Conversión FontDescription (modelo) -> javafx.scene.text.Font (con caché). */
object JavaFxFonts {

    // Crear un Font es caro (resolución de familia nativa); sin caché, un solo
    // pintado de un diagrama grande crea cientos de fuentes por frame.
    private val cache = HashMap<String, Font>()

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

/**
 * Caché global de mediciones de texto (≈ Graphics.MeasureString pero valida/cached:
 * `layoutBounds` fuerza el layout del nodo TEMP cada vez, y con diagramas grandes
 * eso congelaba la UI al redimensionar la ventana).
 *
 * Debe usarse SOLO en el FX Application Thread (igual que el pintado).
 */
object TextMeasure {

    private val cache = HashMap<String, Double>()

    fun width(text: String, font: FontDescription): Double {
        if (text.isEmpty()) return 0.0
        val key = "w|${font.family}|${font.bold}|${font.sizePx}|$text"
        val cached = cache[key]
        if (cached != null) return cached
        val node = Text(text)
        node.font = JavaFxFonts.toJavaFx(font)
        val width = node.layoutBounds.width
        if (cache.size > 50_000) cache.clear() // techo de memoria defensivo
        cache[key] = width
        return width
    }

    fun height(font: FontDescription): Double {
        val key = "h|${font.family}|${font.bold}|${font.sizePx}"
        val cached = cache[key]
        if (cached != null) return cached
        val node = Text("")
        node.font = JavaFxFonts.toJavaFx(font)
        val height = node.layoutBounds.height
        val result = if (height > 0.0) height else font.sizePx * 1.25
        cache[key] = result
        return result
    }
}

/**
 * ≈ Graphics.MeasureString. Debe invocarse en el Event Thread de JavaFX
 * (MainForm.UpdateDiagram lo hace desde un manejador de eventos).
 */
class JavaFxDrawingContext : DrawingContext {

    override fun measureString(text: String, font: FontDescription): SizeF =
        SizeF(TextMeasure.width(text, font), TextMeasure.height(font))
}
