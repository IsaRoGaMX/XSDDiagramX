//    XSD Diagram - Kotlin/JavaFX port
//    Port de XSDDiagrams\Rendering\DiagramPrinter.cs
//
//    Licencias del original: LGPL / MS-PL (véase LICENSE-LGPL.txt / LICENSE-MS-PL.txt)
//    Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización)
//
//    Diferencias:
//      - PrintDocument/PageSetupDialog/PrintDialog del WinForms → PrinterJob de
//        JavaFX (showPageSetupDialog/showPrintDialog). La paginación (columnas/
//        filas de páginas) es un port directo de OnPrintPage.
//      - PrintPreviewDialog no existe en JavaFX: ventana propia con las páginas
//        renderizadas (≈ la vista previa del WinForms) y botón Imprimir.
package xsddiagramx.rendering

import javafx.print.PageLayout
import javafx.print.PrinterJob
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.control.Button
import javafx.scene.control.ScrollPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import xsddiagramx.rendering.javafx.DiagramJavaFxRenderer
import xsddiagramx.rendering.javafx.JavaFxDrawingContext

/**
 * ≈ DiagramPrinter. Toda la operativa DEBE invocarse en el FX Application
 * Thread (los diálogos de JavaFX y el renderizado de páginas son FX).
 */
class DiagramPrinter(
    /** ≈ el Graphics de referencia que usaba el C# para re-layout de páginas. */
    private val drawingContext: JavaFxDrawingContext,
) {

    private var currentPage = 0
    private var diagram: Diagram? = null

    /** ≈ DocumentName. */
    var documentName: String = "XSD Document"

    /** ≈ Diagram. */
    fun setDiagram(value: Diagram?) { diagram = value }
    fun getDiagram(): Diagram? = diagram

    // ------------------------------------------------------------------
    // Diálogos (≈ PageSetupDialog / PrintDialog / PrintPreviewDialog)
    // ------------------------------------------------------------------

    /** ≈ PageSetup(): diálogo modal de configuración de página. */
    fun pageSetup(owner: Stage?): Boolean {
        val job = PrinterJob.createPrinterJob() ?: return false
        return job.showPageSetupDialog(owner)
    }

    /** ≈ Print(showDialog): diálogo de impresión y volcado de páginas. */
    fun print(showDialog: Boolean, owner: Stage?): Boolean {
        val d = diagram ?: throw IllegalStateException("There is no diagram to print")
        val job = PrinterJob.createPrinterJob() ?: return false

        if (showDialog) {
            if (!job.showPrintDialog(owner)) return false
        }

        job.jobSettings.jobName = documentName

        val layout = job.jobSettings.pageLayout
        val marginBounds = KRectangle(0, 0, layout.printableWidth.toInt(), layout.printableHeight.toInt())
        val pages = buildPages(d, layout, marginBounds)
        for (page in pages) job.printPage(layout, page)
        return job.endJob()
    }

    /** ≈ PrintPreview(): ventana propia con el conjunto de páginas. */
    fun printPreview(owner: Stage?) {
        val d = diagram ?: throw IllegalStateException("There is no diagram to print")
        val job = PrinterJob.createPrinterJob()
        if (job == null) return
        val layout = job.jobSettings.pageLayout
        val marginBounds = KRectangle(0, 0, layout.printableWidth.toInt(), layout.printableHeight.toInt())
        val pages = buildPages(d, layout, marginBounds)

        val stage = Stage()
        val scroll = ScrollPane().apply { content = pagesPane(pages) }
        val printButton = Button("Print...").apply {
            setOnAction {
                if (job.showPrintDialog(stage)) {
                    for (page in pages) job.printPage(layout, page)
                    job.endJob()
                }
            }
        }
        val root = BorderPane().apply {
            center = scroll
            bottom = BorderPane().apply {
                left = printButton
            }
        }
        stage.scene = Scene(root, 900.0, 700.0)
        stage.title = "Print Preview - $documentName"
        stage.showAndWait()
    }

    // ------------------------------------------------------------------
    // Paginación (≈ OnPrintPage, port directo)
    // ------------------------------------------------------------------

    /** ≈ el cálculo de páginas de OnPrintPage: devuelve un Canvas por página. */
    private fun buildPages(d: Diagram, layout: PageLayout, marginBounds: KRectangle): List<Canvas> {
        // ≈ _diagram.Layout(graphics): el C# re-layoutaba el diagrama en cada
        // página antes de trocear.
        d.layout(drawingContext)

        // ≈ Dimensiones. La unidad de JavaFX (puntos tipográficos, 1/72")
        // se trata como los "cientos de pulgada" (1/100") del C#: el diagrama
        // queda ~72/100 del tamaño que en el original, a cambio de usar
        // directamente el área imprimible tal cual la da el driver.
        val bbSize = d.boundingBox.size + d.padding + d.padding
        val totalSize = KSize(
            (bbSize.width * d.scale).toInt(),
            (bbSize.height * d.scale).toInt()
        )

        val columnNumber = 1 + totalSize.width / marginBounds.width
        val rowNumber = 1 + totalSize.height / marginBounds.height
        val pageNumber = columnNumber * rowNumber

        val pages = ArrayList<Canvas>()
        for (page in 0 until pageNumber) {
            val (column, row) = Pair(page / rowNumber, page % rowNumber)
            val clipping = KRectangle(
                KPoint(column * marginBounds.width, row * marginBounds.height),
                KSize((column + 1) * marginBounds.width, (row + 1) * marginBounds.height)
            )
            pages.add(
                renderPageNode(
                    d, marginBounds, clipping,
                    clipLeft = clipping.x - marginBounds.x,
                    clipTop = clipping.y - marginBounds.y
                )
            )
        }
        return pages
    }

    /** ≈ el.graphics.Clip/TranslateTransform + DiagramGdiRenderer.Draw(clipping). */
    private fun renderPageNode(
        d: Diagram,
        marginBounds: KRectangle,
        clipping: KRectangle,
        clipLeft: Int,
        clipTop: Int,
    ): Canvas {
        val canvas = Canvas(marginBounds.width.toDouble(), marginBounds.height.toDouble())
        val g = canvas.graphicsContext2D
        g.fill = Color.WHITE
        g.fillRect(0.0, 0.0, canvas.width, canvas.height)
        g.save()
        g.translate(-(clipLeft.toDouble()), -(clipTop.toDouble()))
        DiagramJavaFxRenderer(g).render(d, clipping)
        g.restore()
        return canvas
    }

    private fun pagesPane(pages: List<Canvas>): VBox {
        val box = VBox(8.0)
        box.style = "-fx-background-color: #8a8a8a; -fx-padding: 12px"
        for (page in pages) {
            page.style = "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 6, 0.3, 2, 2)"
            box.children.add(page)
        }
        return box
    }
}
