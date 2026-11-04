//    XSD Diagram - Kotlin/JavaFX port
//    Port de XSDDiagrams\Rendering\DiagramSvgRenderer.cs
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
//    Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización)
//
//    Diferencias: la medición de texto para el "WrapText" del C# usaba el
//    Graphics de referencia; aquí se usa el [DrawingContext] del port
//    (JavaFxDrawingContext en la GUI).
package xsddiagramx.rendering

import java.io.Writer

/**
 * ≈ DiagramSvgRenderer: genera el SVG del diagrama (texto). El renderizado
 * se hace a partir de las cajas YA calculadas (ScaleRectangle sobre el
 * layout vigente del diagrama), no re-pinta nada por sí mismo.
 */
class DiagramSvgRenderer(
    private val writer: Writer,
    /** ≈ referenceGraphics: contexto para medir texto (documentaciones). */
    private val drawingContext: DrawingContext? = null,
) : DiagramRenderer() {

    private var boundingBox = KRectangle()
    private var xsdDocCounter = 0

    override val name: String get() = "SVG"

    override fun beginItemsRender() {
        xsdDocCounter = 0

        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" standalone=\"no\"?>\r\n")
        sb.append("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\r\n")
        if (boundingBox.width > 0 && boundingBox.height > 0)
            sb.append("<svg width=\"${boundingBox.width}\" height=\"${boundingBox.height}\" version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\">\r\n")
        else
            sb.append("<svg width=\"100%\" height=\"100%\" version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\">\r\n")
        writer.write(sb.toString())
    }

    override fun endItemsRender() {
        writer.write("</svg>\r\n")
    }

    /** ≈ Render(Diagram). */
    override fun render(diagram: Diagram) {
        boundingBox = diagram.scaleRectangle(diagram.boundingBox)
        beginItemsRender()

        for (element in diagram.rootElements) {
            render(element)
        }

        endItemsRender()
    }

    /** ≈ Render(DiagramItem). */
    override fun render(drawingItem: DiagramItem) {
        val showDocumentation =
            drawingItem.diagram.showDocumentation && !isRectEmpty(drawingItem.documentationBox) && drawingContext != null

        // Draw the children
        if (drawingItem.showChildElements) {
            for (element in drawingItem.childElements) {
                render(element)
                writer.write("\r\n")
            }
        }

        val backgroundBrush = "fill:rgb(255,255,255)"
        val foregroundColor = "rgb(0,0,0)"
        val foregroundBrush = "fill:$foregroundColor"
        val foregroundPen = "stroke:$foregroundColor;stroke-width:1"
        val foregroundRoundPen = "$foregroundPen;stroke-linecap:round"
        val dashed = "stroke-dasharray:4,1"

        val scaledElementBox = drawingItem.scaleRectangle(drawingItem.elementBox)

        // Draw the children lines
        if (drawingItem.showChildElements && drawingItem.childElements.isNotEmpty()) {
            if (drawingItem.childElements.size == 1 && !showDocumentation) {
                val parentMidleY = drawingItem.scaleInt(drawingItem.location.y + drawingItem.size.height / 2)
                svgLine(
                    foregroundRoundPen,
                    drawingItem.scaleInt(drawingItem.location.x + drawingItem.size.width),
                    parentMidleY, drawingItem.scaleInt(drawingItem.childElements[0].location.x), parentMidleY
                )
            } else if (drawingItem.childElements.size > 1 || showDocumentation) {
                val firstElement = drawingItem.childElements.first()
                val lastElement = drawingItem.childElements.last()
                val verticalLine = drawingItem.scaleInt(firstElement.boundingBox.x)

                for (element in drawingItem.childElements) {
                    if (element.inheritFrom == null) {
                        val currentMidleY = drawingItem.scaleInt(element.location.y + element.size.height / 2)
                        svgLine(
                            foregroundRoundPen, verticalLine, currentMidleY,
                            drawingItem.scaleInt(element.location.x), currentMidleY
                        )
                    }
                }

                val parentMidleY = drawingItem.scaleInt(drawingItem.location.y + drawingItem.size.height / 2)
                var firstMidleY = drawingItem.scaleInt(firstElement.location.y + firstElement.size.height / 2)
                firstMidleY = minOf(firstMidleY, parentMidleY)
                var lastMidleY = drawingItem.scaleInt(lastElement.location.y + lastElement.size.height / 2)
                lastMidleY = maxOf(lastMidleY, parentMidleY)
                svgLine(foregroundRoundPen, verticalLine, firstMidleY, verticalLine, lastMidleY)
                svgLine(
                    foregroundRoundPen,
                    drawingItem.scaleInt(drawingItem.location.x + drawingItem.size.width),
                    parentMidleY, verticalLine, parentMidleY
                )
            }
        }

        // Draw the inheritor line
        val inheritFrom = drawingItem.inheritFrom
        if (inheritFrom != null) {
            val foregroundInheritPen = "$foregroundPen;$dashed"

            val p1 = KPoint(
                drawingItem.scaleInt(inheritFrom.location.x - 5),
                drawingItem.scaleInt(inheritFrom.location.y + inheritFrom.size.height + 5)
            )
            val p2 = KPoint(
                drawingItem.scaleInt(drawingItem.location.x - 5),
                drawingItem.scaleInt(drawingItem.location.y - 5)
            )
            svgLine(foregroundInheritPen, p1, p2)
            svgLine(
                foregroundInheritPen, p2,
                KPoint(drawingItem.scaleInt(drawingItem.location.x), drawingItem.scaleInt(drawingItem.location.y))
            )

            val targetPoint = KPoint(
                drawingItem.scaleInt(inheritFrom.location.x - 3),
                drawingItem.scaleInt(inheritFrom.location.y + inheritFrom.size.height + 3)
            )
            svgLine(foregroundInheritPen, targetPoint, p1)
            val pathPoint = arrayOf(
                targetPoint,
                KPoint(targetPoint.x + drawingItem.scaleInt(2), targetPoint.y + drawingItem.scaleInt(2)),
                KPoint(targetPoint.x + drawingItem.scaleInt(3), targetPoint.y - drawingItem.scaleInt(3)),
                KPoint(targetPoint.x - drawingItem.scaleInt(2), targetPoint.y - drawingItem.scaleInt(2)),
                KPoint(targetPoint.x, targetPoint.y)
            )

            val path = svgPolygonToDrawCommand(pathPoint)
            svgPath("$backgroundBrush;$foregroundPen", path)
        }

        when (drawingItem.itemType) {
            DiagramItemType.element -> {
                // Draw the main shape following the min/max occurences
                var foregroundBoxPen = foregroundPen

                if (drawingItem.minOccurrence == 0) {
                    foregroundBoxPen += ";$dashed"
                }
                if (drawingItem.maxOccurrence == 1) {
                    svgRectangle("$backgroundBrush;$foregroundBoxPen", scaledElementBox)
                } else {
                    val elementBoxShifted = scaledElementBox
                    elementBoxShifted.offset(
                        drawingItem.scalePoint(KPoint(3, 3)).x,
                        drawingItem.scalePoint(KPoint(3, 3)).y
                    )
                    svgRectangle("$backgroundBrush;$foregroundBoxPen", elementBoxShifted)
                    svgRectangle("$backgroundBrush;$foregroundBoxPen", scaledElementBox)
                }
            }

            DiagramItemType.type -> {
                // Draw the main shape following the min/max occurences
                val bevel = (scaledElementBox.height * 0.30).toInt()
                val pathPoint = Array(6) { KPoint(0, 0) }
                pathPoint[0] = KPoint(scaledElementBox.x, scaledElementBox.y)
                pathPoint[5] = KPoint(scaledElementBox.x, scaledElementBox.y)
                pathPoint[1] = KPoint(scaledElementBox.right, scaledElementBox.y)
                pathPoint[2] = KPoint(scaledElementBox.x + scaledElementBox.width, scaledElementBox.y + scaledElementBox.height)
                pathPoint[3] = KPoint(scaledElementBox.x, scaledElementBox.bottom)
                pathPoint[4] = KPoint(pathPoint[3].x, pathPoint[3].y)
                pathPoint[0].x += bevel
                pathPoint[3].x += bevel
                pathPoint[4].y -= bevel
                pathPoint[5].y += bevel

                val path = svgPolygonToDrawCommand(pathPoint)

                val scaledShiftedBevel = drawingItem.scaleSize(KSize(3, 3))
                val pathPointShifted = Array(6) { i ->
                    KPoint(pathPoint[i].x + scaledShiftedBevel.width, pathPoint[i].y + scaledShiftedBevel.height)
                }

                val pathShifted = svgPolygonToDrawCommand(pathPointShifted)

                var foregroundBoxPen = foregroundPen
                if (drawingItem.minOccurrence == 0) {
                    foregroundBoxPen += ";$dashed"
                }
                if (drawingItem.maxOccurrence == 1) {
                    svgPath("$backgroundBrush;$foregroundBoxPen", path)
                } else {
                    svgPath("$backgroundBrush;$foregroundBoxPen", pathShifted)
                    svgPath("$backgroundBrush;$foregroundBoxPen", path)
                }
            }

            DiagramItemType.group -> {
                // Draw the main shape following the min/max occurences
                val bevel = (scaledElementBox.height * 0.30).toInt()
                val pathPoint = Array(8) { KPoint(scaledElementBox.x, scaledElementBox.y) }
                pathPoint[1] = KPoint(scaledElementBox.right, scaledElementBox.y)
                pathPoint[2] = KPoint(pathPoint[1].x, pathPoint[1].y)
                pathPoint[3] = KPoint(scaledElementBox.x + scaledElementBox.width, scaledElementBox.y + scaledElementBox.height)
                pathPoint[4] = KPoint(pathPoint[3].x, pathPoint[3].y)
                pathPoint[5] = KPoint(scaledElementBox.x, scaledElementBox.bottom)
                pathPoint[6] = KPoint(pathPoint[5].x, pathPoint[5].y)
                pathPoint[0].x += bevel
                pathPoint[1].x -= bevel
                pathPoint[2].y += bevel
                pathPoint[3].y -= bevel
                pathPoint[4].x -= bevel
                pathPoint[5].x += bevel
                pathPoint[6].y -= bevel
                pathPoint[7].y += bevel

                val path = svgPolygonToDrawCommand(pathPoint)

                val scaledShiftedBevel = drawingItem.scaleSize(KSize(3, 3))
                val pathPointShifted = Array(8) { i ->
                    KPoint(pathPoint[i].x + scaledShiftedBevel.width, pathPoint[i].y + scaledShiftedBevel.height)
                }

                val pathShifted = svgPolygonToDrawCommand(pathPointShifted)

                var foregroundBoxPen = foregroundPen
                if (drawingItem.minOccurrence == 0) {
                    foregroundBoxPen += ";$dashed"
                }
                if (drawingItem.maxOccurrence == 1) {
                    svgPath("$backgroundBrush;$foregroundBoxPen", path)
                } else {
                    svgPath("$backgroundBrush;$foregroundBoxPen", pathShifted)
                    svgPath("$backgroundBrush;$foregroundBoxPen", path)
                }

                // Draw the group type
                when (drawingItem.groupType) {
                    DiagramItemGroupType.Group -> {
                        // El C# tampoco dibuja nada específico para Group.
                    }
                    DiagramItemGroupType.Sequence -> {
                        val p0 = KPoint(
                            drawingItem.location.x,
                            drawingItem.location.y + drawingItem.elementBox.height / 2
                        )
                        val p1 = KPoint(p0.x + 3, p0.y)
                        val p2 = KPoint(p1.x + drawingItem.elementBox.width - 6, p1.y)
                        svgLine(foregroundPen, drawingItem.scalePoint(p1), drawingItem.scalePoint(p2))
                        val point2 = KPoint(p0.x + drawingItem.elementBox.width / 2, p0.y)
                        val point1 = KPoint(point2.x - 5, point2.y)
                        val point3 = KPoint(point2.x + 5, point2.y)
                        var pointSize = KSize(4, 4)
                        val pointSize2 = KSize(pointSize.width / 2, pointSize.height / 2)
                        point1.x -= pointSize2.width; point1.y -= pointSize2.height
                        point2.x -= pointSize2.width; point2.y -= pointSize2.height
                        point3.x -= pointSize2.width; point3.y -= pointSize2.height
                        pointSize = drawingItem.scaleSize(pointSize)
                        svgEllipse(foregroundColor, KRectangle(drawingItem.scalePoint(point1), pointSize))
                        svgEllipse(foregroundColor, KRectangle(drawingItem.scalePoint(point2), pointSize))
                        svgEllipse(foregroundColor, KRectangle(drawingItem.scalePoint(point3), pointSize))
                    }

                    DiagramItemGroupType.Choice -> {
                        val yMiddle = drawingItem.elementBox.y + drawingItem.elementBox.height / 2
                        val yUp = yMiddle - 4
                        val yDown = yMiddle + 4
                        val xMiddle = drawingItem.elementBox.x + drawingItem.elementBox.width / 2
                        val xLeft2 = xMiddle - 4
                        val xLeft1 = xLeft2 - 4
                        val xLeft0 = xLeft1 - 4
                        val xRight0 = xMiddle + 4
                        val xRight1 = xRight0 + 4
                        val xRight2 = xRight1 + 4

                        var pointSize = KSize(4, 4)
                        val pointSize2 = KSize(pointSize.width / 2, pointSize.height / 2)
                        val point1 = KPoint(xMiddle - pointSize2.width, yUp - pointSize2.height)
                        val point2 = KPoint(xMiddle - pointSize2.width, yMiddle - pointSize2.height)
                        val point3 = KPoint(xMiddle - pointSize2.width, yDown - pointSize2.height)
                        pointSize = drawingItem.scaleSize(pointSize)
                        svgLine(
                            foregroundPen,
                            drawingItem.scalePoint(KPoint(xLeft0, yMiddle)),
                            drawingItem.scalePoint(KPoint(xLeft1, yMiddle))
                        )
                        svgLine(
                            foregroundPen,
                            drawingItem.scalePoint(KPoint(xLeft1, yMiddle)),
                            drawingItem.scalePoint(KPoint(xLeft2, yUp))
                        )
                        svgLine(
                            foregroundPen,
                            drawingItem.scalePoint(KPoint(xRight0, yUp)),
                            drawingItem.scalePoint(KPoint(xRight1, yUp))
                        )
                        svgLine(
                            foregroundPen,
                            drawingItem.scalePoint(KPoint(xRight0, yMiddle)),
                            drawingItem.scalePoint(KPoint(xRight2, yMiddle))
                        )
                        svgLine(
                            foregroundPen,
                            drawingItem.scalePoint(KPoint(xRight0, yDown)),
                            drawingItem.scalePoint(KPoint(xRight1, yDown))
                        )
                        svgLine(
                            foregroundPen,
                            drawingItem.scalePoint(KPoint(xRight1, yUp)),
                            drawingItem.scalePoint(KPoint(xRight1, yDown))
                        )
                        svgEllipse(foregroundColor, KRectangle(drawingItem.scalePoint(point1), pointSize))
                        svgEllipse(foregroundColor, KRectangle(drawingItem.scalePoint(point2), pointSize))
                        svgEllipse(foregroundColor, KRectangle(drawingItem.scalePoint(point3), pointSize))
                    }

                    DiagramItemGroupType.All -> {
                        val yMiddle = drawingItem.elementBox.y + drawingItem.elementBox.height / 2
                        val yUp = yMiddle - 4
                        val yDown = yMiddle + 4
                        val xMiddle = drawingItem.elementBox.x + drawingItem.elementBox.width / 2
                        val xLeft2 = xMiddle - 4
                        val xLeft1 = xLeft2 - 4
                        val xLeft0 = xLeft1 - 4
                        val xRight0 = xMiddle + 4
                        val xRight1 = xRight0 + 4
                        val xRight2 = xRight1 + 4

                        var pointSize = KSize(4, 4)
                        val pointSize2 = KSize(pointSize.width / 2, pointSize.height / 2)
                        val point1 = KPoint(xMiddle - pointSize2.width, yUp - pointSize2.height)
                        val point2 = KPoint(xMiddle - pointSize2.width, yMiddle - pointSize2.height)
                        val point3 = KPoint(xMiddle - pointSize2.width, yDown - pointSize2.height)
                        pointSize = drawingItem.scaleSize(pointSize)
                        svgLine(
                            foregroundPen,
                            drawingItem.scalePoint(KPoint(xLeft2, yUp)),
                            drawingItem.scalePoint(KPoint(xLeft1, yUp))
                        )
                        svgLine(
                            foregroundPen,
                            drawingItem.scalePoint(KPoint(xLeft2, yMiddle)),
                            drawingItem.scalePoint(KPoint(xLeft0, yMiddle))
                        )
                        svgLine(
                            foregroundPen,
                            drawingItem.scalePoint(KPoint(xLeft2, yDown)),
                            drawingItem.scalePoint(KPoint(xLeft1, yDown))
                        )
                        svgLine(
                            foregroundPen,
                            drawingItem.scalePoint(KPoint(xLeft1, yUp)),
                            drawingItem.scalePoint(KPoint(xLeft1, yDown))
                        )

                        svgLine(
                            foregroundPen,
                            drawingItem.scalePoint(KPoint(xRight0, yUp)),
                            drawingItem.scalePoint(KPoint(xRight1, yUp))
                        )
                        svgLine(
                            foregroundPen,
                            drawingItem.scalePoint(KPoint(xRight0, yMiddle)),
                            drawingItem.scalePoint(KPoint(xRight2, yMiddle))
                        )
                        svgLine(
                            foregroundPen,
                            drawingItem.scalePoint(KPoint(xRight0, yDown)),
                            drawingItem.scalePoint(KPoint(xRight1, yDown))
                        )
                        svgLine(
                            foregroundPen,
                            drawingItem.scalePoint(KPoint(xRight1, yUp)),
                            drawingItem.scalePoint(KPoint(xRight1, yDown))
                        )
                        svgEllipse(foregroundColor, KRectangle(drawingItem.scalePoint(point1), pointSize))
                        svgEllipse(foregroundColor, KRectangle(drawingItem.scalePoint(point2), pointSize))
                        svgEllipse(foregroundColor, KRectangle(drawingItem.scalePoint(point3), pointSize))
                    }
                }
            }
        }

        val fontScale = 0.8f

        // Draw text
        if (drawingItem.name.isNotEmpty()) {
            val style = "font-family:${drawingItem.font.family};font-size:${drawingItem.font.sizePx * fontScale}pt;" +
                "fill:$foregroundColor;font-weight:bold;text-anchor:middle;dominant-baseline:central"
            svgText(
                if (drawingItem.diagram.showType && drawingItem.type.isNotEmpty())
                    drawingItem.name + ":" + drawingItem.type
                else drawingItem.name,
                style,
                KRectangle(scaledElementBox.x, scaledElementBox.y, scaledElementBox.width, scaledElementBox.height)
            )
        }

        // Draw Documentation
        if (showDocumentation) {
            val text = drawingItem.getTextDocumentation()
            if (text != null) {
                val scaledDocumentationBox = drawingItem.scaleRectangle(drawingItem.documentationBox)
                val lines = wrapText(
                    drawingContext!!,
                    drawingItem.documentationFont,
                    text,
                    scaledDocumentationBox.width * fontScale * 0.6f
                )
                val style = "font-family:${drawingItem.documentationFont.family};" +
                    "font-size:${drawingItem.documentationFont.sizePx * fontScale * 0.9}pt;" +
                    "fill:$foregroundColor;font-weight:normal;text-anchor:start;dominant-baseline:central;" +
                    "inline-size=${scaledDocumentationBox.width}"
                val fontSizeAdjustement = drawingItem.documentationFont.sizePx * fontScale * 1.4f
                svgText(
                    lines, style,
                    KPoint(scaledDocumentationBox.x, scaledDocumentationBox.y), fontSizeAdjustement,
                    KSize(scaledDocumentationBox.width, scaledDocumentationBox.height)
                )
            }
        }

        // Draw occurences small text
        if (drawingItem.diagram.alwaysShowOccurence ||
            (drawingItem.maxOccurrence > 1 || drawingItem.maxOccurrence == -1)
        ) {
            val occurences = "${drawingItem.minOccurrence}.." +
                (if (drawingItem.maxOccurrence == -1) "\u221E" else drawingItem.maxOccurrence.toString())
            val compactLayoutDensity = drawingItem.diagram.compactLayoutDensity
            val pointOccurencesX = drawingItem.diagram.scale *
                (drawingItem.location.x + drawingItem.size.width + (if (compactLayoutDensity) +23 else -10))
            val pointOccurencesY = drawingItem.diagram.scale *
                (drawingItem.location.y + drawingItem.size.height + (if (compactLayoutDensity) -17 else +10))
            val style = "font-family:${drawingItem.smallFont.family};font-size:${drawingItem.smallFont.sizePx * fontScale}pt;" +
                "fill:$foregroundColor;text-anchor:end;dominant-baseline:central"
            svgText(occurences, style, KPoint(pointOccurencesX.toInt(), pointOccurencesY.toInt()))
        }

        // Draw type
        if (drawingItem.isSimpleContent) {
            var currentPoint = KPoint(scaledElementBox.x + 2, scaledElementBox.y + 2)
            svgLine(foregroundPen, currentPoint, KPoint(currentPoint.x + drawingItem.scaleInt(8), currentPoint.y))
            currentPoint = KPoint(currentPoint.x, currentPoint.y + 2)
            svgLine(foregroundPen, currentPoint, KPoint(currentPoint.x + drawingItem.scaleInt(6), currentPoint.y))
            currentPoint = KPoint(currentPoint.x, currentPoint.y + 2)
            svgLine(foregroundPen, currentPoint, KPoint(currentPoint.x + drawingItem.scaleInt(6), currentPoint.y))
            currentPoint = KPoint(currentPoint.x, currentPoint.y + 2)
            svgLine(foregroundPen, currentPoint, KPoint(currentPoint.x + drawingItem.scaleInt(6), currentPoint.y))
        }

        // Draw reference arrow
        if (drawingItem.isReference) {
            val arrowPen = "stroke:$foregroundColor;stroke-width:${drawingItem.diagram.scale * 2.0f}"
            var basePoint = KPoint(drawingItem.elementBox.x + 1, drawingItem.elementBox.bottom - 1)
            var targetPoint = KPoint(basePoint.x + 3, basePoint.y - 3)
            basePoint = drawingItem.scalePoint(basePoint)
            targetPoint = drawingItem.scalePoint(targetPoint)
            if (drawingItem.itemType == DiagramItemType.group) {
                val bevel = (scaledElementBox.height * 0.30).toInt()
                val groupCornerOffset = (bevel * 0.424264068713).toInt() // 0.6/sqr(2)
                basePoint.x += groupCornerOffset
                basePoint.y -= groupCornerOffset
                targetPoint.x += groupCornerOffset
                targetPoint.y -= groupCornerOffset
            }
            svgLine(arrowPen, basePoint, targetPoint)

            val pathPoint = arrayOf(
                targetPoint,
                KPoint(targetPoint.x + drawingItem.scaleInt(2), targetPoint.y + drawingItem.scaleInt(2)),
                KPoint(targetPoint.x + drawingItem.scaleInt(3), targetPoint.y - drawingItem.scaleInt(3)),
                KPoint(targetPoint.x - drawingItem.scaleInt(2), targetPoint.y - drawingItem.scaleInt(2)),
                KPoint(targetPoint.x, targetPoint.y)
            )

            val path = svgPolygonToDrawCommand(pathPoint)
            svgPath(foregroundBrush, path)
        }

        // Draw children expand box
        if (drawingItem.hasChildElements) {
            val scaledChildExpandButtonBox = drawingItem.scaleRectangle(drawingItem.childExpandButtonBox)
            svgRectangle("$backgroundBrush;$foregroundPen", scaledChildExpandButtonBox)

            val middle = KPoint(scaledChildExpandButtonBox.width / 2, scaledChildExpandButtonBox.height / 2)
            val borderPadding = maxOf(2, drawingItem.scaleInt(2))

            var p1 = KPoint(scaledChildExpandButtonBox.x + borderPadding, scaledChildExpandButtonBox.y + middle.y)
            val p2 = KPoint(scaledChildExpandButtonBox.right - borderPadding, p1.y)
            svgLine(foregroundPen, p1, p2)
            if (!drawingItem.showChildElements) {
                p1 = KPoint(scaledChildExpandButtonBox.x + middle.x, scaledChildExpandButtonBox.y + borderPadding)
                val p3 = KPoint(p1.x, scaledChildExpandButtonBox.bottom - borderPadding)
                svgLine(foregroundPen, p1, p3)
            }
        }
    }

    // ------------------------------------------------------------------
    // Emisión SVG
    // ------------------------------------------------------------------

    /** Emite una <line> SVG entre dos puntos. ≈ DrawLine del Graphics. */
    private fun svgLine(pen: String, pt1: KPoint, pt2: KPoint) =
        svgLine(pen, pt1.x, pt1.y, pt2.x, pt2.y)

    /** Emite una <line> SVG con coordenadas sueltas. ≈ DrawLine del Graphics. */
    private fun svgLine(pen: String, x1: Int, y1: Int, x2: Int, y2: Int) {
        writer.write("<line x1=\"$x1\" y1=\"$y1\" x2=\"$x2\" y2=\"$y2\" style=\"$pen\"/>\r\n")
    }

    /** Emite un <rect> SVG (sólo borde, sin relleno). ≈ DrawRectangle. */
    private fun svgRectangle(pen: String, rect: KRectangle) {
        writer.write(
            "<rect x=\"${rect.x}\" y=\"${rect.y}\" width=\"${rect.width}\" height=\"${rect.height}\" style=\"$pen\"/>\r\n"
        )
    }

    /** Emite una <ellipse> SVG inscrita en el rectángulo (≈ DrawEllipse). */
    private fun svgEllipse(brush: String, rect: KRectangle) {
        writer.write(
            "<ellipse cx=\"${rect.x + rect.width / 2}\" cy=\"${rect.y + rect.height / 2}\" " +
                "rx=\"${rect.width / 2}\" ry=\"${rect.height / 2}\" style=\"$brush\"/>\r\n"
        )
    }

    /** Emite un <path> SVG de línea continua (≈ DrawPath). */
    private fun svgPath(style: String, drawCommand: String) {
        writer.write("<path d=\"$drawCommand\" style=\"$style\"/>\r\n")
    }

    /** Emite un <text> SVG en un punto (≈ DrawString). */
    private fun svgText(text: String, style: String, point: KPoint) {
        writer.write(
            "<text x=\"${point.x}\" y=\"${point.y}\" style=\"$style\">${xmlEscape(text)}</text>\r\n"
        )
    }

    /** Emite un <text> SVG centrado en el rectángulo (≈ DrawString centrado). */
    private fun svgText(text: String, style: String, rect: KRectangle) {
        writer.write(
            "<text x=\"${rect.x + rect.width / 2.0}\" y=\"${rect.y + rect.height / 2.0}\" style=\"$style\">${xmlEscape(text)}</text>\r\n"
        )
    }

    /**
     * Emite un bloque de documentación (multi-línea) con clip-path para acotar
     * el alto visible (≈ el DocumentBox del diagrama GDI/SVG).
     */
    private fun svgText(text: List<String>, style: String, point: KPoint, fontSize: Float, size: KSize) {
        val x = point.x; val y = point.y
        writer.write(
            "<clipPath id=\"xsddoc$xsdDocCounter\"><rect x=\"$x\" y=\"${y - fontSize.toInt()}\" " +
                "width=\"${size.width * 2}\" height=\"${size.height + 2 * fontSize.toInt()}\"/></clipPath>\r\n"
        )
        val yy = y + (fontSize * 0.5f).toInt()
        writer.write(
            "<text x=\"$x\" y=\"$yy\" style=\"$style\" clip-path=\"url(#xsddoc$xsdDocCounter)\">" +
                (if (text.isNotEmpty()) xmlEscape(text[0]) else "") + "\r\n"
        )
        for (i in 1 until text.size)
            writer.write("<tspan x=\"$x\" y=\"${yy + i * fontSize.toInt()}\">${xmlEscape(text[i])}</tspan>\r\n")
        writer.write("</text>\r\n")
        xsdDocCounter++
    }

    /** Convierte los vértices de un polígono al comando d="M... L... Z" de SVG. */
    private fun svgPolygonToDrawCommand(pathPoint: Array<KPoint>): String {
        val result = StringBuilder()
        for (i in pathPoint.indices) {
            result.append(if (i == 0) 'M' else 'L').append(pathPoint[i].x).append(' ').append(pathPoint[i].y).append(' ')
        }
        result.append('Z')
        return result.toString()
    }

    /** ≈ isRectEmpty (Rect.IsEmpty del System.Drawing). */
    private fun isRectEmpty(box: KRectangle): Boolean = box.width == 0 && box.height == 0

    /** ≈ WrapText(Graphics, Font, text, pixels): medición vía DrawingContext. */
    private fun wrapText(
        g: DrawingContext,
        font: FontDescription,
        text: String,
        pixels: Float,
    ): List<String> {
        val originalLines = text.split(" ")
        val wrappedLines = ArrayList<String>()
        val actualLine = StringBuilder()
        var actualWidth = 0f

        for (item in originalLines) {
            val wordMeasure = g.measureString(item, font)
            actualWidth += wordMeasure.width.toFloat()
            actualLine.append(item).append(" ")
            if (actualWidth > pixels) {
                wrappedLines.add(actualLine.toString())
                actualLine.setLength(0)
                actualWidth = 0f
            }
        }

        if (actualLine.length > 0)
            wrappedLines.add(actualLine.toString())

        return wrappedLines
    }

    companion object {
        /** Escapado XML mínimo para el contenido de <text>. */
        fun xmlEscape(s: String): String =
            s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }
}
