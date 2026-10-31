//    XSD Diagram - Kotlin/JavaFX port
//    Port de DiagramControl.cs (UserControl con doble buffer del WinForms)
//    sobre javafx.scene.canvas.Canvas. La lógica de pintado la ejecuta
//    MainForm.DiagramControl_Paint vía paint(virtualPoint).
//
//    Licencias del original: LGPL (MainForm/DiagramControl) / MS-PL.
package xsddiagramx.app

import javafx.scene.canvas.Canvas
import xsddiagramx.rendering.Diagram
import xsddiagramx.rendering.KPoint
import xsddiagramx.rendering.KRectangle
import xsddiagramx.rendering.KSize
import xsddiagramx.rendering.javafx.DiagramJavaFxRenderer

/** ≈ DiagramControl del C#: Canvas donde se pinta el diagrama; [paint] lo
 *  invoca el contenedor con el punto virtual. */
class DiagramControl : Canvas() {

    /** Diagrama a pintar (≈ DiagramControl.Diagram; null = lienzo vacío). */
    var diagram: Diagram? = null

    /**
     * ≈ DiagramControl_Paint(PaintEventArgs): limpia el fondo, traslada por el
     * punto virtual y renderiza con el clip del área visible (antialias gratis
     * con Canvas).
     */
    fun paint(virtualPoint: KPoint) {
        val d = diagram ?: return

        val g = graphicsContext2D
        // Fondo del tema (≈ BackColor del UserControl; Claro = blanco).
        g.fill = xsddiagramx.AppTheme.currentPalette().background
        g.fillRect(0.0, 0.0, width, height)
        g.save()
        g.translate(-virtualPoint.x.toDouble(), -virtualPoint.y.toDouble())
        val clip = KRectangle(virtualPoint.x, virtualPoint.y, width.toInt(), height.toInt())
        DiagramJavaFxRenderer(g).render(d, clip)
        g.restore()
    }

    /** Equivale a UserControl.ClientSize. */
    val clientSize: KSize
        get() = KSize(width.toInt(), height.toInt())

    /** Equivale a Control.ClientRectangle. */
    val clientRectangle: KRectangle
        get() = KRectangle(0, 0, width.toInt(), height.toInt())
}
