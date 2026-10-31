//    XSD Diagram - Kotlin/JavaFX port
//    KPoint.kt: punto entero derivado de System.Drawing.Point del modelo original.
//    Origen C#: sustituye a System.Drawing.Point tal como se usa en
//    DiagramItem.cs y Diagram.cs (portado a mano para no depender de AWT).
//
//    Licencias del original: LGPL / MS-PL. Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización).
package xsddiagramx.rendering

/**
 * Punto entero mutable, equivalente a System.Drawing.Point tal como lo usan
 * DiagramItem.cs y Diagram.cs (posiciones de nodos en el layout).
 */
data class KPoint(
    /** Coordenada horizontal (≈ Point.X). */
    var x: Int = 0,

    /** Coordenada vertical (≈ Point.Y). */
    var y: Int = 0,
) {
    /** Desplaza el punto por el tamaño dado (≈ Point.Add(Size) / Point.op_Addition). */
    operator fun plus(size: KSize): KPoint = KPoint(x + size.width, y + size.height)

    /** Resta punto a punto (≈ System.Drawing.Point.op_Subtraction(Point)). */
    operator fun minus(other: KPoint): KPoint = KPoint(x - other.x, y - other.y)

    /** Resta el tamaño al punto (≈ System.Drawing.Point.op_Subtraction(Size)). */
    operator fun minus(size: KSize): KPoint = KPoint(x - size.width, y - size.height)

    /** Depuración, con el formato "(x, y)". */
    override fun toString(): String = "($x, $y)"
}
