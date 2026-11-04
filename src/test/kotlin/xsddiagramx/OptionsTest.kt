//    XSD Diagram - Kotlin/JavaFX port
//    Test del port de Options.cs (parseo de la línea de comandos).
package xsddiagramx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OptionsTest {

    @Test
    fun parseCommandLine() {
// Ojo: Options es un singleton (el C# era de estático único). Este test
// corre en una JVM propia de la suite (ningún otro test llama a init).
        Options.init(
            listOf(
                "-e", "3", "-d", "-z", "150", "-f", "PATH, NAME", "-a",
                "--o:out.png", "--no-gui", "input.xsd"
            )
        )

        assertEquals(3, Options.expandLevel)
        assertTrue(Options.showDocumentation)
        assertEquals(150f, Options.zoom)
        assertEquals(listOf("PATH", "NAME"), Options.textOutputFields)
        assertTrue(Options.displayAttributes)
        assertTrue(Options.noGUI)
        assertEquals("out.png", Options.outputFile)
        assertEquals("input.xsd", Options.inputFile)
    }
}
