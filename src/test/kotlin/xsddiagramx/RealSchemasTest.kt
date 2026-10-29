//    XSD Diagram - Kotlin/JavaFX port
//    Validación contra esquemas reales de la carpeta Tests\ del proyecto C#
//    (equivalente a test.bat / test-mono.sh del original).
package xsddiagramx

import xsddiagramx.parsing.Schema
import xsddiagramx.rendering.Diagram
import xsddiagramx.rendering.DrawingContext
import xsddiagramx.rendering.FontDescription
import xsddiagramx.rendering.SizeF
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RealSchemasTest {

    private fun loadResource(name: String): File {
        val url = javaClass.getResource("/schemas/$name")
            ?: throw IllegalStateException("Recurso no encontrado: $name")
        return File(url.toURI())
    }

    private fun runDiagram(schema: Schema): Diagram {
        assertTrue(schema.isLoaded())
        assertTrue(schema.elements.isNotEmpty(), "debe indexar declaraciones globales")

        val diagram = Diagram()
        diagram.elementsByName = schema.elementsByName as MutableMap<String, xsddiagramx.parsing.XSDObject?>
        schema.firstElementOrNull()?.let { diagram.add(it.tag, it.nameSpace) }
        diagram.expandOneLevel()

        val fakeContext = object : DrawingContext {
            override fun measureString(text: String, font: FontDescription): SizeF =
                SizeF((text.length * 6).toDouble(), 12.0)
        }
        diagram.layout(fakeContext)
        assertTrue(diagram.boundingBox.width > 0 && diagram.boundingBox.height > 0)
        return diagram
    }

    @Test
    fun `loads spring-beans-3_0 (github issue 2)`() {
        val schema = Schema()
        schema.loadSchema(loadResource("spring-beans-3.0.xsd").absolutePath)
        schema.loadErrors.forEach { println("spring-beans loadError: $it") } // diagnóstico
        val diagram = runDiagram(schema)
        // Elementos raíz conocidos de spring-beans: beans, bean (top-level).
        assertTrue(diagram.search("bean") != null)
    }

    @Test
    fun `loads COLLADASchema_141`() {
        val schema = Schema()
        schema.loadSchema(loadResource("COLLADASchema_141.xsd").absolutePath)
        schema.loadErrors.forEach { println("COLLADA loadError: $it") } // diagnóstico
        val diagram = runDiagram(schema)
        assertTrue(diagram.search("COLLADA") != null)
    }

    @Test
    fun `loads domicoudin (github issue 15, case sensitivity)`() {
        val schema = Schema()
        schema.loadSchema(loadResource("domicoudin.xsd").absolutePath)
        assertEquals(emptyList(), schema.loadErrors)

        // issue 15: referencias a elementos diferenciadas sólo por mayúsculas/minúsculas.
        // Sin targetNamespace -> la clave usa namespace vacío "".
        assertEquals("a", schema.elementsByName[":element:a"]?.name)
        assertEquals("A", schema.elementsByName[":element:A"]?.name)
        assertEquals(5, schema.elements.size)

        val diagram = Diagram()
        diagram.elementsByName = schema.elementsByName as MutableMap<String, xsddiagramx.parsing.XSDObject?>
        val root = diagram.add(schema.elementsByName[":element:x"]!!.tag, "")
        assertTrue(diagram.expandOneLevel())

        // x > [compositor sequence] > [a, b] (refs resueltas por nombre exacto)
        val sequence = root!!.childElements[0]
        assertEquals(1, root.childElements.size)
        assertEquals(listOf("a", "b"), sequence.childElements.map { it.name })

        // b > A (exact case)  > c: el ref  A  resuelve al elemento  A .
        val b = sequence.childElements[1]
        assertTrue(diagram.expandChildren(b))
        assertEquals("A", b.childElements[0].childElements[0].name)
    }
}
