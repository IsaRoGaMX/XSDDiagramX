//    XSD Diagram - Kotlin/JavaFX port
//    KRectangle.kt: rectángulo entero derivado de System.Drawing.Rectangle del original.
//    Origen C#: sustituye a System.Drawing.Rectangle tal como se usa en
//    DiagramItem.cs y Diagram.cs (elementBox, boundingBox, ...).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
//    Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización).
package xsddiagramx.rendering

/**
 * Rectángulo entero mutable, equivalente a System.Drawing.Rectangle tal como
 * lo usan DiagramItem.cs y Diagram.cs (cajas de elemento, expandir, documentación).
 */
class KRectangle(
    /** Coordenada horizontal de la esquina superior izquierda (≈ Rectangle.X). */
    var x: Int = 0,

    /** Coordenada vertical de la esquina superior izquierda (≈ Rectangle.Y). */
    var y: Int = 0,

    /** Ancho del rectángulo (≈ Rectangle.Width). */
    var width: Int = 0,

    /** Alto del rectángulo (≈ Rectangle.Height). */
    var height: Int = 0,
) {

    /** Construye el rectángulo a partir de su esquina superior izquierda y su tamaño (≈ Rectangle(Point, Size)). */
    constructor(point: KPoint, size: KSize) : this(point.x, point.y, size.width, size.height)

    /** Tamaño del rectángulo expresado como KSize (≈ Rectangle.Size). */
    val size: KSize get() = KSize(width, height)

    /** Coordenada del borde derecho (≈ Rectangle.Right). */
    val right: Int get() = x + width

    /** Coordenada del borde inferior (≈ Rectangle.Bottom). */
    val bottom: Int get() = y + height

    /** Indica si el punto está dentro del rectángulo (borde derecho/inferior excluidos, como Rectangle.Contains). */
    fun contains(p: KPoint): Boolean =
        p.x >= x && p.x < right && p.y >= y && p.y < bottom

    /** Equivale a Rectangle.IntersectsWith (intersección no vacía). */
    fun intersects(other: KRectangle): Boolean =
        other.x < right && x < other.right && other.y < bottom && y < other.bottom

    /** Equivale a Rectangle.Offset(Point). */
    fun offset(delta: KPoint) {
        x += delta.x
        y += delta.y
    }

    /** Equivale a Rectangle.Offset(int, int). */
    fun offset(dx: Int, dy: Int) {
        x += dx
        y += dy
    }

    /** Equivale a Rectangle.Union(a, b): menor rectángulo que contiene ambos. */
    fun union(other: KRectangle): KRectangle {
        val left = minOf(x, other.x)
        val top = minOf(y, other.y)
        val rightEdge = maxOf(right, other.right)
        val bottomEdge = maxOf(bottom, other.bottom)
        return KRectangle(left, top, rightEdge - left, bottomEdge - top)
    }

    /** Copia con semántica de struct (Rectangle es value type en C#). */
    fun copy(): KRectangle = KRectangle(x, y, width, height)

    /** Depuración, con el formato "C# ToString()" del struct Rectangle original. */
    override fun toString(): String =
        "{X=$x, Y=$y, Width=$width, Height=$height}"

    /** Igualdad por componentes, emulando el == de structs de System.Drawing. */
    override fun equals(other: Any?): Boolean =
        other is KRectangle &&
            other.x == x && other.y == y && other.width == width && other.height == height

    /** Hash consistente con equals (por componentes). */
    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}
