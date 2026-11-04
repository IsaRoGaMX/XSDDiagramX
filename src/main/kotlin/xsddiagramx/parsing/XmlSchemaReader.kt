//    XSD Diagram - Kotlin/JavaFX port
//
//    Sustituto del XmlSerializer de Schema.cs: en el C# el fichero .xsd se
//    deserializaba directamente a las clases generadas por xsd.exe. Aquí se
//    parsea el documento con el DOM del JDK (javax.xml.parsers, sin librerías
//    externas) y se mapea manualmente sobre el modelo de XmlSchema.kt.
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
package xsddiagramx.parsing

import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Parser del DOM del JDK (javax.xml.parsers, sin librerías externas) que lee un
 * fichero .xsd y rellena el modelo de datos espejo de XML Schema (≈ la
 * deserialización que en el C# hacía XmlSerializer con las clases de xsd.exe).
 * Solo soporta el subconjunto de elementos que el diagrama usa realmente.
 */
class XmlSchemaReader {

    private val factory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        isIgnoringComments = true
        isCoalescing = true
        // El original eliminaba el <!DOCTYPE ...> de los XSD descargados a mano;
        // aquí evitamos cargar DTDs externos para no salir a red durante el parseo.
        try {
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setFeature("http://xml.org/sax/features/validation", false)
        } catch (_: Exception) {
            // Ignorado: no todas las implementaciones soportan estas features.
        }
    }

    /** Parsea el documento XML (stream) y devuelve el modelo; valida la raíz. */
    fun read(inputStream: InputStream): schema {
        val document = try {
            factory.newDocumentBuilder().parse(inputStream)
        } catch (e: Exception) {
            throw XmlSchemaReadException("Cannot parse the XML schema: ${e.message}", e)
        }
        val root = document.documentElement
            ?: throw XmlSchemaReadException("The document is empty")
        if (root.localName != "schema") {
            throw XmlSchemaReadException(
                "The document element is not 'schema' (it is '${root.localName ?: root.nodeName}')"
            )
        }
        return parseSchema(root)
    }

    /** Lee un fichero .xsd del disco. */
    fun read(file: File): schema = file.inputStream().use { read(it) }

    /** Lee un .xsd a partir de su texto completo. */
    fun read(text: String): schema =
        read(ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)))

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Hijos elemento del nodo (ignora texto y comentarios). */
    private fun children(el: Element): List<Element> {
        val result = ArrayList<Element>()
        var node = el.firstChild
        while (node != null) {
            if (node is Element) result.add(node)
            node = node.nextSibling
        }
        return result
    }

    /** getAttribute devuelve "" cuando no existe; aquí se convierte a null. */
    private fun attr(el: Element, name: String): String? {
        val value = el.getAttribute(name)
        return if (value.isEmpty()) null else value
    }

    /** Lee un valor booleano xsd (true/1) con su valor por defecto. */
    private fun attrBool(el: Element, name: String, default: Boolean = false): Boolean {
        val value = attr(el, name) ?: return default
        return value == "true" || value == "1"
    }

    /**
     * Resuelve el QName de un valor de atributo a los namespace reales del documento
     * (equivale a como XmlSerializer inflaba System.Xml.XmlQualifiedName).
     */
    private fun qn(container: Element, raw: String?): QualifiedName? {
        if (raw == null || raw.isEmpty()) return null
        val idx = raw.indexOf(':')
        return if (idx < 0) {
            // Atributo sin prefijo: sin namespace (cadena vacía), igual que en C#.
            QualifiedName("", raw)
        } else {
            val prefix = raw.substring(0, idx)
            val local = raw.substring(idx + 1)
            val ns = container.lookupNamespaceURI(prefix) ?: ""
            QualifiedName(ns, local)
        }
    }

    /** par minOccurs/maxOccurs del elemento con sus valores por defecto. */
    private fun minMaxOccurs(el: Element): Pair<String, String> =
        Pair(attr(el, "minOccurs") ?: "1", attr(el, "maxOccurs") ?: "1")

    // ------------------------------------------------------------------
    // xs:schema
    // ------------------------------------------------------------------

    /** Parsea xs:schema: atributos raíz y reparto document order a los handlers. */
    private fun parseSchema(el: Element): schema {
        val s = schema()
        s.targetNamespace = attr(el, "targetNamespace")
        s.version = attr(el, "version")
        s.finalDefault = attr(el, "finalDefault") ?: ""
        s.blockDefault = attr(el, "blockDefault") ?: ""

        for (child in children(el)) when (child.localName) {
            "annotation" -> s.items.add(parseAnnotation(child))
            "include" -> s.items.add(
                include().apply { schemaLocation = attr(child, "schemaLocation") }
            )
            "import" -> s.items.add(
                schemaImport().apply {
                    schemaLocation = attr(child, "schemaLocation")
                    namespace = attr(child, "namespace")
                }
            )
            "redefine" -> s.items.add(redefine()) // TODO original: redefine no soportado
            "element" -> s.items1.add(parseElement(child))
            "complexType" -> s.items1.add(parseComplexType(child))
            "simpleType" -> s.items1.add(parseSimpleType(child))
            "group" -> s.items1.add(parseGroup(child))
            "attribute" -> s.items1.add(parseAttribute(child))
            "attributeGroup" -> s.items1.add(parseAttributeGroup(child))
            // "notation" y cualquier otra cosa: no usadas por la lógica de negocio.
        }
        return s
    }

    /** Parsea xs:annotation > xs:documentation (xml:lang vía getAttributeNS). */
    private fun parseAnnotation(el: Element): annotation {
        val a = annotation()
        for (child in children(el)) when (child.localName) {
            "documentation" -> a.items.add(
                documentation().apply {
                    // xml:lang es un atributo reservado del namespace XML (no vale getAttribute).
                    val xmlLang = child.getAttributeNS("http://www.w3.org/XML/1998/namespace", "lang")
                    text = child.textContent.trim()
                    lang = if (xmlLang.isEmpty()) null else xmlLang
                    source = attr(child, "source")
                }
            )
            "appinfo" -> {} // No usado por la lógica del C#.
        }
        return a
    }

    // ------------------------------------------------------------------
    // Tipos
    // ------------------------------------------------------------------

    /** Parsea xs:complexType: content model (items/itemsElementName en document order). */
    private fun parseComplexType(el: Element): complexType {
        val c = complexType()
        c.name = attr(el, "name")
        c.mixed = attrBool(el, "mixed")
        parseAnnotatedAnnotation(el)?.let { c.annotation = it }

        for (child in children(el)) when (child.localName) {
            "sequence", "choice", "all", "group" -> {
                c.items.add(ComplexGroup(parseGroupCompositor(child)))
                c.itemsElementName.add(ItemsChoiceType4.valueOf(child.localName))
            }
            "complexContent" -> {
                c.items.add(ComplexComplexContent(parseComplexContent(child)))
                c.itemsElementName.add(ItemsChoiceType4.complexContent)
            }
            "simpleContent" -> {
                c.items.add(ComplexSimpleContent(parseSimpleContent(child)))
                c.itemsElementName.add(ItemsChoiceType4.simpleContent)
            }
            "attribute" -> {
                c.items.add(ComplexAttribute(parseAttribute(child)))
                c.itemsElementName.add(ItemsChoiceType4.attribute)
            }
            "attributeGroup" -> {
                c.items.add(ComplexAttributeGroup(parseAttributeGroup(child)))
                c.itemsElementName.add(ItemsChoiceType4.attributeGroup)
            }
            "anyAttribute" -> {
                c.items.add(ComplexAnyAttribute(parseWildcardAttribute(child)))
                c.itemsElementName.add(ItemsChoiceType4.anyAttribute)
            }
            "annotation" -> {} // ya consumido arriba
        }
        return c
    }

    /** Parsea xs:simpleType (restriction + facetas, list, union). */
    private fun parseSimpleType(el: Element): simpleType {
        val st = simpleType()
        st.name = attr(el, "name")
        parseAnnotatedAnnotation(el)?.let { st.annotation = it }

        for (child in children(el)) when (child.localName) {
            "restriction" -> {
                val base = qn(el, attr(child, "base"))
                val restriction = SimpleRestriction(base)
                // Facetas (enumeration, pattern, length, minInclusive...): kind + value.
                for (facetElement in children(child)) {
                    if (facetElement.localName == "annotation") continue
                    restriction.facets.add(
                        facet().apply {
                            kind = facetElement.localName
                            value = attr(facetElement, "value")
                        }
                    )
                }
                st.item = restriction
            }
            "list" -> st.item = SimpleList(qn(el, attr(child, "itemType")))
            "union" -> {
                val members = attr(child, "memberTypes")
                    ?.split(Regex("\\s+"))
                    ?.filter { it.isNotEmpty() }
                    ?.map { qn(el, it) }
                    ?: emptyList()
                st.item = SimpleUnion(members)
            }
            "annotation" -> {} // ya consumido arriba
        }
        return st
    }

    /** Parsea xs:group / compositores en un [group] común (name/ref/occurs/partículas). */
    private fun parseGroup(el: Element): group {
        val g = group()
        g.name = attr(el, "name")
        g.ref = qn(el, attr(el, "ref"))
        val (min, max) = minMaxOccurs(el)
        g.minOccurs = min
        g.maxOccurs = max
        parseAnnotatedAnnotation(el)?.let { g.annotation = it }

        for (child in children(el)) when (child.localName) {
            "element" -> {
                g.items.add(GroupElement(parseElement(child)))
                g.itemsElementName.add(ItemsChoiceType2.element)
            }
            "any" -> {
                g.items.add(GroupAny(parseAny(child)))
                g.itemsElementName.add(ItemsChoiceType2.any)
            }
            "sequence", "choice", "all", "group" -> {
                g.items.add(GroupCompositor(parseGroupCompositor(child)))
                g.itemsElementName.add(ItemsChoiceType2.valueOf(child.localName))
            }
            "annotation" -> {} // ya consumido arriba
        }
        return g
    }

    // ------------------------------------------------------------------
    // Contenido (complexContent / simpleContent)
    // ------------------------------------------------------------------

    /** Parsea xs:complexContent (extension/restriction). */
    private fun parseComplexContent(el: Element): complexContent {
        val cc = complexContent()
        for (child in children(el)) when (child.localName) {
            "extension" -> cc.item = parseExtension(child)
            "restriction" -> cc.item = parseComplexRestriction(child)
            "annotation" -> parseAnnotatedAnnotation(child)?.let { cc.annotation = it }
        }
        return cc
    }

    /** Parsea xs:simpleContent (extension/restriction simple). */
    private fun parseSimpleContent(el: Element): simpleContent {
        val sc = simpleContent()
        for (child in children(el)) when (child.localName) {
            "extension" -> sc.item = parseExtension(child)
            "restriction" -> {
                // Cambio: en simple se usa la misma clase de restriction (base + atributos).
                val restriction = restrictionType(qn(el, attr(child, "base")))
                parseAnnotatedAnnotation(child)?.let { restriction.annotation = it }
                sc.item = restriction
            }
            "annotation" -> parseAnnotatedAnnotation(child)?.let { sc.annotation = it }
        }
        return sc
    }

    /** Parsea xs:extension (compositores + atributos en document order). */
    private fun parseExtension(el: Element): extensionType {
        val ext = extensionType(qn(el, attr(el, "base")))

        for (child in children(el)) when (child.localName) {
            "sequence" -> ext.sequence = parseGroupCompositor(child)
            "choice" -> ext.choice = parseGroupCompositor(child)
            "all" -> ext.all = parseGroupCompositor(child)
            "group" -> ext.group = parseGroupCompositor(child)
            "attribute" -> ext.items.add(AttrAttribute(parseAttribute(child)))
            "attributeGroup" -> ext.items.add(AttrAttributeGroup(parseAttributeGroup(child)))
            "anyAttribute" -> ext.items.add(AttrAnyAttribute(parseWildcardAttribute(child)))
            "annotation" -> parseAnnotatedAnnotation(child)?.let { ext.annotation = it }
        }
        return ext
    }

    /** Parsea xs:restriction complejo (partículas + atributos). */
    private fun parseComplexRestriction(el: Element): restrictionType {
        val restriction = restrictionType(qn(el, attr(el, "base")))

        for (child in children(el)) when (child.localName) {
            "sequence", "choice", "all", "group" -> {
                restriction.items.add(ComplexGroup(parseGroupCompositor(child)))
                restriction.itemsElementName.add(ItemsChoiceType4.valueOf(child.localName))
            }
            "attribute" -> restriction.items1.add(AttrAttribute(parseAttribute(child)))
            "attributeGroup" -> restriction.items1.add(AttrAttributeGroup(parseAttributeGroup(child)))
            "anyAttribute" -> restriction.items1.add(AttrAnyAttribute(parseWildcardAttribute(child)))
            "annotation" -> parseAnnotatedAnnotation(child)?.let { restriction.annotation = it }
        }
        return restriction
    }

    // Nota: los compositores anidados (all/choice/sequence) y las referencias
    // (xs:group) comparten la clase [group], igual que en el C# (explicitGroup : group).
    /** Parsea un compositor (all/choice/sequence): delega en parseGroup. */
    private fun parseGroupCompositor(el: Element): group = parseGroup(el)

    // ------------------------------------------------------------------
    // Partículas
    // ------------------------------------------------------------------

    /** Parsea xs:element (top-level y local: name/type/ref/occurs/inline types). */
    private fun parseElement(el: Element): element {
        val e = element()
        e.name = attr(el, "name")
        e.type = qn(el, attr(el, "type"))
        e.ref = qn(el, attr(el, "ref"))
        e.substitutionGroup = qn(el, attr(el, "substitutionGroup"))
        e.isAbstract = attrBool(el, "abstract")
        e.nillable = attrBool(el, "nillable")
        val (min, max) = minMaxOccurs(el)
        e.minOccurs = min
        e.maxOccurs = max

        for (child in children(el)) when (child.localName) {
            "complexType" -> e.complexType = parseComplexType(child)
            "simpleType" -> e.simpleType = parseSimpleType(child)
            "annotation" -> e.annotation = parseAnnotation(child)
            // key / keyref / unique: no usados por la lógica de negocio.
        }
        return e
    }

    /** Parsea xs:any (wildcard de partícula con occurs). */
    private fun parseAny(el: Element): any {
        val a = any()
        a.namespace = attr(el, "namespace")
        val (min, max) = minMaxOccurs(el)
        a.minOccurs = min
        a.maxOccurs = max
        parseAnnotatedAnnotation(el)?.let { a.annotation = it }
        return a
    }

    /** Parsea xs:anyAttribute (wildcard de atributos). */
    private fun parseWildcardAttribute(el: Element) = wildcard().apply {
        namespace = attr(el, "namespace")
    }

    // ------------------------------------------------------------------
    // Atributos
    // ------------------------------------------------------------------

    /** Parsea xs:attribute (name/type/ref/default/fixed/use + inline simpleType). */
    private fun parseAttribute(el: Element): attribute {
        val a = attribute()
        a.name = attr(el, "name")
        a.type = qn(el, attr(el, "type"))
        a.ref = qn(el, attr(el, "ref"))
        a.defaultValue = attr(el, "default")
        a.fixed = attr(el, "fixed")
        // El C# exponía un enum attributeUse cuyo ToString() daba el valor tal cual;
        // el valor por defecto del estándar es "optional".
        a.use = attr(el, "use") ?: "optional"
        for (child in children(el)) when (child.localName) {
            "simpleType" -> a.simpleType = parseSimpleType(child)
            "annotation" -> a.annotation = parseAnnotation(child)
        }
        return a
    }

    /** Parsea xs:attributeGroup (name/ref + atributos en document order). */
    private fun parseAttributeGroup(el: Element): attributeGroup {
        val ag = attributeGroup()
        ag.name = attr(el, "name")
        ag.ref = qn(el, attr(el, "ref"))
        parseAnnotatedAnnotation(el)?.let { ag.annotation = it }

        for (child in children(el)) when (child.localName) {
            "attribute" -> ag.items.add(AttrAttribute(parseAttribute(child)))
            "attributeGroup" -> ag.items.add(AttrAttributeGroup(parseAttributeGroup(child)))
            "anyAttribute" -> ag.items.add(AttrAnyAttribute(parseWildcardAttribute(child)))
            "annotation" -> {} // ya consumido arriba
        }
        return ag
    }

    /** Annotation (propiedad común de Annotated) o null si el nodo no la trae. */
    private fun parseAnnotatedAnnotation(el: Element): annotation? {
        val annotationElement = children(el).firstOrNull { it.localName == "annotation" }
        return annotationElement?.let { parseAnnotation(it) }
    }
}
