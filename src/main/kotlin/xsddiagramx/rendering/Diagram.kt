//    XSD Diagram - Kotlin/JavaFX port
//    Diagram.kt: motor del diagrama — colección de raíces, expansión perezosa
//    (binding con ElementsByName en el momento de expandir), layout (medida +
//    posición), hit-test, búsqueda y utilidades de escalado.
//    Origen C#: port de XSDDiagrams\Rendering\Diagram.cs.
//
//    Licencias del original: LGPL / MS-PL. Autores originales: Regis Cosnier (inicial),
//    Paul Selormey (refactorización), Edu Serna (funcionalidad de búsqueda).
package xsddiagramx.rendering

import xsddiagramx.parsing.Annotated
import xsddiagramx.parsing.ComplexComplexContent
import xsddiagramx.parsing.ComplexComplexType
import xsddiagramx.parsing.ComplexGroup
import xsddiagramx.parsing.ComplexSimpleContent
import xsddiagramx.parsing.GroupAny
import xsddiagramx.parsing.GroupCompositor
import xsddiagramx.parsing.GroupElement
import xsddiagramx.parsing.ItemsChoiceType2
import xsddiagramx.parsing.ItemsChoiceType4
import xsddiagramx.parsing.XSDObject
import xsddiagramx.parsing.any
import xsddiagramx.parsing.complexType
import xsddiagramx.parsing.element
import xsddiagramx.parsing.extensionType
import xsddiagramx.parsing.group
import xsddiagramx.parsing.restrictionType
import xsddiagramx.parsing.simpleType

/**
 * Callback del evento `RequestAnyElement` del C# (evento con `out` params):
 * dado un nodo del diagrama devuelve el elemento a añadir (o null).
 */
typealias RequestAnyElementHandler = (diagramElement: DiagramItem) -> Pair<element, String>?

/**
 * Motor del diagrama. ≈ `XSDDiagrams.Rendering.Diagram` (Diagram.cs): construye
 * el árbol visual desde el esquema, lo mide/posiciona (Layout), resuelve el
 * hit-test, la búsqueda y el escalado.
 */
class Diagram {

    // ------------------------------------------------------------
    // Private state
    // ------------------------------------------------------------

    private var _showBoundingBox = false
    private var _showDocumentation = false
    private var _alwaysShowOccurence = false
    private var _showType = false
    private var _compactLayoutDensity = false
    private var _sizes = KSize(100, 100)
    private var _padding = KSize(10, 10)
    private var _scale = 1.0f
    private var _lastScale = 1.0f

    private var _font: FontDescription? = null
    private var _fontScaled: FontDescription? = null
    private var _smallFont: FontDescription? = null
    private var _smallFontScaled: FontDescription? = null
    private var _documentationFont: FontDescription? = null
    private var _documentationFontScaled: FontDescription? = null

    private var _boundingBox = KRectangle()
    private var _alignement = DiagramAlignement.Center

    private var _elementsByName: MutableMap<String, XSDObject?> = HashMap()
    private val _rootElements = ArrayList<DiagramItem>()
    private var _selectedElement: DiagramItem? = null

    private var _lastSearchText = ""
    private var _lastSearchHitElementIndex = 0
    private val _lastSearchHitElements = ArrayList<DiagramItem>()

    private var _fakeAny: any? = null

    // ------------------------------------------------------------
    // Public properties
    // ------------------------------------------------------------

    /** Tamaño del lienzo de referencia (≈ Diagram.Size). */
    var size: KSize
        get() = _sizes
        set(value) { _sizes = value }

    /** Espaciado entre elementos raíz y alrededor del diagrama (≈ Diagram.Padding). */
    var padding: KSize
        get() = _padding
        set(value) { _padding = value }

    /** Caja que engloba todo el diagrama, calculada en layout() (≈ Diagram.BoundingBox). */
    val boundingBox: KRectangle get() = _boundingBox

    /** Factor de escalado del diagrama (≈ Diagram.Scale). */
    var scale: Float
        get() = _scale
        set(value) { _scale = value }

    /** Alineación vertical de los nodos en su caja (≈ Diagram.Alignement). */
    var alignement: DiagramAlignement
        get() = _alignement
        set(value) { _alignement = value }

    /** Dibuja el bounding box de depuración de cada nodo (≈ Diagram.ShowBoundingBox). */
    var showBoundingBox: Boolean
        get() = _showBoundingBox
        set(value) { _showBoundingBox = value }

    /** Muestra la documentación xs:annotation bajo el elemento (≈ Diagram.ShowDocumentation). */
    var showDocumentation: Boolean
        get() = _showDocumentation
        set(value) { _showDocumentation = value }

    /** Muestra siempre la cadena de ocurrencias min..max (≈ Diagram.AlwaysShowOccurence). */
    var alwaysShowOccurence: Boolean
        get() = _alwaysShowOccurence
        set(value) { _alwaysShowOccurence = value }

    /** Añade "name:type" tras el nombre del nodo (≈ Diagram.ShowType). */
    var showType: Boolean
        get() = _showType
        set(value) { _showType = value }

    /** Layout compacto: paddings reducidos y desplazamientos de ocurrencias (≈ Diagram.CompactLayoutDensity). */
    var compactLayoutDensity: Boolean
        get() = _compactLayoutDensity
        set(value) { _compactLayoutDensity = value }

    // Fuentes (resueltas en layout(); visibles para DiagramItem y renderers).

    /** Fuente base del nombre del nodo (≈ Diagram.Font). */
    val font: FontDescription? get() = _font

    /** Fuente del nombre escalada por scale, resuelta en layout() (≈ Diagram.FontScaled). */
    val fontScaled: FontDescription? get() = _fontScaled

    /** Fuente pequeña para ocurrencias (≈ Diagram.SmallFont). */
    val smallFont: FontDescription? get() = _smallFont

    /** Fuente pequeña escalada por scale (≈ Diagram.SmallFontScaled). */
    val smallFontScaled: FontDescription? get() = _smallFontScaled

    /** Fuente del bloque de documentación (≈ Diagram.DocumentationFont). */
    val documentationFont: FontDescription? get() = _documentationFont

    /** Fuente de documentación escalada por scale (≈ Diagram.DocumentationFontScaled). */
    val documentationFontScaled: FontDescription? get() = _documentationFontScaled

    /** Compartido con Schema (misma referencia que en el C#): clave del binding perezoso. */
    var elementsByName: MutableMap<String, XSDObject?>
        get() = _elementsByName
        set(value) { _elementsByName = value }

    /** Elementos raíz añadidos al diagrama (≈ Diagram.RootElements). */
    val rootElements: List<DiagramItem> get() = _rootElements

    /** Elemento seleccionado actualmente, o null (≈ Diagram.SelectedElement). */
    val selectedElement: DiagramItem? get() = _selectedElement

    /** Número de resultados de la última búsqueda (≈ Diagram.SearchHits). */
    val searchHits: Int get() = _lastSearchHitElements.size

    /** Resultado de búsqueda actual, 1-based (0 si no hay búsqueda activa) (≈ Diagram.ActualSearchHit). */
    val actualSearchHit: Int get() = _lastSearchHitElementIndex + 1

    /** Equivale al evento RequestAnyElement (usado con xs:any). */
    var requestAnyElement: RequestAnyElementHandler? = null

    // ------------------------------------------------------------
    // Add (construcción del árbol)
    // ------------------------------------------------------------

    /** Equivale a Add(XMLSchema.openAttrs, string). */
    fun add(childElement: Any?, nameSpace: String): DiagramItem? =
        when (childElement) {
            is element -> addElement(childElement, nameSpace)
            is group -> addCompositors(childElement, nameSpace)
            is complexType -> addComplexType(childElement, nameSpace)
            else -> null
        }

    /** Equivale a AddElement(XMLSchema.element, string): añade el elemento como raíz. */
    fun addElement(childElement: element, nameSpace: String): DiagramItem =
        addElement(null, childElement, nameSpace)

    /** Equivale a AddElement(DiagramItem parentDiagramElement, XMLSchema.element, string). */
    fun addElement(parentDiagramElement: DiagramItem?, elementChild: element, nameSpace: String): DiagramItem {
        clearSearch()

        var childElement = elementChild
        val childDiagramElement = DiagramItem()

        var referenceElement: element? = null
        val ref = childElement.ref
        if (ref != null) {
            childDiagramElement.isReference = true

            val objectReferred = _elementsByName["${ref.namespace}:element:${ref.name}"]
            if (objectReferred != null) {
                val elementReferred = objectReferred.tag as? element
                if (elementReferred != null) {
                    referenceElement = childElement
                    childElement = elementReferred
                }
            } else {
                // Referencia no resuelta: al menos conserva el nombre.
                childElement.name = ref.name
            }
        }

        childDiagramElement.diagram = this
        childDiagramElement.tabSchema = childElement
        childDiagramElement.name = childElement.name ?: ""
        childDiagramElement.nameSpace = nameSpace

        val type = childDiagramElement.getTypeAnnotation()
        if (!type.isNullOrEmpty()) childDiagramElement.type = type
        childDiagramElement.itemType = DiagramItemType.element

        // int.TryParse -> Int (equivale a los TryParse con catch del original)
        val minOcc = (referenceElement?.minOccurs ?: childElement.minOccurs).toIntOrNull()
        childDiagramElement.minOccurrence = minOcc ?: -1
        val maxOcc = (referenceElement?.maxOccurs ?: childElement.maxOccurs).toIntOrNull()
        childDiagramElement.maxOccurrence = maxOcc ?: -1

        val (hasChildren, isSimple) = getChildrenInfo(childElement)
        childDiagramElement.hasChildElements = hasChildren
        childDiagramElement.isSimpleContent = isSimple

        if (parentDiagramElement == null) {
            _rootElements.add(childDiagramElement)
        } else {
            childDiagramElement.parent = parentDiagramElement
            parentDiagramElement.addChild(childDiagramElement)
        }

        // Grupos de substitución: si el elemento es abstract, se añaden sus
        // sustitutos como hermanos "herederos".
        if (childElement.isAbstract) {
            val abstractElementFullName = childDiagramElement.fullName
            for (xsdObject in _elementsByName.values) {
                if (xsdObject != null && xsdObject.tag is element) {
                    val substElement = xsdObject.tag as element
                    val substitutionGroup = substElement.substitutionGroup
                    if (substitutionGroup != null) {
                        val elementFullName =
                            "${substitutionGroup.namespace}:element:${substitutionGroup.name}"
                        if (elementFullName == abstractElementFullName) {
                            val diagramBase =
                                addElement(parentDiagramElement, substElement, xsdObject.nameSpace)
                            if (diagramBase != null) diagramBase.inheritFrom = childDiagramElement
                        }
                    }
                }
            }
        }

        return childDiagramElement
    }

    /** Equivale a AddComplexType(XMLSchema.complexType, string): añade el tipo como raíz. */
    fun addComplexType(childElement: complexType, nameSpace: String): DiagramItem =
        addComplexType(null, childElement, false, nameSpace)

    /** Equivale a AddComplexType(DiagramItem, XMLSchema.complexType, string). */
    fun addComplexType(
        parentDiagramElement: DiagramItem?,
        childElement: complexType,
        nameSpace: String,
    ): DiagramItem = addComplexType(parentDiagramElement, childElement, false, nameSpace)

    /** Equivale a AddComplexType(..., bool isReference, string). */
    fun addComplexType(
        parentDiagramElement: DiagramItem?,
        complexTypeChild: complexType,
        isReference: Boolean,
        nameSpace: String,
    ): DiagramItem {
        clearSearch()

        val childDiagramElement = DiagramItem()
        childDiagramElement.diagram = this
        childDiagramElement.tabSchema = complexTypeChild
        childDiagramElement.name = complexTypeChild.name ?: ""
        childDiagramElement.nameSpace = nameSpace
        childDiagramElement.itemType = DiagramItemType.type
        childDiagramElement.minOccurrence = 1
        childDiagramElement.maxOccurrence = 1
        childDiagramElement.isReference = isReference
        childDiagramElement.isSimpleContent = false
        childDiagramElement.hasChildElements = false

        for (item in complexTypeChild.items) {
            if (item is ComplexGroup || item is ComplexComplexType || item is ComplexComplexContent) {
                childDiagramElement.hasChildElements = true
                break
            }
        }

        if (parentDiagramElement == null) {
            _rootElements.add(childDiagramElement)
        } else {
            childDiagramElement.parent = parentDiagramElement
            parentDiagramElement.addChild(childDiagramElement)
        }

        return childDiagramElement
    }

    /** Equivale a AddAny. El C# creaba un `any` fake (min=0, max=unbounded). */
    fun addAny(parentDiagramElement: DiagramItem?, anyChild: any?, nameSpace: String): DiagramItem? {
        var childElement = anyChild
        var isDisabled = false
        if (childElement == null) {
            isDisabled = true
            if (_fakeAny == null) {
                _fakeAny = any().apply {
                    minOccurs = "0"
                    maxOccurs = "unbounded"
                }
            }
            childElement = _fakeAny!!
        }

        val childDiagramElement = DiagramItem()
        childDiagramElement.isDisabled = isDisabled
        childDiagramElement.diagram = this
        childDiagramElement.tabSchema = childElement
        childDiagramElement.name = "any  ${childElement.namespace}"
        childDiagramElement.nameSpace = nameSpace
        childDiagramElement.itemType = DiagramItemType.group

        childDiagramElement.minOccurrence = childElement.minOccurs.toIntOrNull() ?: -1
        childDiagramElement.maxOccurrence = childElement.maxOccurs.toIntOrNull() ?: -1

        childDiagramElement.isReference = false
        childDiagramElement.isSimpleContent = false
        childDiagramElement.hasChildElements = false

        if (parentDiagramElement == null) {
            _rootElements.add(childDiagramElement)
        } else {
            childDiagramElement.parent = parentDiagramElement
            parentDiagramElement.addChild(childDiagramElement)
        }

        return childDiagramElement
    }

    /** Equivale a AddCompositors(XMLSchema.group, string): añade el xs:group como raíz. */
    fun addCompositors(childGroup: group, nameSpace: String): DiagramItem =
        addCompositors(null, childGroup, DiagramItemGroupType.Group, nameSpace)

    /** Equivale a AddCompositors(DiagramItem, XMLSchema.group, string). */
    fun addCompositors(
        parentDiagramElement: DiagramItem?,
        childGroup: group,
        nameSpace: String,
    ): DiagramItem = addCompositors(parentDiagramElement, childGroup, DiagramItemGroupType.Group, nameSpace)

    /** Equivale a AddCompositors(..., DiagramItemGroupType type, string). */
    fun addCompositors(
        parentDiagramElement: DiagramItem?,
        groupChild: group,
        type: DiagramItemGroupType,
        nameSpace: String,
    ): DiagramItem {
        clearSearch()

        var childGroup = groupChild
        val childDiagramGroup = DiagramItem()
        childDiagramGroup.itemType = DiagramItemType.group

        var referenceGroup: group? = null
        val groupRef = childGroup.ref
        if (groupRef != null) {
            childDiagramGroup.isReference = true
            childDiagramGroup.name = groupRef.name ?: ""
            childDiagramGroup.nameSpace = groupRef.namespace ?: ""

            val grpObject = _elementsByName[childDiagramGroup.fullName]
            if (grpObject != null) {
                val referredGroup = grpObject.tag as? group
                if (referredGroup != null) {
                    referenceGroup = childGroup
                    childGroup = referredGroup
                }
            }
        } else if (type == DiagramItemGroupType.Group) {
            childDiagramGroup.name = childGroup.name ?: ""
            childDiagramGroup.nameSpace = nameSpace
        } else {
            childDiagramGroup.nameSpace = nameSpace
        }

        childDiagramGroup.diagram = this
        childDiagramGroup.tabSchema = childGroup

        val minOcc = (referenceGroup?.minOccurs ?: childGroup.minOccurs).toIntOrNull()
        childDiagramGroup.minOccurrence = minOcc ?: -1
        val maxOcc = (referenceGroup?.maxOccurs ?: childGroup.maxOccurs).toIntOrNull()
        childDiagramGroup.maxOccurrence = maxOcc ?: -1

        childDiagramGroup.hasChildElements = true
        childDiagramGroup.groupType = type

        if (parentDiagramElement == null) {
            _rootElements.add(childDiagramGroup)
        } else {
            childDiagramGroup.parent = parentDiagramElement
            parentDiagramElement.addChild(childDiagramGroup)
        }

        return childDiagramGroup
    }

    // ------------------------------------------------------------
    // Remove / clear
    // ------------------------------------------------------------

    /** Equivale a Remove(DiagramItem): quita el nodo de su padre (o de la raíz). */
    fun remove(element: DiagramItem) {
        clearSearch()
        val parent = element.parent
        if (parent == null) {
            _rootElements.remove(element)
        } else {
            parent.removeChild(element)
            if (parent.childElements.isEmpty()) parent.showChildElements = false
        }
    }

    /** Equivale a RemoveAll(): borra todos los elementos raíz. */
    fun removeAll() {
        clearSearch()
        _rootElements.clear()
    }

    // ------------------------------------------------------------
    // Expansión (perezosa, con binding del esquema)
    // ------------------------------------------------------------

    /** Equivale a ExpandOneLevel(): expande un nivel completo del árbol. */
    fun expandOneLevel(): Boolean {
        var result = false
        for (item in _rootElements) {
            result = expandOneLevel(item) || result
            if (item.hasChildElements && item.childElements.isEmpty()) {
                result = expandChildren(item) || result
            }
        }
        return result
    }

    /** Equivale a Clear(): vacía el diagrama (binding, raíces y selección). */
    fun clear() {
        _elementsByName.clear()
        _rootElements.clear()
        _selectedElement = null
        clearSearch()
    }

    // ------------------------------------------------------------
    // Búsqueda
    // ------------------------------------------------------------

    /** Equivale a ClearSearch(): borra el texto y los hits de la búsqueda activa. */
    fun clearSearch() {
        _lastSearchText = ""
        _lastSearchHitElements.clear()
    }

    /** Equivale a Search(text): devuelve el siguiente hit (cíclico). */
    fun search(text: String): DiagramItem? {
        if (text.isNullOrEmpty()) return null
        val lowered = text.lowercase()
        if (lowered == _lastSearchText) {
            _lastSearchHitElementIndex++
            if (_lastSearchHitElementIndex >= _lastSearchHitElements.size) {
                _lastSearchHitElementIndex = 0
            }
            return _lastSearchHitElements[_lastSearchHitElementIndex]
        }
        _lastSearchHitElementIndex = 0
        _lastSearchHitElements.clear()
        searchInternal(lowered, _rootElements)
        return if (_lastSearchHitElements.isEmpty()) {
            null
        } else {
            _lastSearchText = lowered
            _lastSearchHitElements[_lastSearchHitElementIndex]
        }
    }

    /** Equivale al Search(string, IList<DiagramItem>) privado: recolecta hits por nombre (sólo nodos visibles). */
    private fun searchInternal(text: String, items: List<DiagramItem>) {
        for (item in items) {
            if (item.name.lowercase().contains(text)) {
                _lastSearchHitElements.add(item)
            }
            if (item.hasChildElements && item.showChildElements) {
                searchInternal(text, item.childElements)
            }
        }
    }

    // ------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------

    /** Equivale a Layout(Graphics g): culturas de fuentes + medida + posición. */
    fun layout(g: DrawingContext) {
        val fontName = "Arial" // el original también consideró "Verdana"

        if (_font == null) {
            _font = FontDescription(fontName, 10.0f, bold = true)
            _smallFont = FontDescription(fontName, 9.0f)
            _documentationFont = FontDescription(fontName, 10.0f)
        }
        if (_fontScaled == null || _lastScale != _scale) {
            val fontScale = _scale
            _fontScaled = FontDescription(fontName, 10.0f * fontScale, bold = true)
            _smallFontScaled = FontDescription(fontName, 9.0f * fontScale)
            _documentationFontScaled = FontDescription(fontName, 10.0f * fontScale)
            _lastScale = _scale
        }

        for (element in _rootElements) element.generateMeasure(g)

        _boundingBox = KRectangle(0, 0, 100, 0)

        var currentY = _padding.height
        for (element in _rootElements) {
            val elementBoundingBox = element.boundingBox.copy()
            elementBoundingBox.x = _padding.width
            elementBoundingBox.y = currentY
            element.boundingBox = elementBoundingBox
            element.generateLocation()
            currentY += element.boundingBox.height

            _boundingBox = _boundingBox.union(element.boundingBox)
        }
    }

    // ------------------------------------------------------------
    // Hit-test / escalado
    // ------------------------------------------------------------

    /** Equivale a HitTest(Point, out DiagramItem, out DiagramHitTestRegion). */
    fun hitTest(point: KPoint): HitTestResult {
        val result = HitTestResult()
        for (childElement in _rootElements) {
            childElement.hitTest(point, result)
            if (result.region != DiagramHitTestRegion.None) break
        }
        return result
    }

    /** Equivale a ScaleInt(int): entero truncado por scale. */
    fun scaleInt(integer: Int): Int = (integer * scale).toInt()

    /** Equivale a ScalePoint(Point): punto redondeado por scale. */
    fun scalePoint(point: KPoint): KPoint =
        KPoint(Math.round(point.x * scale).toInt(), Math.round(point.y * scale).toInt())

    /** Equivale a ScaleSize(Size): tamaño redondeado por scale. */
    fun scaleSize(point: KSize): KSize =
        KSize(Math.round(point.width * scale).toInt(), Math.round(point.height * scale).toInt())

    /** Equivale a ScaleRectangle(Rectangle): caja redondeada por scale. */
    fun scaleRectangle(rectangle: KRectangle): KRectangle =
        KRectangle(
            Math.round(rectangle.x * scale).toInt(),
            Math.round(rectangle.y * scale).toInt(),
            Math.round(rectangle.width * scale).toInt(),
            Math.round(rectangle.height * scale).toInt()
        )

    // ------------------------------------------------------------
    // Expansión detallada (binding perezoso con ElementsByName)
    // ------------------------------------------------------------

    /** Equivale a ExpandChildren(DiagramItem). */
    fun expandChildren(parentDiagramElement: DiagramItem): Boolean {
        var result = false
        clearSearch()

        if (parentDiagramElement.itemType == DiagramItemType.element ||
            parentDiagramElement.itemType == DiagramItemType.type
        ) {
            result = true
            val diagramElement = parentDiagramElement
            val tabSchema = diagramElement.tabSchema

            if (tabSchema is element) {
                if (tabSchema.complexType != null) {
                    // Equivale a `element.Item is XMLSchema.complexType`
                    expandComplexType(diagramElement, tabSchema.complexType!!)
                } else if (tabSchema.type != null) {
                    val type = tabSchema.type!!
                    val objectAnnotated = _elementsByName["${type.namespace}:type:${type.name}"]
                    if (objectAnnotated != null) {
                        val annotated = objectAnnotated.tag as? Annotated
                        if (annotated != null) {
                            expandAnnotated(diagramElement, annotated, type.namespace ?: "")
                        }
                    }
                }
            } else if (tabSchema is any) {
                if (requestAnyElement != null) {
                    val requested = requestAnyElement!!(diagramElement)
                    if (requested != null) {
                        addElement(diagramElement, requested.first, requested.second)
                        diagramElement.showChildElements = true
                    }
                }
            } else if (tabSchema is complexType) {
                expandComplexType(diagramElement, tabSchema)
            }
        } else if (parentDiagramElement.itemType == DiagramItemType.group) {
            result = true
            val diagramCompositors = parentDiagramElement
            val group = diagramCompositors.tabSchema as group

            if (group.items.isNotEmpty()) {
                for ((i, item) in group.items.withIndex()) {
                    when (group.itemsElementName[i]) {
                        ItemsChoiceType2.element ->
                            if (item is GroupElement) {
                                addElement(diagramCompositors, item.value, diagramCompositors.nameSpace)
                            }
                        ItemsChoiceType2.any ->
                            if (item is GroupAny) {
                                addAny(diagramCompositors, item.value, diagramCompositors.nameSpace)
                            }
                        ItemsChoiceType2.group ->
                            if (item is GroupCompositor) {
                                addCompositors(diagramCompositors, item.value, DiagramItemGroupType.Group, diagramCompositors.nameSpace)
                            }
                        ItemsChoiceType2.all ->
                            if (item is GroupCompositor) {
                                addCompositors(diagramCompositors, item.value, DiagramItemGroupType.All, diagramCompositors.nameSpace)
                            }
                        ItemsChoiceType2.choice ->
                            if (item is GroupCompositor) {
                                addCompositors(diagramCompositors, item.value, DiagramItemGroupType.Choice, diagramCompositors.nameSpace)
                            }
                        ItemsChoiceType2.sequence ->
                            if (item is GroupCompositor) {
                                addCompositors(diagramCompositors, item.value, DiagramItemGroupType.Sequence, diagramCompositors.nameSpace)
                            }
                    }
                }
                parentDiagramElement.showChildElements = true
            } else {
                addAny(diagramCompositors, null, diagramCompositors.nameSpace)
            }
        }
        return result
    }

    // ------------------------------------------------------------
    // Selección
    // ------------------------------------------------------------

    /** Equivale a SelectElement(DiagramItem): conmuta la selección actual. */
    fun selectElement(element: DiagramItem?) {
        _selectedElement?.isSelected = false
        if (element != null) {
            _selectedElement = element
            element.isSelected = true
        }
    }

    // ------------------------------------------------------------
    // Private
    // ------------------------------------------------------------

    /** Equivale a GetChildrenInfo(XMLSchema.complexType, out, out). */
    private fun getChildrenInfo(complexTypeElement: complexType): Pair<Boolean, Boolean> {
        var hasSimpleContent = false
        for (item in complexTypeElement.items) {
            if (item is ComplexGroup || item is ComplexComplexType || item is ComplexComplexContent) {
                var hasChildren = true
                var isSimpleType = complexTypeElement.mixed
                if (item is ComplexComplexContent) {
                    val contentBody = item.value.item
                    if (contentBody is extensionType) {
                        hasChildren = false
                        if (contentBody.all != null || contentBody.group != null ||
                            contentBody.choice != null || contentBody.sequence != null
                        ) {
                            hasChildren = true
                        } else if (contentBody.base != null) {
                            val base = contentBody.base!!
                            val xsdObject = _elementsByName["${base.namespace}:type:${base.name}"]
                            if (xsdObject != null) {
                                val baseComplexType = xsdObject.tag as? complexType
                                if (baseComplexType != null) {
                                    val (hasBase, simpleBase) = getChildrenInfo(baseComplexType)
                                    hasChildren = hasBase
                                    isSimpleType = simpleBase
                                }
                            }
                        }
                    }
                    // (En el C# el caso restrictionType del contenido complejo estaba comentado.)
                }
                return hasChildren to isSimpleType
            } else if (item is ComplexSimpleContent) {
                hasSimpleContent = true
            }
        }
        return false to (hasSimpleContent || complexTypeElement.mixed)
    }

    /** Equivale a GetChildrenInfo(XMLSchema.element, out, out). */
    private fun getChildrenInfo(childElement: element): Pair<Boolean, Boolean> {
        if (childElement.complexType != null) {
            return getChildrenInfo(childElement.complexType!!)
        } else if (childElement.type != null) {
            val type = childElement.type!!
            val xsdObject = _elementsByName["${type.namespace}:type:${type.name}"]
            if (xsdObject != null) {
                val annotated = xsdObject.tag as? Annotated
                if (annotated != null) {
                    if (annotated is simpleType) {
                        return false to true
                    } else if (annotated is complexType) {
                        return getChildrenInfo(annotated)
                    }
                }
            }
        }
        return false to true
    }

    /** Equivale a ExpandAnnotated(DiagramItem, XMLSchema.annotated, string). */
    private fun expandAnnotated(parentDiagramElement: DiagramItem, annotated: Annotated, nameSpace: String) {
        if (annotated is element) {
            addElement(parentDiagramElement, annotated, nameSpace)
            parentDiagramElement.showChildElements = true
        } else if (annotated is group) {
            addCompositors(parentDiagramElement, annotated, nameSpace)
            parentDiagramElement.showChildElements = true
        } else if (annotated is complexType) {
            expandComplexType(parentDiagramElement, annotated)
            parentDiagramElement.showChildElements = true
        }
    }

    /** Equivale a ExpandComplexType(DiagramItem, XMLSchema.complexType). */
    private fun expandComplexType(parentDiagramElement: DiagramItem, complexTypeElement: complexType) {
        val items = complexTypeElement.items
        val itemsChoiceType = complexTypeElement.itemsElementName

        for ((i, item) in items.withIndex()) {
            if (item is ComplexGroup) {
                // Enum.Parse(typeof(DiagramItemGroupType), ItemsElementName[i].ToString(), true)
                val diagramCompositors = addCompositors(
                    parentDiagramElement,
                    item.value,
                    mapCompositorKind(itemsChoiceType[i]),
                    parentDiagramElement.nameSpace
                )
                parentDiagramElement.showChildElements = true
                if (diagramCompositors != null) expandChildren(diagramCompositors)
            } else if (item is ComplexComplexContent) {
                val contentBody = item.value.item
                when (contentBody) {
                    is extensionType -> {
                        val baseObject = contentBody.base
                        if (baseObject != null) {
                            val xsdObject = _elementsByName["${baseObject.namespace}:type:${baseObject.name}"]
                            if (xsdObject != null) {
                                val annotated = xsdObject.tag as? Annotated
                                if (annotated != null) {
                                    expandAnnotated(parentDiagramElement, annotated, baseObject.namespace ?: "")
                                }
                            }
                        }

                        // Mismo uso namespace del base que hacía el original.
                        val extensionNamespace = baseObject?.namespace ?: ""
                        contentBody.group?.let { group ->
                            val diagramCompositors = addCompositors(parentDiagramElement, group, DiagramItemGroupType.Group, extensionNamespace)
                            parentDiagramElement.showChildElements = true
                            if (diagramCompositors != null) expandChildren(diagramCompositors)
                        }
                        contentBody.sequence?.let { group ->
                            val diagramCompositors = addCompositors(parentDiagramElement, group, DiagramItemGroupType.Sequence, extensionNamespace)
                            parentDiagramElement.showChildElements = true
                            if (diagramCompositors != null) expandChildren(diagramCompositors)
                        }
                        contentBody.choice?.let { group ->
                            val diagramCompositors = addCompositors(parentDiagramElement, group, DiagramItemGroupType.Choice, extensionNamespace)
                            parentDiagramElement.showChildElements = true
                            if (diagramCompositors != null) expandChildren(diagramCompositors)
                        }
                        contentBody.all?.let { group ->
                            val diagramCompositors = addCompositors(parentDiagramElement, group, DiagramItemGroupType.All, extensionNamespace)
                            parentDiagramElement.showChildElements = true
                            if (diagramCompositors != null) expandChildren(diagramCompositors)
                        }
                    }
                    is restrictionType -> {
                        // Igual que en el original, con el "dgis fix github issue 2".
                        val baseObject = contentBody.base
                        var resolved = false
                        if (baseObject != null) {
                            val xsdObject = _elementsByName["${baseObject.namespace}:type:${baseObject.name}"]
                            if (xsdObject != null) {
                                val annotated = xsdObject.tag as? Annotated
                                if (annotated != null) {
                                    expandAnnotated(parentDiagramElement, annotated, baseObject.namespace ?: "")
                                    resolved = true
                                }
                            }
                        }
                        if (!resolved) {
                            // fix github issue 2: restriction con partículas propias
                            for ((j, nestedItem) in contentBody.items.withIndex()) {
                                if (nestedItem is ComplexGroup) {
                                    val diagramCompositors = addCompositors(
                                        parentDiagramElement,
                                        nestedItem.value,
                                        mapCompositorKind(contentBody.itemsElementName[j]),
                                        parentDiagramElement.nameSpace
                                    )
                                    parentDiagramElement.showChildElements = true
                                    if (diagramCompositors != null) expandChildren(diagramCompositors)
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /** Equivale a Enum.Parse(typeof(DiagramItemGroupType), ItemsElementName). */
    private fun mapCompositorKind(kind: ItemsChoiceType4): DiagramItemGroupType = when (kind) {
        ItemsChoiceType4.all -> DiagramItemGroupType.All
        ItemsChoiceType4.choice -> DiagramItemGroupType.Choice
        ItemsChoiceType4.sequence -> DiagramItemGroupType.Sequence
        else -> DiagramItemGroupType.Group
    }

    /** Equivale al ExpandOneLevel(DiagramItem) privado (recursión interna). */
    private fun expandOneLevel(parentItem: DiagramItem): Boolean {
        var result = false
        clearSearch()
        for (item in parentItem.childElements) {
            result = expandOneLevel(item) || result
            if (item.hasChildElements && item.childElements.isEmpty()) {
                result = expandChildren(item) || result
            }
        }
        return result
    }
}
