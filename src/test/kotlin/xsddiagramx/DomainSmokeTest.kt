//    XSD Diagram - Kotlin/JavaFX port
//    Tests de humo del port de la capa de dominio (parsing + rendering).
//
//    Equivalen a los esquemas de prueba de la carpeta Tests\ del original
//    (COLLADASchema, issues\1, issues\2, issues\15).
package xsddiagramx

import xsddiagramx.parsing.Schema
import xsddiagramx.parsing.XSDAttribute
import xsddiagramx.rendering.Diagram
import xsddiagramx.rendering.DrawingContext
import xsddiagramx.rendering.FontDescription
import xsddiagramx.rendering.SizeF
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DomainSmokeTest {

    // ------------------------------------------------------------------
    // Esquema de prueba: element con type + extensión, grupo de substitución,
    // atributo con ref, documentación e include entre ficheros.
    // ------------------------------------------------------------------

    private val mainXsd = """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                   xmlns:t="urn:test"
                   targetNamespace="urn:test">
          <xs:include schemaLocation="child.xsd"/>

          <xs:element name="Car" type="t:Vehicle">
            <xs:annotation>
              <xs:documentation xml:lang="en">A car.</xs:documentation>
            </xs:annotation>
          </xs:element>

          <xs:element name="RoadVehicle" abstract="true">
            <xs:complexType>
              <xs:sequence>
                <xs:element name="wheels" type="xs:positiveInteger"/>
              </xs:sequence>
            </xs:complexType>
          </xs:element>

          <xs:element name="Bike" type="t:BikeModel" substitutionGroup="t:RoadVehicle"/>

          <xs:element name="WithRef">
            <xs:complexType>
              <xs:attribute ref="t:globalAtt" use="required"/>
            </xs:complexType>
          </xs:element>

          <xs:complexType name="Vehicle">
            <xs:complexContent>
              <xs:extension base="t:Part">
                <xs:sequence>
                  <xs:element name="engine" nillable="true" maxOccurs="2" type="xs:string"/>
                </xs:sequence>
                <xs:attribute name="brand" type="xs:string" use="required"/>
              </xs:extension>
            </xs:complexContent>
          </xs:complexType>

          <xs:complexType name="Part">
            <xs:sequence>
              <xs:element name="id" type="xs:string"/>
            </xs:sequence>
          </xs:complexType>

          <xs:complexType name="BikeModel">
            <xs:sequence>
              <xs:element name="bell" type="xs:string" minOccurs="0"/>
            </xs:sequence>
          </xs:complexType>

          <xs:attribute name="globalAtt" type="xs:int" use="optional"/>
        </xs:schema>
    """.trimIndent()

    private val childXsd = """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                   xmlns:t="urn:test"
                   targetNamespace="urn:test">
          <xs:element name="GlobalElement" type="xs:string"/>
        </xs:schema>
    """.trimIndent()

    private fun loadMain(tempDir: Path): Schema {
        tempDir.resolve("main.xsd").writeText(mainXsd)
        tempDir.resolve("child.xsd").writeText(childXsd)

        val schema = Schema()
        schema.loadSchema(tempDir.resolve("main.xsd").toString())
        return schema
    }

    // ------------------------------------------------------------------
    // 1) Carga e índexación
    // ------------------------------------------------------------------

    @Test
    fun `index and include recursion work`() {
        val temp = createTempDir()
        val schema = loadMain(temp)

        assertEquals(emptyList<String>(), schema.loadErrors)
        assertTrue(schema.isLoaded())
        assertEquals(2, schema.xsdFilenames.size, "el include debe añadir child.xsd a XsdFilenames")

        val byName = schema.elementsByName

        assertNotNull(byName["urn:test:element:Car"])
        assertNotNull(byName["urn:test:element:RoadVehicle"])
        assertNotNull(byName["urn:test:element:Bike"])
        assertNotNull(byName["urn:test:element:GlobalElement"], "el include debe indexar child.xsd")
        assertNotNull(byName["urn:test:type:Vehicle"])
        assertNotNull(byName["urn:test:type:Part"])
        assertNotNull(byName["urn:test:element:WithRef"])

        // XSDObject.FullName usa ns:type:name para los tipos y ns:element:name para los elementos.
        assertEquals("complexType: Vehicle (urn:test)", byName["urn:test:type:Vehicle"].toString())

        val globalAtt = schema.attributesByName["urn:test:attribute:globalAtt"]
        assertNotNull(globalAtt)
        assertTrue(globalAtt is XSDAttribute && globalAtt.use == "optional")
    }

    // ------------------------------------------------------------------
    // 2) Expansión perezosa + layout
    // ------------------------------------------------------------------

    @Test
    fun `expand resolves extension base, substitution groups and lays out`() {
        val temp = createTempDir()
        val schema = loadMain(temp)

        val diagram = Diagram()
        diagram.elementsByName = schema.elementsByName as MutableMap<String, xsddiagramx.parsing.XSDObject?>

        // a) Substitute: al añadir el elemento abstracto, el Bike entra como "heredero".
        val car = diagram.add(schema.elementsByName["urn:test:element:Car"]!!.tag, "urn:test")
        val roadVehicle = diagram.add(schema.elementsByName["urn:test:element:RoadVehicle"]!!.tag, "urn:test")
        assertNotNull(car)
        assertNotNull(roadVehicle)

        // roots: Car, Bike, RoadVehicle (Bike añadido por substitución de RoadVehicle).
        val bike = diagram.rootElements.first { it.name == "Bike" }
        assertEquals(roadVehicle, bike.inheritFrom)

        // b) Expansión: base (Part/id) + extensión (engine).
        assertTrue(diagram.expandOneLevel())

        val carItem = diagram.rootElements[0]
        assertEquals("Car", carItem.name)
        assertTrue(carItem.showChildElements)

        // Car children: [compositor sequence (hereda de Part/id)] + [compositor sequence (engine)]
        assertEquals(2, carItem.childElements.size)
        assertEquals(xsddiagramx.rendering.DiagramItemGroupType.Sequence, carItem.childElements[0].groupType)
        assertEquals(listOf("id"), carItem.childElements[0].childElements.map { it.name })
        assertEquals(listOf("engine"), carItem.childElements[1].childElements.map { it.name })

        // Las ocurrencias del element con maxOccurs="2" se resuelven.
        assertEquals(2, carItem.childElements[1].childElements[0].maxOccurrence)

        // Wheels bajo el elemento abstracto RoadVehicle.
        assertTrue(diagram.expandChildren(roadVehicle))
        val roadVehicleChildren = roadVehicle.childElements[0].childElements.map { it.name }
        // El compositor sequence del inline complexType + el substituto Bike.
        assertTrue(roadVehicleChildren.contains("wheels"))

        // c) Layout con un contexto de medición falso (6px/char, 12px de alto).
        val fakeContext = object : DrawingContext {
            override fun measureString(text: String, font: FontDescription): SizeF =
                SizeF((text.length * 6).toDouble(), 12.0)
        }
        diagram.layout(fakeContext)

        val bbox = diagram.boundingBox
        assertTrue(bbox.width > 100, "el layout debe crecer más del placeholder de 100: $bbox")
        assertTrue(bbox.height >= carItem.boundingBox.height)

        // d) Hit-test y búsqueda.
        val hit = diagram.hitTest(xsddiagramx.rendering.KPoint(carItem.elementBox.x + 2, carItem.elementBox.y + 2))
        assertEquals(carItem, hit.element)
        assertEquals(xsddiagramx.rendering.DiagramHitTestRegion.Element, hit.region)

        val hitEngine = diagram.search("engine")
        assertNotNull(hitEngine)
        assertEquals("engine", hitEngine.name)
        assertEquals(1, diagram.searchHits)
    }

    // ------------------------------------------------------------------
    // 3) Atributos con herencia + documentación
    // ------------------------------------------------------------------

    @Test
    fun `attributes resolution with inheritance and documentation text`() {
        val temp = createTempDir()
        val schema = loadMain(temp)

        // Documentación: texto + tag xml:lang
        val carElement = schema.elementsByName["urn:test:element:Car"]!!.tag
            as xsddiagramx.parsing.element
        val annotation = carElement.annotation
        assertNotNull(annotation)
        assertEquals("A car. (en)", DiagramHelpers.getAnnotationText(annotation))

        // Atributos de Car (complexType Vehicle: extensión de Part + brand)
        val attributes = DiagramHelpers.getAnnotatedAttributes(schema, carElement, "urn:test")
        assertEquals(listOf("brand"), attributes.map { it.name })
        assertEquals("required", attributes[0].use)

        // Atributo referenciado: use="required" sobrescribe el "optional" del global.
        val withRef = schema.elementsByName["urn:test:element:WithRef"]!!.tag as xsddiagramx.parsing.element
        val refAttributes = DiagramHelpers.getAnnotatedAttributes(schema, withRef, "urn:test")
        assertEquals(listOf("globalAtt"), refAttributes.map { it.name })
        assertEquals("required", refAttributes[0].use)
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Directorio temporal único (sin API deprecated). */
    private fun createTempDir(): Path =
        java.nio.file.Files.createTempDirectory("xsddiagramx-test")

    private fun writeTemp(dir: Path, name: String, content: String): String {
        val path = dir.resolve(name)
        path.writeText(content)
        return path.toString()
    }
}
