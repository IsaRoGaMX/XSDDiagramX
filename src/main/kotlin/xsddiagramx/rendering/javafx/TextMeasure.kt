//    XSD Diagram - Kotlin/JavaFX port
//    TextMeasure.kt: caché global de mediciones de texto JavaFX usada por el
//    DrawingContext de este paquete (ancho y alto de línea de una fuente).
//    Origen C#: sustituye a Graphics.MeasureString como fuente de mediciones
//    (antes de la caché, el port re-layoutaba un nodo TEMP por cada texto).
//
//    Licencias del original: LGPL / MS-PL. Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización).
package xsddiagramx.rendering.javafx

import javafx.scene.text.Text
import xsddiagramx.rendering.FontDescription

/**
 * Caché global de mediciones de texto (≈ Graphics.MeasureString pero validada/con caché:
 * `layoutBounds` fuerza el layout del nodo TEMP cada vez, y con diagramas grandes
 * eso congelaba la UI al redimensionar la ventana).
 *
 * Debe usarse SOLO en el FX Application Thread (igual que el pintado).
 */
object TextMeasure {

    private val cache = HashMap<String, Double>()

    /**
     * Ancho del texto con la fuente indicada, medido con un Text TEMP y
     * su layoutBounds; resultado cacheado (techo defensivo de 50k entradas).
     * ≈ Graphics.MeasureString(text, font).Width.
     */
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

    /**
     * Alto de línea de la fuente, medido con un Text vacío; si la medición
     * da 0 se usa el 125% del tamaño (fallback razonable).
     * ≈ Graphics.MeasureString("", font).Height.
     */
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
