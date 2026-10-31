//    XSD Diagram - Kotlin/JavaFX port
//    Port de DiagramControlContainer.cs (UserControl con scrollbars verticales/
//    horizontales sobre un área "virtual", pan con el ratón y rueda del mouse).
//
//    Licencias del original: LGPL / MS-PL.
//    Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización)
//
//    Diferencias deliberadas respecto a las ScrollBars de WinForms:
//      - En WinForms Maximum = valor máximo + LargeChange, y se resta
//        (Maximum - LargeChange); el ScrollBar de JavaFX expone directamente el
//        valor máximo del desplazamiento => aquí max = virtualSize - clientSize.
//      - La rueda Java entrega deltaY en "notches" (~40/paso); el original
//        restaba e.Delta (120/paso): se multiplica x3 para igualar el efecto.
package xsddiagramx.app

import javafx.scene.Cursor
import javafx.geometry.Orientation
import javafx.scene.control.ScrollBar
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.scene.shape.Rectangle
import xsddiagramx.rendering.KPoint
import xsddiagramx.rendering.KSize
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class DiagramControlContainer : BorderPane() {

    val diagramControl = DiagramControl()

    // JOJO: en JavaFX un ScrollBar suelto es HORIZONTAL por defecto (al contrario
    // que las barras internas del ScrollPane): hay que fijar la orientación y el
    // grosor, o la celda derecha del BorderPane se reserva ~63px en blanco.
    private val vScrollBar = ScrollBar().apply {
        orientation = Orientation.VERTICAL
        unitIncrement = 5.0
        prefWidth = 17.0
    }
    private val hScrollBar = ScrollBar().apply {
        orientation = Orientation.HORIZONTAL
        unitIncrement = 5.0
        prefHeight = 17.0
    }

    private var virtualPoint = KPoint(0, 0)
    private var virtualSize = KSize(10, 10)

    companion object {
        /** CSS de las scrollbars (track/thumb visibles), por tema. */
        private val css_scrollbar: String? by lazy {
            DiagramControlContainer::class.java
                .getResource("/xsddiagramx/app/scrollbar.css")
                ?.toExternalForm()
        }
        private val css_scrollbar_dark: String? by lazy {
            DiagramControlContainer::class.java
                .getResource("/xsddiagramx/app/scrollbar-dark.css")
                ?.toExternalForm()
        }
    }

    private var isDragging = false
    private var mouseDownPosition = KPoint(0, 0)
    private var mouseDownHScroll = 0
    private var mouseDownVScroll = 0

    /** Contenedor del canvas (necesario para recortar el dibujo a la celda central). */
    private val canvasHolder = StackPane(diagramControl).apply {
        val clipRectangle = Rectangle()
        clipRectangle.widthProperty().bind(widthProperty())
        clipRectangle.heightProperty().bind(heightProperty())
        clip = clipRectangle
    }

    // ------------------------------------------------------------------
    // Eventos que MainForm conecta (≈ los del UserControl en el C#)
    // ------------------------------------------------------------------

    /** ≈ DiagramControl.MouseClick (MainForm hit-testea sumando VirtualPoint). */
    var diagramMouseClick: ((KPoint) -> Unit)? = null

    /** ≈ DiagramControl.MouseWheel + Control: MainForm cambia el zoom (+1/-1). */
    var diagramWheelZoom: ((Int) -> Unit)? = null

    /** ≈ DiagramControl.KeyDown (navegación del diagrama). */
    var diagramKeyPressed: ((KeyEvent) -> Unit)? = null

    init {
        center = canvasHolder
        right = vScrollBar
        bottom = hScrollBar

        // Estilo: las barras por defecto de Modena son casi invisibles sobre el
        // lienzo (track claro y thumb gris pálido): el usuario reportó que
        // "nunca aparecían". Track gris + thumb oscuro bien visible. La
        // variante (claro/oscuro) la elige applyTheme() según el tema activo.
        applyTheme()

        // El Canvas NO es "resizable" en JavaFX: StackPane no lo estira por su
        // cuenta, así que se enlaza explícitamente al tamaño del holder (en
        // WinForms el UserControl rellenaba el panel con DockStyle.Fill).
        diagramControl.widthProperty().bind(canvasHolder.widthProperty())
        diagramControl.heightProperty().bind(canvasHolder.heightProperty())
        // Al redimensionar el Canvas JavaFX borra su contenido: repintar.
        // DIFFERIDO (debounce): mientras se arrastra la esquina no se pinta
        // nada y se repinta 150 ms después de parar (evita el "no responde").
        diagramControl.widthProperty().addListener { _, _, _ -> requestRepaintAfterResize() }
        diagramControl.heightProperty().addListener { _, _, _ -> requestRepaintAfterResize() }

        canvasHolder.widthProperty().addListener { _, _, _ -> layoutScrollBars() }
        canvasHolder.heightProperty().addListener { _, _, _ -> layoutScrollBars() }

        // BUGFIX: el Canvas no es redimensionable y su mínimo = su tamaño
        // actual (747x631 al arrancar), así que al ENCGER la ventana el
        // StackPane no encogía (mínimo "pegado"), no disparaba los listeners
        // y las scrollbars dejaban de recalcular (desaparecían aunque el
        // diagrama ya no cupiera). Mínimos a 0: el holder puede encoger
        // libremente y los listeners disparan el re-layout de las barras.
        canvasHolder.minWidth = 0.0
        canvasHolder.minHeight = 0.0

        // ≈ vScrollBar_ValueChanged / hScrollBar_ValueChanged
        vScrollBar.valueProperty().addListener { _, _, _ -> scrollbars_ValueChanged() }
        hScrollBar.valueProperty().addListener { _, _, _ -> scrollbars_ValueChanged() }

        // ≈ DiagramControl.MouseClick
        diagramControl.setOnMouseClicked { event ->
            if (!event.isStillSincePress) return@setOnMouseClicked
            diagramMouseClick?.invoke(KPoint(event.x.roundToInt(), event.y.roundToInt()))
        }

        // ≈ DiagramControl.MouseDown / MouseMove / MouseUp (pan arrastrando).
        // Cursor "mano cerrada" SOLO durante el arrastre real (usabilidad:
        // sensación de sujetar el papel; al soltar vuelve al cursor normal).
        // No va en MOUSE_PRESSED porque un clic sin arrastre (p.ej. para
        // ver los atributos de un nodo) mantendría la mano cerrada: los
        // eventos MOUSE_DRAGGED solo llegan con el botón pulsado Y algo
        // de movimiento, así que ahí se activa el cursor.
        diagramControl.addEventFilter(MouseEvent.MOUSE_PRESSED) { event ->
            if (!isDragging) {
                mouseDownPosition = KPoint(event.screenX.roundToInt(), event.screenY.roundToInt())
                mouseDownHScroll = hScrollBar.value.roundToInt()
                mouseDownVScroll = vScrollBar.value.roundToInt()
                isDragging = true
                diagramControl.requestFocus()
            }
        }
        diagramControl.addEventFilter(MouseEvent.MOUSE_DRAGGED) { event ->
            if (isDragging) {
                // Mano cerrada desde el primer movimiento real del pan.
                if (diagramControl.cursor != Cursor.CLOSED_HAND)
                    diagramControl.cursor = Cursor.CLOSED_HAND

                // Pan: reversa del drag (= arrastrar el papel, no la ventana).
                val hValue = mouseDownHScroll + (mouseDownPosition.x - event.screenX.roundToInt())
                if (hValue >= hScrollBar.min && hValue <= hScrollBar.max)
                    hScrollBar.value = hValue.toDouble()

                val vValue = mouseDownVScroll + (mouseDownPosition.y - event.screenY.roundToInt())
                if (vValue >= vScrollBar.min && vValue <= vScrollBar.max)
                    vScrollBar.value = vValue.toDouble()
            }
        }
        diagramControl.addEventFilter(MouseEvent.MOUSE_RELEASED) { _ ->
            isDragging = false
            diagramControl.cursor = Cursor.DEFAULT
        }

        // ≈ DiagramControl.MouseWheel (MainForm): sin Control => scroll, con Control => zoom.
        diagramControl.addEventFilter(ScrollEvent.SCROLL) { event ->
            val dbg = System.getProperty("xsddiagramx.fxwatch") == "1"
            if (event.isControlDown) {
                if (dbg) println("[WHEELDBG] ctrl-wheel deltaY=${event.deltaY}")
                diagramWheelZoom?.invoke(if (event.deltaY > 0) 1 else -1)
            } else {
                // e.Delta del WinForms es 120/notch; deltaY ~40/notch: x3.
                // Con clamp expl�cito: JavaFX NO garantiza el clamp en
                // asignaci�n directa, y sin este la rueda puede salirse del
                // rango y "desplazarse a zonas donde el diagrama no es visible".
                val min = vScrollBar.min
                val max = vScrollBar.max
                val target = (vScrollBar.value - event.deltaY * 3).coerceIn(min, max)
                if (dbg) {
                    println(
                        "[WHEELDBG] deltaY=${event.deltaY} value=${vScrollBar.value} -> $target " +
                            "(range $min..$max, visible=${vScrollBar.isVisible})"
                    )
                }
                vScrollBar.value = target
            }
            event.consume()
        }

        // ≈ DiagramControl.KeyDown
        diagramControl.isFocusTraversable = true
        diagramControl.setOnKeyPressed { event -> diagramKeyPressed?.invoke(event) }
    }

    // ------------------------------------------------------------------
    // Propiedades virtuales (≈ VirtualPoint / VirtualSize)
    // ------------------------------------------------------------------

    fun getVirtualPoint(): KPoint = virtualPoint

    fun setVirtualPoint(value: KPoint) {
        virtualPoint = value
    }

    fun getVirtualSize(): KSize = virtualSize

    fun setVirtualSize(value: KSize) {
        virtualSize = value
        if (System.getProperty("xsddiagramx.fxwatch") == "1") {
            println("[SCROLLDBG] setVirtualSize=$value client=${diagramControl.clientSize}")
        }
        layoutScrollBars()
    }

    // ------------------------------------------------------------------
    // Scrollbars (≈ LayoutScrollBars / Clear / ScrollTo)
    // ------------------------------------------------------------------

    /** Cambia el CSS de las scrollbars según el tema activo (≈ un
     *  "theme switch": sólo cambia el color, no la forma de la barra). */
    fun applyTheme() {
        val dark = xsddiagramx.AppTheme.currentMode() == xsddiagramx.AppTheme.Mode.DARK
        val wanted = if (dark) css_scrollbar_dark else css_scrollbar
        stylesheets.removeAll { it.contains("scrollbar.css") || it.contains("scrollbar-dark.css") }
        if (wanted != null) stylesheets.add(wanted)
    }

    fun clear() {
        hScrollBar.value = 0.0
        vScrollBar.value = 0.0
        layoutScrollBars()
    }

    fun layoutScrollBars() {
        val client = diagramControl.clientSize
        val dbg = System.getProperty("xsddiagramx.fxwatch") == "1"

        // max = virtual - client (el rango real de desplazamiento) es lo que
        // hace el port (≈ Maximum-LargeChange del WinForms). Pero ADemás hay
        // que dar a JavaFX la fracción visible de la barra (visibleAmount) para
        // que el tamano del thumb represente client/virtual (como el thumb que
        // WinForms dibujaba automáticamente); sin esto el thumb queda de
        // tamano fijo y "se mueve mucho la barra para mover poco el diagrama".
        if (virtualSize.width > client.width) {
            hScrollBar.max = (virtualSize.width - client.width).toDouble()
            hScrollBar.blockIncrement = client.width.toDouble()
            hScrollBar.visibleAmount =
                hScrollBar.max * client.width.toDouble() / virtualSize.width.toDouble()
            hScrollBar.isVisible = true
        } else {
            hScrollBar.max = 0.0
            hScrollBar.isVisible = false
        }

        if (virtualSize.height > client.height) {
            vScrollBar.max = (virtualSize.height - client.height).toDouble()
            vScrollBar.blockIncrement = client.height.toDouble()
            vScrollBar.visibleAmount =
                vScrollBar.max * client.height.toDouble() / virtualSize.height.toDouble()
            vScrollBar.isVisible = true
        } else {
            vScrollBar.max = 0.0
            vScrollBar.isVisible = false
        }

        if (dbg) {
            println(
                "[SCROLLDBG] layout: virtual=$virtualSize client=$client " +
                    "h.visible=${hScrollBar.isVisible} h.max=${hScrollBar.max} " +
                    "v.visible=${vScrollBar.isVisible} v.max=${vScrollBar.max}"
            )
        }

        // ≈ diagramControl.Invalidate(ClientRectangle) del C#. Puede llegar
        // disparado por el resize de la ventana (canvasHolder) o por un cambio
        // de tamaño virtual (zoom): DIFERIDO en ambos casos (debounce 150 ms).
        requestRepaintAfterResize()
    }

    /** ≈ ScrollTo(Point) / ScrollTo(Point, bool center). */
    fun scrollTo(point: KPoint, center: Boolean = false) {
        var p = point
        if (center) {
            p = KPoint(
                p.x - diagramControl.clientRectangle.width / 2,
                p.y - diagramControl.clientRectangle.height / 2
            )
        }

        val clampedX = min(max(p.x, hScrollBar.min.roundToInt()), hScrollBar.max.roundToInt())
        val clampedY = min(max(p.y, vScrollBar.min.roundToInt()), vScrollBar.max.roundToInt())
        hScrollBar.value = clampedX.toDouble()
        vScrollBar.value = clampedY.toDouble()
    }

    private fun scrollbars_ValueChanged() {
        virtualPoint = KPoint(hScrollBar.value.roundToInt(), vScrollBar.value.roundToInt())
        // Coalescido: durante el pan/rueda llegan muchos cambios en un frame.
        requestRepaint()
    }

    /** ≈ diagramControl.Invalidate(ClientRectangle). */
    fun repaint() {
        if (diagramControl.width > 0 && diagramControl.height > 0) {
            diagramControl.paint(virtualPoint)
        }
    }

    /**
     * Repintado coalescido del scroll/pan: como máximo 1 pintado por frame del
     * FX Thread (el pintado del diagrama es caro).
     */
    private var repaintScheduled = false

    private fun requestRepaint() {
        if (repaintScheduled) return
        repaintScheduled = true
        javafx.application.Platform.runLater {
            repaintScheduled = false
            repaint()
        }
    }

    /**
     * Repintado DIFFERIDO (debounce) para los cambios impulsados por el resize
     * de la ventana: mientras se arrastra NO se pinta nada; se repinta ~150 ms
     * después de que paran los eventos. Es lo que evita el "no responde" al
     * estirar la ventana desde una esquina (Windows envía decenas de WM_SIZE
     * por segundo y cada repintado completo era carísimo).
     */
    private val resizeRepaintTimer = javafx.animation.PauseTransition(
        javafx.util.Duration.millis(150.0)
    )

    private var resizeRepaintScheduled = false

    private fun requestRepaintAfterResize() {
        if (!resizeRepaintScheduled) {
            resizeRepaintScheduled = true
            resizeRepaintTimer.setOnFinished {
                resizeRepaintScheduled = false
                repaint()
            }
        }
        resizeRepaintTimer.playFromStart()
    }
}
