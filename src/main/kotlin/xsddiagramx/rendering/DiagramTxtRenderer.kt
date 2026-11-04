//    XSD Diagram - Kotlin/JavaFX port
//    DiagramTxtRenderer.kt: renderer que vuelca el diagrama como texto plano
//    (TSV o CSV) con columnas por campo.
//    Origen C#: port de XSDDiagrams\Rendering\DiagramTxtRenderer.cs.
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
//    Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización).
package xsddiagramx.rendering

import xsddiagramx.DiagramHelpers
import xsddiagramx.parsing.XSDAttribute
import java.io.Writer
import java.util.regex.Pattern

/**
 * ≈ DiagramTxtRenderer: exporta el diagrama como TXT (tab-separado) o CSV
 * (coma). Campos: PATH, NAME, TYPE, NAMESPACE, COMMENT, SEQ, LASTCHILD,
 * XSDTYPE (y opcionalmente los atributos con DisplayAttributes).
 */
class DiagramTxtRenderer(private val writer: Writer) : DiagramRenderer() {

    /** Modo CSV (separador coma) o TXT (separador tabulación). */
    var isCSV = false

    /** Separador de campos definitivo, fijado en render(diagram) según isCSV. */
    private var fieldSeparator = "\t"

    /** Campos solicitados por el llamador (vacío = lista completa por defecto). ≈ textOutputFields. */
    private var textOutputFields: List<String> = emptyList()

    /** Campos finales tras filtrar contra `fields` (≈ finalTextOutputFields del C#). */
    private var finalTextOutputFields: List<String> = emptyList()

    /** Si se activa, emite una fila extra por cada atributo XSD del elemento. ≈ displayAttributes. */
    private var displayAttributes = false

    /** Esquema de referencia para enumerar atributos (≈ schema). */
    private var schema: xsddiagramx.parsing.Schema? = null

    /** ≈ iteratingLastChild (también público en el C#, fijado durante el recorrido). */
    @JvmField
    var iteratingLastChild = false

    /** ≈ Name del formato: "TXT". */
    override val name: String get() = "TXT"

    // ≈ setters/getters del C# (kotlin: propiedades).
    /** Fija si se muestran los atributos de cada elemento. ≈ set DisplayAttributes. */
    fun setDisplayAttributes(value: Boolean) { displayAttributes = value }

    /** Devuelve si se muestran los atributos de cada elemento. ≈ get DisplayAttributes. */
    fun getDisplayAttributes(): Boolean = displayAttributes

    /** Fija el esquema usado para enumerar atributos. ≈ set Schema. */
    fun setSchema(value: xsddiagramx.parsing.Schema?) { schema = value }

    /** Devuelve el esquema usado para enumerar atributos. ≈ get Schema. */
    fun getSchema(): xsddiagramx.parsing.Schema? = schema

    /** Fija la lista de columnas de salida. ≈ set TextOutputFields. */
    fun setTextOutputFields(value: List<String>) { textOutputFields = value }

    /** Devuelve la lista de columnas de salida solicitada. ≈ get TextOutputFields. */
    fun getTextOutputFields(): List<String> = textOutputFields

    companion object {
        /** ≈ fields (orden de columnas por defecto). */
        private val fields = listOf(
            "PATH", "NAME", "TYPE", "NAMESPACE", "COMMENT", "SEQ", "LASTCHILD", "XSDTYPE"
        )

        /** ≈ Regex.Replace(text, " +", " "): colapsa espacios en los comentarios. */
        private val spacesRegex = Pattern.compile(" +")
    }

    /** ≈ BeginItemsRender: escribe la fila de cabecera con los campos finales. */
    override fun beginItemsRender() {
        //PATH,NAME,TYPE,NAMESPACE,COMMENT,SEQ,LASTCHILD,XSDTYPE,ATTRIBUT_NAME,ATTRIBUT_TYPE,ATTRIBUT_COMMENT
        for (i in finalTextOutputFields.indices) {
            if (i > 0) writer.write(fieldSeparator)
            writer.write(finalTextOutputFields[i])
        }
        writer.write("\r\n")
    }

    /** ≈ EndItemsRender: no-op (TXT/CSV no llevan pie). */
    override fun endItemsRender() {
    }

    /** ≈ Render(Diagram): fija separador y campos, escribe cabecera y recorre las raíces. */
    override fun render(diagram: Diagram) {
        fieldSeparator = if (isCSV) "," else "\t"

        finalTextOutputFields = if (textOutputFields.isEmpty()) fields
        else textOutputFields.filter { fields.contains(it) }

        beginItemsRender()
        var c = 0
        for (element in diagram.rootElements) {
            iteratingLastChild = (diagram.rootElements.size == ++c)
            render(element)
        }
        endItemsRender()
    }

    /**
     * ≈ Render(DiagramItem): emite la fila del ítem (ruta, nombre, tipo,
     * namespace, comentario, ocurrencia, último hijo, XSD type), opcionalmente
     * una fila por atributo, y recorre recursivamente los hijos visibles.
     */
    override fun render(drawingItem: DiagramItem) {
        val type = when (drawingItem.tabSchema) {
            is xsddiagramx.parsing.element -> "element"
            is xsddiagramx.parsing.simpleType -> "simpleType"
            is xsddiagramx.parsing.complexType -> "complexType"
            else -> ""
        }

        val occurences = "${drawingItem.minOccurrence}.." +
            (if (drawingItem.maxOccurrence == -1) "\u221E" else drawingItem.maxOccurrence.toString())

        if (type.isNotEmpty()) {
            var path = "/" + drawingItem.name
            var parentElement = drawingItem.parent
            while (parentElement != null) {
                if ((type == "element" || type == "simpleType" || type == "complexType") && parentElement.name.isNotEmpty())
                    path = "/" + parentElement.name + path
                parentElement = parentElement.parent
            }

            // Get the comment/documentation (≈ GetCommentText: items ya son
            // documentation en el port, no hace falta el `is` del C#).
            var comment = ""
            val annotated = drawingItem.tabSchema as? xsddiagramx.parsing.Annotated
            if (annotated != null && annotated.annotation != null) {
                for (o in annotated.annotation!!.items) {
                    val text0 = o.text
                    if (text0 != null) {
                        var text = text0
                        text = text.replace("\n", " ")
                        text = text.replace("\t", " ")
                        text = text.replace("\r", "")
                        text = spacesRegex.matcher(text).replaceAll(" ")
                        text = text.trim()
                        comment = text
                    } else if (o.source != null) {
                        comment = o.source!!
                    }
                    break
                }
            }

            // Output the item
            for (i in finalTextOutputFields.indices) {
                val el = drawingItem.tabSchema as? xsddiagramx.parsing.element
                var t: String
                if (el != null) {
                    val et = el.type?.toString() ?: ""
                    t = if (et.isEmpty()) "[none]"
                    else {
                        val idx = et.lastIndexOf(':')
                        val sub = et.substring(idx + 1)
                        if (sub.isEmpty()) "[none]" else sub
                    }
                } else {
                    t = "[none]"
                }
                if (i > 0) writer.write(fieldSeparator)
                val field = finalTextOutputFields[i]
                when (field) {
                    "PATH" -> writer.write(path)
                    "NAME" -> writer.write(drawingItem.name)
                    "TYPE" -> writer.write(type)
                    "NAMESPACE" -> writer.write(drawingItem.nameSpace)
                    "COMMENT" -> writer.write(comment)
                    "SEQ" -> writer.write(occurences)
                    "LASTCHILD" -> writer.write(if (iteratingLastChild) "1" else "0")
                    "XSDTYPE" -> writer.write(t)
                }
            }
            writer.write("\r\n")

            // Output the item attributes
            val theSchema = schema
            if (theSchema != null && displayAttributes) {
                val nameSpace = drawingItem.nameSpace
                if (annotated != null) {
                    // Attributes enumeration
                    val listAttributes: MutableList<XSDAttribute> = DiagramHelpers
                        .getAnnotatedAttributes(theSchema, annotated, nameSpace)
                        .toMutableList()
                    listAttributes.reverse()
                    for (a in listAttributes.indices) {
                        val xsdAttribute = listAttributes[a] ?: continue
                        var commentAttribute = ""
                        val attribute = xsdAttribute.tag
                        if (attribute != null && attribute.annotation != null)
                            commentAttribute = DiagramHelpers.getAnnotationText(attribute.annotation!!)

                        for (i in finalTextOutputFields.indices) {
                            if (i > 0) writer.write(fieldSeparator)
                            val field = finalTextOutputFields[i]
                            when (field) {
                                "PATH" -> writer.write(path + "@" + (xsdAttribute.name))
                                "NAME" -> writer.write(drawingItem.name + "@" + (xsdAttribute.name))
                                "TYPE" -> writer.write(xsdAttribute.type)
                                "NAMESPACE" -> writer.write(xsdAttribute.nameSpace)
                                "COMMENT" -> writer.write(commentAttribute)
                            }
                        }
                        writer.write("\r\n")
                    }
                }
            }
        }

        // Draw children expand box
        if (drawingItem.hasChildElements && drawingItem.showChildElements) {
            var c = 0
            for (element in drawingItem.childElements) {
                iteratingLastChild = (drawingItem.childElements.size == ++c)
                render(element)
            }
        }
    }
}
