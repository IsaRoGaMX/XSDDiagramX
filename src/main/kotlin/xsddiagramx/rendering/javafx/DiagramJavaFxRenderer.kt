//    XSD Diagram - Kotlin/JavaFX port
//    Port de XSDDiagrams\Rendering\DiagramGdiRenderer.cs sobre JavaFX Canvas.
//
//    Licencias del original: LGPL / MS-PL.
//    Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización)
//
//    Equivalencias System.Drawing -> JavaFX:
//      Graphics                    -> GraphicsContext
//      FillRectangle/DrawRectangle -> fillRect / strokeRect
//      FillPath/DrawPath (políg.)  -> fillPolygon / strokePolygon (rank=2 arrays)
//      DrawLine                    -> strokeLine
//      FillEllipse                 -> fillOval
//      Pen.DashStyle/DashPattern   -> setLineDashes
//      DrawString (alineación)     -> setTextAlign / setTextBaseline + fillText
package xsddiagramx.rendering.javafx

import javafx.geometry.VPos
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import xsddiagramx.rendering.Diagram
import xsddiagramx.rendering.DiagramItem
import xsddiagramx.rendering.DiagramItemGroupType
import xsddiagramx.rendering.DiagramItemType
import xsddiagramx.rendering.FontDescription
import xsddiagramx.rendering.KPoint
import xsddiagramx.rendering.KRectangle
import xsddiagramx.rendering.KSize
import xsddiagramx.AppTheme
import kotlin.math.max
import kotlin.math.roundToInt

class DiagramJavaFxRenderer(
    private val gc: GraphicsContext,
    /** Paleta del tema; por defecto la del tema activo. Los documentos
     *  (exportación PNG/JPG) pasan AppTheme.LIGHT para
     *  dibujar "papel" blanco aunque la app esté en tema Oscuro. */
    private val palette: AppTheme.Palette = AppTheme.currentPalette(),
) {

    // ------------------------------------------------------------------
    // Colores (tema Claro = esquema del C#; el Oscuro sale de AppTheme)
    // ------------------------------------------------------------------

    private val backgroundColor = palette.background
    private val selectedBackground = palette.selectedBackground
    private val foreground = palette.foreground
    private val expandBoxBackground = palette.expandBoxBackground
    private val gray = palette.disabled

    // ------------------------------------------------------------------
    // Render del diagrama completo (con culling por clip)
    // ------------------------------------------------------------------

    /** Equivale a Render(Diagram, Rectangle?). */
    fun render(diagram: Diagram, clipRectangle: KRectangle? = null) {
        val scaledClip = clipRectangle?.let { clipBox ->
            KRectangle(
                (clipBox.x / diagram.scale).roundToInt(),
                (clipBox.y / diagram.scale).roundToInt(),
                (clipBox.width / diagram.scale).roundToInt(),
                (clipBox.height / diagram.scale).roundToInt()
            )
        }
        for (element in diagram.rootElements) {
            if (scaledClip == null || element.boundingBox.intersects(scaledClip)) {
                render(element, scaledClip)
            }
        }
        gc.setLineDashes()
    }

    // ------------------------------------------------------------------
    // Render de un nodo (port 1:1 de DiagramGdiRenderer.Render)
    // ------------------------------------------------------------------

    fun render(drawingItem: DiagramItem, clipRectangle: KRectangle? = null) {
        if (drawingItem.diagram.showBoundingBox) {
            gc.fill = boundingBoxColor(drawingItem.depth)
            gc.stroke = foregroundColor(drawingItem)
            val scaledBoundingBox = drawingItem.scaleRectangle(drawingItem.boundingBox)
            fillRectBox(scaledBoundingBox)
            strokeRectBox(scaledBoundingBox)
        }

        // Draw the children
        if (drawingItem.showChildElements) {
            for (element in drawingItem.childElements) {
                if (clipRectangle == null || element.boundingBox.intersects(clipRectangle)) {
                    render(element, clipRectangle)
                }
            }
        }

        val scaledElementBox = drawingItem.scaleRectangle(drawingItem.elementBox)

        // Draw the children lines
        if (drawingItem.showChildElements && drawingItem.childElements.isNotEmpty()) {
            val showDocumentation = drawingItem.diagram.showDocumentation
            if (drawingItem.childElements.size == 1 && !showDocumentation) {
                val parentMidleY = drawingItem.scaleInt(drawingItem.location.y + drawingItem.size.height / 2)
                drawLine(
                    drawingItem.scaleInt(drawingItem.location.x + drawingItem.size.width), parentMidleY,
                    drawingItem.scaleInt(drawingItem.childElements[0].location.x), parentMidleY
                )
            } else if (drawingItem.childElements.size > 1 || showDocumentation) {
                val firstElement = drawingItem.childElements.first()
                val lastElement = drawingItem.childElements.last()
                val verticalLine = drawingItem.scaleInt(firstElement.boundingBox.x)
                for (element in drawingItem.childElements) {
                    if (element.inheritFrom == null) {
                        val currentMidleY = drawingItem.scaleInt(element.location.y + element.size.height / 2)
                        drawLine(verticalLine, currentMidleY, drawingItem.scaleInt(element.location.x), currentMidleY)
                    }
                }
                val parentMidleY = drawingItem.scaleInt(drawingItem.location.y + drawingItem.size.height / 2)
                val firstMidleY = minOf(
                    drawingItem.scaleInt(firstElement.location.y + firstElement.size.height / 2),
                    parentMidleY
                )
                val lastMidleY = maxOf(
                    drawingItem.scaleInt(lastElement.location.y + lastElement.size.height / 2),
                    parentMidleY
                )
                drawLine(verticalLine, firstMidleY, verticalLine, lastMidleY)
                drawLine(
                    drawingItem.scaleInt(drawingItem.location.x + drawingItem.size.width), parentMidleY,
                    verticalLine, parentMidleY
                )
            }
        }

        // Draw the inheritor line
        val inheritFrom = drawingItem.inheritFrom
        if (inheritFrom != null) {
            setLineDashPattern(drawingItem)

            val p1 = KPoint(
                drawingItem.scaleInt(inheritFrom.location.x - 5),
                drawingItem.scaleInt(inheritFrom.location.y + inheritFrom.size.height + 5)
            )
            val p2 = KPoint(
                drawingItem.scaleInt(drawingItem.location.x - 5),
                drawingItem.scaleInt(drawingItem.location.y - 5)
            )
            drawLine(p1.x, p1.y, p2.x, p2.y)
            drawLine(p2.x, p2.y, drawingItem.scaleInt(drawingItem.location.x), drawingItem.scaleInt(drawingItem.location.y))

            val targetPoint = KPoint(
                drawingItem.scaleInt(inheritFrom.location.x),
                drawingItem.scaleInt(inheritFrom.location.y + inheritFrom.size.height)
            )
            drawLine(targetPoint.x, targetPoint.y, p1.x, p1.y)

            // Pequeño triángulo junto al padre (≈ GraphicsPath.AddPolygon del C#)
            val triangle = doubleArrayOf(
                targetPoint.x.toDouble(), targetPoint.y.toDouble(),
                targetPoint.x.toDouble(), targetPoint.y + drawingItem.scaleInt(5).toDouble(),
                targetPoint.x - drawingItem.scaleInt(5).toDouble(), targetPoint.y.toDouble()
            )
            gc.fill = resolveBackground(drawingItem)
            fillPolygon(triangle)
            gc.stroke = foregroundColor(drawingItem)
            strokePolygon(triangle)

            gc.setLineDashes()
        }

        when (drawingItem.itemType) {
            DiagramItemType.element -> drawElementShape(drawingItem, scaledElementBox)
            DiagramItemType.type -> drawTypeShape(drawingItem, scaledElementBox)
            DiagramItemType.group -> drawGroupShape(drawingItem, scaledElementBox)
        }

        // Draw text
        if (drawingItem.name.isNotEmpty()) {
            val text =
                if (drawingItem.diagram.showType && drawingItem.type.isNotEmpty())
                    drawingItem.name + ":" + drawingItem.type
                else drawingItem.name
            val font = drawingItem.diagram.fontScaled ?: drawingItem.font
            drawTextCentered(drawingItem, text, font, scaledElementBox)
        }

        // Draw Documentation
        if (drawingItem.diagram.showDocumentation && !isRectEmpty(drawingItem.documentationBox)) {
            val text = drawingItem.getTextDocumentation()
            if (text != null) {
                val scaledDocumentationBox = drawingItem.scaleRectangle(drawingItem.documentationBox)
                val font = drawingItem.diagram.documentationFontScaled ?: drawingItem.documentationFont
                drawWrappedText(drawingItem, text, font, scaledDocumentationBox)
            }
        }

        // Draw occurences small text
        if (drawingItem.diagram.alwaysShowOccurence ||
            (drawingItem.maxOccurrence > 1 || drawingItem.maxOccurrence == -1)
        ) {
            val font = drawingItem.diagram.smallFontScaled ?: drawingItem.smallFont
            val occurences =
                "${drawingItem.minOccurrence}.." +
                    if (drawingItem.maxOccurrence == -1) "\u221E" else "${drawingItem.maxOccurrence}"
            val compactLayoutDensity = drawingItem.diagram.compactLayoutDensity
            val pointOccurencesX = drawingItem.diagram.scale *
                (drawingItem.location.x + drawingItem.size.width + (if (compactLayoutDensity) +23 else -10))
            val pointOccurencesY = drawingItem.diagram.scale *
                (drawingItem.location.y + drawingItem.size.height + (if (compactLayoutDensity) -17 else +10))

            gc.setFont(JavaFxFonts.toJavaFx(font))
            gc.setFill(foregroundColor(drawingItem))
            // ≈ StringFormat.Alignment=Far / LineAlignment=Center del C#.
            gc.setTextAlign(TextAlignment.RIGHT)
            gc.setTextBaseline(VPos.CENTER)
            gc.fillText(occurences, pointOccurencesX.toDouble(), pointOccurencesY.toDouble())
        }

        // Draw type (simple content: las 4 líneas superiores del "paper")
        if (drawingItem.isSimpleContent) {
            gc.stroke = foregroundColor(drawingItem)
            var currentPoint = KPoint(scaledElementBox.x + 2, scaledElementBox.y + 2)
            drawLine(currentPoint.x, currentPoint.y, currentPoint.x + drawingItem.scaleInt(8), currentPoint.y)
            currentPoint += KSize(0, 2)
            drawLine(currentPoint.x, currentPoint.y, currentPoint.x + drawingItem.scaleInt(6), currentPoint.y)
            currentPoint += KSize(0, 2)
            drawLine(currentPoint.x, currentPoint.y, currentPoint.x + drawingItem.scaleInt(6), currentPoint.y)
            currentPoint += KSize(0, 2)
            drawLine(currentPoint.x, currentPoint.y, currentPoint.x + drawingItem.scaleInt(6), currentPoint.y)
        }

        // Draw reference arrow
        if (drawingItem.isReference) {
            gc.lineWidth = drawingItem.diagram.scale * 2.0
            var basePoint = KPoint(drawingItem.elementBox.x + 1, drawingItem.elementBox.bottom - 1)
            var targetPoint = basePoint + KSize(3, -3)
            basePoint = drawingItem.scalePoint(basePoint)
            targetPoint = drawingItem.scalePoint(targetPoint)
            if (drawingItem.itemType == DiagramItemType.group) {
                val bevel = (scaledElementBox.height * 0.30).toInt()
                val groupCornerOffset = (bevel * 0.424264068713).toInt() // 0.6/sqr(2)
                basePoint += KSize(groupCornerOffset, -groupCornerOffset)
                targetPoint += KSize(groupCornerOffset, -groupCornerOffset)
            }
            gc.stroke = foregroundColor(drawingItem)
            drawLine(basePoint.x, basePoint.y, targetPoint.x, targetPoint.y)

            // Cabeza de flecha (≈ GraphicsPath.AddPolygon del C#)
            val arrowHead = doubleArrayOf(
                targetPoint.x.toDouble(), targetPoint.y.toDouble(),
                targetPoint.x + drawingItem.scaleInt(2).toDouble(), targetPoint.y + drawingItem.scaleInt(2).toDouble(),
                targetPoint.x + drawingItem.scaleInt(3).toDouble(), targetPoint.y - drawingItem.scaleInt(3).toDouble(),
                targetPoint.x - drawingItem.scaleInt(2).toDouble(), targetPoint.y - drawingItem.scaleInt(2).toDouble()
            )
            gc.setFill(foregroundColor(drawingItem))
            fillPolygon(arrowHead)
            gc.lineWidth = 1.0
        }

        // Draw children expand box
        if (drawingItem.hasChildElements) {
            val scaledChildExpandButtonBox = drawingItem.scaleRectangle(drawingItem.childExpandButtonBox)
            gc.fill = expandBoxBackground
            fillRectBox(scaledChildExpandButtonBox)
            gc.stroke = foregroundColor(drawingItem)
            strokeRectBox(scaledChildExpandButtonBox)

            val middleX = scaledChildExpandButtonBox.width / 2
            val middleY = scaledChildExpandButtonBox.height / 2
            val borderPadding = max(2, drawingItem.scaleInt(2))

            val p1x = scaledChildExpandButtonBox.x + borderPadding
            val p1y = scaledChildExpandButtonBox.y + middleY
            val p2x = scaledChildExpandButtonBox.right - borderPadding
            drawLine(p1x, p1y, p2x, p1y)
            if (!drawingItem.showChildElements) {
                val vx = scaledChildExpandButtonBox.x + middleX
                drawLine(vx, scaledChildExpandButtonBox.y + borderPadding, vx, scaledChildExpandButtonBox.bottom - borderPadding)
            }
        }
    }

    // ------------------------------------------------------------------
    // Formas principales (element / type / group)
    // ------------------------------------------------------------------

    private fun applyOccurrencePen(item: DiagramItem) {
        gc.lineWidth = 1.0
        if (item.minOccurrence == 0) setLineDashPattern(item)
    }

    /** ≈ case DiagramItemType.element. */
    private fun drawElementShape(item: DiagramItem, scaledElementBox: KRectangle) {
        gc.stroke = foregroundColor(item)
        applyOccurrencePen(item)
        val background = resolveBackground(item)

        if (item.maxOccurrence == 1) {
            gc.fill = background
            fillRectBox(scaledElementBox)
            strokeRectBox(scaledElementBox)
        } else {
            val shifted = scaledElementBox.copy()
            shifted.offset(item.scalePoint(KPoint(3, 3)))
            gc.fill = background
            fillRectBox(shifted)
            strokeRectBox(shifted)
            gc.fill = background
            fillRectBox(scaledElementBox)
            strokeRectBox(scaledElementBox)
        }
        gc.setLineDashes()

        if (item.isDisabled) drawHatch(item, scaledElementBox)
    }

    /** ≈ case DiagramItemType.type (polígono de 6 puntos con bisel izquierdo). */
    private fun drawTypeShape(item: DiagramItem, scaledElementBox: KRectangle) {
        gc.stroke = foregroundColor(item)
        applyOccurrencePen(item)
        val background = resolveBackground(item)

        val bevel = (scaledElementBox.height * 0.30).toInt()
        val path = polygonType(scaledElementBox, bevel)
        val pathShifted = shiftPolygon(item, path)

        if (item.maxOccurrence == 1) {
            gc.fill = background
            fillPolygon(path)
            strokePolygon(path)
        } else {
            gc.fill = background
            fillPolygon(pathShifted)
            strokePolygon(pathShifted)
            gc.fill = background
            fillPolygon(path)
            strokePolygon(path)
        }
        gc.setLineDashes()

        if (item.isDisabled) drawHatch(item, scaledElementBox)
    }

    /** pathPoint[0..5] del C#. */
    private fun polygonType(box: KRectangle, bevel: Int): DoubleArray =
        doubleArrayOf(
            (box.x + bevel).toDouble(), box.y.toDouble(),                 // 0
            box.right.toDouble(), box.y.toDouble(),                       // 1
            box.right.toDouble(), box.bottom.toDouble(),                  // 2
            (box.x + bevel).toDouble(), box.bottom.toDouble(),            // 3
            (box.x + bevel).toDouble(), (box.bottom - bevel).toDouble(),  // 4
            box.x.toDouble(), (box.y + bevel).toDouble()                  // 5
        )

    /** ≈ case DiagramItemType.group (polígono de 8 puntos) + glifo del compositor. */
    private fun drawGroupShape(item: DiagramItem, scaledElementBox: KRectangle) {
        gc.stroke = foregroundColor(item)
        applyOccurrencePen(item)
        val background = resolveBackground(item)

        val bevel = (scaledElementBox.height * 0.30).toInt()
        val path = polygonGroup(scaledElementBox, bevel)
        val pathShifted = shiftPolygon(item, path)

        if (item.maxOccurrence == 1) {
            gc.fill = background
            fillPolygon(path)
            strokePolygon(path)
        } else {
            gc.fill = background
            fillPolygon(pathShifted)
            strokePolygon(pathShifted)
            gc.fill = background
            fillPolygon(path)
            strokePolygon(path)
        }
        gc.setLineDashes()

        if (item.isDisabled) drawHatch(item, scaledElementBox)

        when (item.groupType) {
            DiagramItemGroupType.Sequence -> drawSequenceGlyph(item)
            DiagramItemGroupType.Choice -> drawChoiceOrAllGlyph(item, all = false)
            DiagramItemGroupType.All -> drawChoiceOrAllGlyph(item, all = true)
            DiagramItemGroupType.Group -> {}
        }
    }

    /** pathPoint[0..7] del C#. */
    private fun polygonGroup(box: KRectangle, bevel: Int): DoubleArray =
        doubleArrayOf(
            (box.x + bevel).toDouble(), box.y.toDouble(),                 // 0
            (box.right - bevel).toDouble(), box.y.toDouble(),             // 1
            (box.right - bevel).toDouble(), (box.y + bevel).toDouble(),   // 2
            box.right.toDouble(), (box.bottom - bevel).toDouble(),        // 3
            box.right.toDouble(), box.bottom.toDouble(),                  // 4
            (box.x + bevel).toDouble(), box.bottom.toDouble(),            // 5
            (box.x + bevel).toDouble(), (box.bottom - bevel).toDouble(),  // 6
            box.x.toDouble(), (box.y + bevel).toDouble()                  // 7
        )

    /** pathPointShifted[i] = pathPoint[i] + ScaleSize(3,3) del C#. */
    private fun shiftPolygon(item: DiagramItem, path: DoubleArray): DoubleArray {
        val bevelShift = item.scaleSize(KSize(3, 3))
        val shifted = DoubleArray(path.size)
        for (i in path.indices step 2) {
            shifted[i] = path[i] + bevelShift.width
            shifted[i + 1] = path[i + 1] + bevelShift.height
        }
        return shifted
    }

    /** ≈ case DiagramItemGroupType.Sequence (línea horizontal con 3 bolas). */
    private fun drawSequenceGlyph(item: DiagramItem) {
        gc.stroke = foregroundColor(item)

        val p0 = item.location + KSize(0, item.elementBox.height / 2)
        val p1 = p0 + KSize(3, 0)
        val p2 = p1 + KSize(item.elementBox.width - 6, 0)
        val s1 = item.scalePoint(p1)
        val s2 = item.scalePoint(p2)
        drawLine(s1.x, s1.y, s2.x, s2.y)

        var pointSize = KSize(4, 4)
        val pointSize2 = KSize(pointSize.width / 2, pointSize.height / 2)
        val point2 = p0 + KSize(item.elementBox.width / 2, 0)
        val points = listOf(
            point2 + KSize(-5, 0) - pointSize2,
            point2 - pointSize2,
            point2 + KSize(5, 0) - pointSize2
        )
        pointSize = item.scaleSize(pointSize)
        gc.fill = foregroundColor(item)
        for (point in points) {
            val scaled = item.scalePoint(point)
            gc.fillOval(scaled.x.toDouble(), scaled.y.toDouble(), pointSize.width.toDouble(), pointSize.height.toDouble())
        }
    }

    /** ≈ case DiagramItemGroupType.Choice / All (líneas con 3 bolas). */
    private fun drawChoiceOrAllGlyph(item: DiagramItem, all: Boolean) {
        gc.stroke = foregroundColor(item)
        val yMiddle = item.elementBox.y + item.elementBox.height / 2
        val yUp = yMiddle - 4
        val yDown = yMiddle + 4
        val xMiddle = item.elementBox.x + item.elementBox.width / 2
        val xLeft2 = xMiddle - 4
        val xLeft1 = xLeft2 - 4
        val xLeft0 = xLeft1 - 4
        val xRight0 = xMiddle + 4
        val xRight1 = xRight0 + 4
        val xRight2 = xRight1 + 4

        var pointSize = KSize(4, 4)
        val pointSize2 = KSize(pointSize.width / 2, pointSize.height / 2)
        val points = listOf(
            KPoint(xMiddle, yUp) - pointSize2,
            KPoint(xMiddle, yMiddle) - pointSize2,
            KPoint(xMiddle, yDown) - pointSize2
        )
        pointSize = item.scaleSize(pointSize)

        fun line(x1: Int, y1: Int, x2: Int, y2: Int) {
            val a = item.scalePoint(KPoint(x1, y1))
            val b = item.scalePoint(KPoint(x2, y2))
            drawLine(a.x, a.y, b.x, b.y)
        }

        if (!all) {
            // Choice
            line(xLeft0, yMiddle, xLeft1, yMiddle)
            line(xLeft1, yMiddle, xLeft2, yUp)
            line(xRight0, yUp, xRight1, yUp)
            line(xRight0, yMiddle, xRight2, yMiddle)
            line(xRight0, yDown, xRight1, yDown)
            line(xRight1, yUp, xRight1, yDown)
        } else {
            // All
            line(xLeft2, yUp, xLeft1, yUp)
            line(xLeft2, yMiddle, xLeft0, yMiddle)
            line(xLeft2, yDown, xLeft1, yDown)
            line(xLeft1, yUp, xLeft1, yDown)

            line(xRight0, yUp, xRight1, yUp)
            line(xRight0, yMiddle, xRight2, yMiddle)
            line(xRight0, yDown, xRight1, yDown)
            line(xRight1, yUp, xRight1, yDown)
        }

        gc.fill = foregroundColor(item)
        for (point in points) {
            val scaled = item.scalePoint(point)
            gc.fillOval(scaled.x.toDouble(), scaled.y.toDouble(), pointSize.width.toDouble(), pointSize.height.toDouble())
        }
    }

    // ------------------------------------------------------------------
    // Helpers de dibujo
    // ------------------------------------------------------------------

    /** ≈ 255 - depth*8 del C#: escalones de gris para el bounding box. En el
     *  tema Oscuro se escalona desde el fondo oscuro (0x2A + depth*8) para no
     *  "quemar" la pantalla con cajas casi blancas. */
    private fun boundingBoxColor(depth: Int): Color {
        val level = if (palette == AppTheme.LIGHT) 255 - depth * 8 else 0x2A + depth * 8
        return Color.rgb(level, level, level)
    }

    /** Fondo del tema; si el nodo está seleccionado, el color de selección. */
    private fun resolveBackground(item: DiagramItem): Color =
        if (item.isSelected) selectedBackground else backgroundColor

    private fun foregroundColor(item: DiagramItem): Color = if (item.isDisabled) gray else foreground

    private fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int) {
        gc.strokeLine(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())
    }

    private fun fillRectBox(box: KRectangle) =
        gc.fillRect(box.x.toDouble(), box.y.toDouble(), box.width.toDouble(), box.height.toDouble())

    private fun strokeRectBox(box: KRectangle) =
        gc.strokeRect(box.x.toDouble(), box.y.toDouble(), box.width.toDouble(), box.height.toDouble())

    private fun isRectEmpty(box: KRectangle): Boolean = box.width == 0 && box.height == 0

    /** fillPolygon(x[], y[], n) del Canvas: desdobla el array de pares. */
    private fun fillPolygon(points: DoubleArray) {
        val n = points.size / 2
        val xs = DoubleArray(n)
        val ys = DoubleArray(n)
        for (i in 0 until n) {
            xs[i] = points[2 * i]
            ys[i] = points[2 * i + 1]
        }
        gc.fillPolygon(xs, ys, n)
    }

    private fun strokePolygon(points: DoubleArray) {
        val n = points.size / 2
        val xs = DoubleArray(n)
        val ys = DoubleArray(n)
        for (i in 0 until n) {
            xs[i] = points[2 * i]
            ys[i] = points[2 * i + 1]
        }
        gc.strokePolygon(xs, ys, n)
    }

    /** ≈ Pen.DashPattern = { max(2, ScaleInt(5)), max(1, ScaleInt(2)) }. */
    private fun setLineDashPattern(item: DiagramItem) {
        val dash1 = max(2.0, item.scaleInt(5).toDouble())
        val dash2 = max(1.0, item.scaleInt(2).toDouble())
        gc.setLineDashes(dash1, dash2)
    }

    /**
     * ≈ HatchBrush(HatchStyle.BackwardDiagonal, Gray, backgroundColor) para
     * nodos deshabilitados (aproximado con líneas diagonales grises).
     */
    private fun drawHatch(item: DiagramItem, box: KRectangle) {
        gc.save()
        gc.beginPath()
        gc.rect(box.x.toDouble(), box.y.toDouble(), box.width.toDouble(), box.height.toDouble())
        gc.clip()
        gc.stroke = gray
        val step = max(4.0, item.scaleInt(7).toDouble())
        val span = box.width + box.height
        var offset = -box.height.toDouble()
        while (offset < span) {
            gc.strokeLine(
                box.x + offset, box.bottom.toDouble(),
                box.x + offset + box.height, box.y.toDouble()
            )
            offset += step
        }
        gc.restore()
    }

    /** Texto centrado en la caja (≈ StringFormat Alignment/LineAlignment = Center). */
    private fun drawTextCentered(item: DiagramItem, text: String, font: FontDescription, box: KRectangle) {
        gc.setFont(JavaFxFonts.toJavaFx(font))
        gc.setFill(foregroundColor(item))
        gc.setTextAlign(TextAlignment.CENTER)
        gc.setTextBaseline(VPos.CENTER)
        gc.fillText(text, box.x + box.width / 2.0, box.y + box.height / 2.0)
    }

    /**
     * Documentación con envoltura de líneas dentro de la caja
     * (≈ DrawString con StringFormat Alignment=Near + NoClip del C#).
     */
    private fun drawWrappedText(item: DiagramItem, text: String, font: FontDescription, box: KRectangle) {
        gc.setFont(JavaFxFonts.toJavaFx(font))
        gc.setFill(foregroundColor(item))
        gc.setTextAlign(TextAlignment.LEFT)
        gc.setTextBaseline(VPos.TOP)

        val lineHeight = measureHeight(font)
        if (lineHeight <= 0.0) return

        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return

        var line = ""
        var y = box.y.toDouble()
        var index = 0
        while (index < words.size && y < box.bottom) {
            val candidate = if (line.isEmpty()) words[index] else "$line ${words[index]}"
            if (measureWidth(candidate, font) <= box.width || line.isEmpty()) {
                line = candidate
                index++
                if (index >= words.size || measureWidth("$line ${words[index]}", font) > box.width) {
                    gc.fillText(line, box.x.toDouble(), y)
                    line = ""
                    y += lineHeight
                }
            } else {
                gc.fillText(line, box.x.toDouble(), y)
                line = ""
                y += lineHeight
            }
        }
    }

    /** Ancho real del texto con la fuente indicada (≈ Graphics.MeasureString, cached). */
    private fun measureWidth(text: String, font: FontDescription): Double =
        TextMeasure.width(text, font)

    /** Alto de línea de la fuente (≈ métrica de MeasureString, cached). */
    private fun measureHeight(font: FontDescription): Double =
        TextMeasure.height(font)
}