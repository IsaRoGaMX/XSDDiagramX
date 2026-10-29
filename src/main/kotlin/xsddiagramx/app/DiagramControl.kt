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

class DiagramControl : Canvas() {

    var diagram: Diagram? = null

    /**
     * ≈ DiagramControl_Paint(PaintEventArgs): limpia el fondo, traslada por el
     * punto virtual y renderiza con el clip del área visible (antialias gratis
     * con Canvas).
     */
    fun paint(virtualPoint: KPoint) {
        val d = diagram ?: return

        val g = graphicsContext2D
        // Fondo blanco (≈ BackColor del UserControl).
        g.fill = javafx.scene.paint.Color.WHITE
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
