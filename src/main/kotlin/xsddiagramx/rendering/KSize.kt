//    XSD Diagram - Kotlin/JavaFX port
//    KSize.kt: tamaño entero derivado de System.Drawing.Size del modelo original.
//    Origen C#: sustituye a System.Drawing.Size tal como se usa en
//    DiagramItem.cs y Diagram.cs (portado a mano para no depender de AWT).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
//    Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización).
package xsddiagramx.rendering

/**
 * Tamaño entero mutable, equivalente a System.Drawing.Size tal como lo usan
 * DiagramItem.cs y Diagram.cs (medidas de nodos, márgenes y paddings).
 */
class KSize(
    /** Ancho en píxeles (≈ Size.Width). */
    var width: Int = 0,

    /** Alto en píxeles (≈ Size.Height). */
    var height: Int = 0,
) {
    /** Suma componente a componente (≈ Size.op_Addition(Size)). */
    operator fun plus(other: KSize): KSize = KSize(width + other.width, height + other.height)

    /** Resta componente a componente (≈ Size.op_Subtraction(Size)). */
    operator fun minus(other: KSize): KSize = KSize(width - other.width, height - other.height)

    /** Multiplica por un factor flotante y redondea a entero (≈ Size * float del C#). */
    operator fun times(scale: Float): KSize =
        KSize(Math.round(width * scale), Math.round(height * scale))

    /** Multiplica por un factor doble y redondea a entero (≈ Size * double del C#). */
    operator fun times(scale: Double): KSize =
        KSize(Math.round(width * scale).toInt(), Math.round(height * scale).toInt())

    /** Copia permitiendo sobreescribir width/height (emula la semántica de struct Size). */
    fun copy(widthP: Int = width, heightP: Int = height): KSize = KSize(widthP, heightP)

    /** Depuración, con el formato "C# ToString()" del struct Size original. */
    override fun toString(): String = "{Width=$width, Height=$height}"
}
