//    XSD Diagram - Kotlin/JavaFX port
//    Port de XSDDiagrams\Rendering\DiagramItem.cs
//
//    Licencias del original: LGPL / MS-PL (véase LICENSE-LGPL.txt / LICENSE-MS-PL.txt)
//    Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización)
//
//    Nodo visual del diagrama: estado, geometría (medida + localización
//    recursivas) y hit-test. Port 1:1 con dos sustituciones:
//      - System.Drawing.Point/Size/Rectangle -> KPoint/KSize/KRectangle.
//      - System.Drawing.Font / Graphics -> FontDescription / DrawingContext.
package xsddiagramx.rendering

import xsddiagramx.DiagramHelpers
import xsddiagramx.parsing.Annotated
import xsddiagramx.parsing.element

class DiagramItem {

    // ------------------------------------------------------------
    // Private state (mismos nombres que el C#)
    // ------------------------------------------------------------

    private var _depth = 0
    private var _childExpandButtonSize = 10
    private var _minOccurrence = -1
    private var _maxOccurrence = -1

    private var _isReference = false
    private var _isSimpleContent = false
    private var _isDisabled = false
    private var _isSelected = false
    private var _hasChildElements = false
    private var _showChildElements = false

    private var _size = KSize(100, 25)
    private var _margin = KSize(10, 5)
    private var _padding = KSize(10, 15)
    private var _paddingCompact = KSize(10, 4)
    private var _location = KPoint(0, 0)

    private var _name: String = ""
    private var _nameSpace: String = ""
    private var _type: String = ""

    private var _elementBox = KRectangle()
    private var _childExpandButtonBox = KRectangle()
    private var _documentationBox = KRectangle()
    private var _boundingBox = KRectangle()
    private var _documentationMinWidth = 100

    private var _diagram: Diagram? = null
    private var _parent: DiagramItem? = null
    private var _inheritFrom: DiagramItem? = null
    private var _itemType: DiagramItemType = DiagramItemType.element
    private var _groupType: DiagramItemGroupType? = null

    private val _childElements = ArrayList<DiagramItem>()

    private var _tabSchema: Any? = null

    // ------------------------------------------------------------
    // Properties
    // ------------------------------------------------------------

    var diagram: Diagram
        get() = _diagram!!
        set(value) { _diagram = value }

    var parent: DiagramItem?
        get() = _parent
        set(value) { _parent = value }

    var inheritFrom: DiagramItem?
        get() = _inheritFrom
        set(value) { _inheritFrom = value }

    var name: String
        get() = _name
        set(value) { _name = value }

    var nameSpace: String
        get() = _nameSpace
        set(value) { _nameSpace = value }

    var type: String
        get() = _type
        set(value) { _type = value }

    var itemType: DiagramItemType
        get() = _itemType
        set(value) { _itemType = value }

    var groupType: DiagramItemGroupType
        get() = _groupType ?: DiagramItemGroupType.Group
        set(value) { _groupType = value }

    var isReference: Boolean
        get() = _isReference
        set(value) { _isReference = value }

    var isSimpleContent: Boolean
        get() = _isSimpleContent
        set(value) { _isSimpleContent = value }

    var isDisabled: Boolean
        get() = _isDisabled
        set(value) { _isDisabled = value }

    var isSelected: Boolean
        get() = _isSelected
        set(value) { _isSelected = value }

    var minOccurrence: Int
        get() = _minOccurrence
        set(value) { _minOccurrence = value }

    var maxOccurrence: Int
        get() = _maxOccurrence
        set(value) { _maxOccurrence = value }

    var hasChildElements: Boolean
        get() = _hasChildElements
        set(value) { _hasChildElements = value }

    /**
     * Equivale a la propiedad `ShowChildElements` del C#, que limpiaba la
     * búsqueda activa cada vez que se conmutaba.
     */
    var showChildElements: Boolean
        get() = _showChildElements
        set(value) {
            if (_showChildElements != value) {
                _showChildElements = value
                diagram.clearSearch()
            }
        }

    val fullName: String get() = "$_nameSpace:${_itemType.value}:$_name"

    val font: FontDescription get() = _diagram?.font ?: FontDescription()
    val smallFont: FontDescription get() = _diagram?.smallFont ?: FontDescription()
    val documentationFont: FontDescription get() = _diagram?.documentationFont ?: FontDescription()

    var location: KPoint
        get() = _location
        set(value) { _location = value }

    var size: KSize
        get() = _size
        set(value) { _size = value }

    var margin: KSize
        get() = _margin
        set(value) { _margin = value }

    var padding: KSize
        get() = _padding
        set(value) { _padding = value }

    var paddingCompact: KSize
        get() = _paddingCompact
        set(value) { _paddingCompact = value }

    var elementBox: KRectangle
        get() = _elementBox
        set(value) { _elementBox = value }

    var childExpandButtonBox: KRectangle
        get() = _childExpandButtonBox
        set(value) { _childExpandButtonBox = value }

    var documentationBox: KRectangle
        get() = _documentationBox
        set(value) { _documentationBox = value }

    var boundingBox: KRectangle
        get() = _boundingBox
        set(value) { _boundingBox = value }

    var childExpandButtonSize: Int
        get() = _childExpandButtonSize
        set(value) { _childExpandButtonSize = value }

    var depth: Int
        get() = _depth
        set(value) { _depth = value }

    val childElements: List<DiagramItem> get() = _childElements

    /** Equivale a List.Add sobre ChildElements en el C#. */
    fun addChild(child: DiagramItem) {
        _childElements.add(child)
    }

    /** Equivale a List.Remove sobre ChildElements en el C#. */
    fun removeChild(child: DiagramItem) {
        _childElements.remove(child)
    }

    /**
     * Referencia al nodo del modelo (`XMLSchema.openAttrs` en el C#) del que
     * procede este nodo visual.
     */
    var tabSchema: Any?
        get() = _tabSchema
        set(value) { _tabSchema = value }

    // ------------------------------------------------------------
    // Escalado (delegado en el diagrama, como en el C#)
    // ------------------------------------------------------------

    fun scaleInt(integer: Int): Int = diagram.scaleInt(integer)
    fun scalePoint(point: KPoint): KPoint = diagram.scalePoint(point)
    fun scaleSize(point: KSize): KSize = diagram.scaleSize(point)
    fun scaleRectangle(rectangle: KRectangle): KRectangle = diagram.scaleRectangle(rectangle)

    // ------------------------------------------------------------
    // Documentación / tipo anotado
    // ------------------------------------------------------------

    /** Equivale a GetTypeAnnotation(): `"` + element.type`".Substring(idx+1)`. */
    fun getTypeAnnotation(): String? {
        val el = _tabSchema as? element ?: return null
        val type = el.type ?: return null
        val t = type.toString() // C#: "" + element.type (XmlQualifiedName.ToString())
        val idx = t.lastIndexOf(':')
        return t.substring(idx + 1)
    }

    /** Equivale a GetTextDocumentation(). */
    fun getTextDocumentation(): String? {
        val annotated = _tabSchema as? Annotated ?: return null
        val annotation = annotated.annotation ?: return null
        return DiagramHelpers.getAnnotationText(annotation)
    }

    // ------------------------------------------------------------
    // Medida y localización (layout del árbol)
    // ------------------------------------------------------------

    /** Equivale a GenerateMeasure(Graphics g). */
    fun generateMeasure(g: DrawingContext) {
        val padding = if (diagram.compactLayoutDensity) _paddingCompact else _padding

        if (_parent != null) _depth = _parent!!.depth + 1

        if (_itemType == DiagramItemType.group) {
            _size = KSize(40, 20)
        }

        if (_name.isNotEmpty()) {
            val text = if (diagram.showType && _type.isNotEmpty()) "$_name:$_type" else _name
            val sizeF = g.measureString(text, font)
            // MONOFIX: en el original Size = sizeF.ToSize() se sustituyó por el truncado manual.
            _size = KSize(sizeF.width.toInt(), sizeF.height.toInt())
            _size = _size + KSize(
                2 * _margin.width + (if (_hasChildElements) _childExpandButtonSize else 0),
                2 * _margin.height
            )
        }

        var childBoundingBoxHeight = 0
        var childBoundingBoxWidth = 0
        if (_showChildElements) {
            // Measure the children
            for (element in _childElements) {
                element.generateMeasure(g)
                childBoundingBoxWidth = maxOf(childBoundingBoxWidth, element.boundingBox.size.width)
                childBoundingBoxHeight += element.boundingBox.size.height
            }
        }

        _boundingBox.width = _size.width + 2 * padding.width + childBoundingBoxWidth
        _boundingBox.height = maxOf(_size.height + 2 * padding.height, childBoundingBoxHeight)

        if (diagram.showDocumentation) {
            val text = getTextDocumentation()
            if (text != null) {
                if (_size.width < _documentationMinWidth) {
                    val widthOffset = _documentationMinWidth - _size.width
                    _size.width += widthOffset
                    _boundingBox.width += widthOffset
                }

                val sizeF = g.measureString(text, documentationFont)
                val documentationWidth = Math.max(1.0, _size.width.toDouble() + padding.width)
                val documentationHeight =
                    (Math.ceil(sizeF.width / documentationWidth) + 1.8) * sizeF.height
                _documentationBox = KRectangle(
                    KPoint(0, 0),
                    KSize(
                        documentationWidth.toInt(),
                        documentationHeight.toInt()
                    )
                )
                _boundingBox.height = maxOf(
                    _size.height + 2 * padding.height + _documentationBox.height + 2 * padding.height,
                    childBoundingBoxHeight
                )
            }
        }

        _elementBox = KRectangle(
            KPoint(0, 0),
            _size - KSize(if (_hasChildElements) _childExpandButtonSize / 2 else 0, 0)
        )

        if (_hasChildElements) {
            _childExpandButtonBox.x = _elementBox.width - _childExpandButtonSize / 2
            _childExpandButtonBox.y = (_elementBox.height - _childExpandButtonSize) / 2
            _childExpandButtonBox.width = _childExpandButtonSize
            _childExpandButtonBox.height = _childExpandButtonSize
        }
    }

    /** Equivale a GenerateLocation(). */
    fun generateLocation() {
        val padding = if (diagram.compactLayoutDensity) _paddingCompact else _padding
        _location.x = _boundingBox.x + padding.width

        when (diagram.alignement) {
            DiagramAlignement.Center -> {
                _location.y = _boundingBox.y + (_boundingBox.height - _size.height) / 2
                if (diagram.showDocumentation && !isEmptyRectangle(_documentationBox)) {
                    _location.y = _boundingBox.y +
                        (_boundingBox.height - (2 * padding.height + _documentationBox.height)) / 2
                }
            }
            DiagramAlignement.Near -> {
                if (_itemType == DiagramItemType.group &&
                    _parent != null && _parent!!.childElements.size == 1
                ) {
                    _location.y = _parent!!.location.y +
                        (_parent!!._elementBox.height - _elementBox.height) / 2
                } else {
                    _location.y = _boundingBox.y + padding.height
                }
            }
            DiagramAlignement.Far -> {
                if (_itemType == DiagramItemType.group &&
                    _parent != null && _parent!!.childElements.size == 1
                ) {
                    _location.y = _parent!!.location.y +
                        (_parent!!._elementBox.height - _elementBox.height) / 2
                } else {
                    _location.y = _boundingBox.y + _boundingBox.height - _size.height - padding.height
                }
            }
        }

        if (_showChildElements) {
            var childrenHeight = 0
            for (element in _childElements) childrenHeight += element.boundingBox.height

            val childrenX = _boundingBox.x + 2 * padding.width + size.width
            var childrenY = _boundingBox.y + Math.max(0, (_boundingBox.height - childrenHeight) / 2)

            for (element in _childElements) {
                val elementBoundingBox = element.boundingBox.copy()
                elementBoundingBox.x = childrenX
                elementBoundingBox.y = childrenY
                element.boundingBox = elementBoundingBox
                element.generateLocation()

                childrenY += element.boundingBox.height
            }
        }

        if (_hasChildElements) _childExpandButtonBox.offset(_location)

        _elementBox.offset(_location)

        if (!isEmptyRectangle(_documentationBox)) {
            if (diagram.alignement == DiagramAlignement.Far) {
                _documentationBox.offset(_location.x, _location.y - _documentationBox.height - padding.height)
            } else {
                _documentationBox.offset(_location.x, _location.y + _elementBox.height + padding.height)
            }
        }
    }

    // ------------------------------------------------------------
    // Hit-test recursivo
    // ------------------------------------------------------------

    /** Equivale a HitTest(Point point, out DiagramItem, out DiagramHitTestRegion). */
    fun hitTest(point: KPoint, result: HitTestResult) {
        if (scaleRectangle(_boundingBox).contains(point)) {
            result.element = this
            if (scaleRectangle(KRectangle(_location, _size)).contains(point)) {
                if (_hasChildElements && scaleRectangle(_childExpandButtonBox).contains(point)) {
                    result.region = DiagramHitTestRegion.ChildExpandButton
                } else {
                    result.region = DiagramHitTestRegion.Element
                }
            } else {
                result.region = DiagramHitTestRegion.BoundingBox
                if (_showChildElements) {
                    for (childElement in _childElements) {
                        childElement.hitTest(point, result)
                        if (result.region != DiagramHitTestRegion.None) break
                    }
                }
            }
        } else {
            result.region = DiagramHitTestRegion.None
        }
    }

    /** Equivale a Rectangle.IsEmpty (Width == 0 && Height == 0). */
    private fun isEmptyRectangle(r: KRectangle): Boolean = r.width == 0 && r.height == 0
}
