//    XSD Diagram - Kotlin/JavaFX port
//
//    Equivale a System.Drawing.Point / Size / SizeF / Rectangle tal como los usa
//    el original (DiagramItem.cs y Diagram.cs). Implementados a mano para no
//    depender de AWT en el modelo de dominio; mañana los renderers concretos
//    (JavaFX Controller, SVG, TXT...) ya leerán / escribirán los mismos cajas.
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.rendering

/** Punto entero (System.Drawing.Point). */
data class KPoint(var x: Int = 0, var y: Int = 0) {
    operator fun plus(size: KSize): KPoint = KPoint(x + size.width, y + size.height)
    operator fun minus(other: KPoint): KPoint = KPoint(x - other.x, y - other.y)
    operator fun minus(size: KSize): KPoint = KPoint(x - size.width, y - size.height)
    override fun toString(): String = "($x, $y)"
}

/** Tamaño entero mutable (System.Drawing.Size). */
class KSize(var width: Int = 0, var height: Int = 0) {
    operator fun plus(other: KSize): KSize = KSize(width + other.width, height + other.height)
    operator fun minus(other: KSize): KSize = KSize(width - other.width, height - other.height)
    operator fun times(scale: Float): KSize =
        KSize(Math.round(width * scale), Math.round(height * scale))
    operator fun times(scale: Double): KSize =
        KSize(Math.round(width * scale).toInt(), Math.round(height * scale).toInt())
    fun copy(widthP: Int = width, heightP: Int = height): KSize = KSize(widthP, heightP)
    override fun toString(): String = "{Width=$width, Height=$height}"
}

/** Tamaño flotante resultado de la medición de texto (System.Drawing.SizeF). */
data class SizeF(val width: Double = 0.0, val height: Double = 0.0) {
    override fun toString(): String = "{Width=$width, Height=$height}"
}

/** Rectángulo entero mutable (System.Drawing.Rectangle). */
class KRectangle(var x: Int = 0, var y: Int = 0, var width: Int = 0, var height: Int = 0) {

    constructor(point: KPoint, size: KSize) : this(point.x, point.y, size.width, size.height)

    val size: KSize get() = KSize(width, height)
    val right: Int get() = x + width
    val bottom: Int get() = y + height

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

    fun offset(dx: Int, dy: Int) {
        x += dx
        y += dy
    }

    /** Equivale a Rectangle.Union(a, b). */
    fun union(other: KRectangle): KRectangle {
        val left = minOf(x, other.x)
        val top = minOf(y, other.y)
        val rightEdge = maxOf(right, other.right)
        val bottomEdge = maxOf(bottom, other.bottom)
        return KRectangle(left, top, rightEdge - left, bottomEdge - top)
    }

    /** Copia con semántica de struct (Rectangle es value type en C#). */
    fun copy(): KRectangle = KRectangle(x, y, width, height)

    override fun toString(): String =
        "{X=$x, Y=$y, Width=$width, Height=$height}"

    override fun equals(other: Any?): Boolean =
        other is KRectangle &&
            other.x == x && other.y == y && other.width == width && other.height == height

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}
