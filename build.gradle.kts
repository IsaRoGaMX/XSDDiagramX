// XSD Diagram - Kotlin/JavaFX port
// Estructura replicada del proyecto C# original (Regis Cosnier, GPL/LGPL/MS-PL)
//
// Sin plugins de terceros: JavaFX se declara como dependencias org.openjfx
// (Maven Central) y se pasa por --module-path en las tareas de ejecución.
// Todas las licencias son libres (LGPL/gpl con exception de classpath).
import org.gradle.api.tasks.JavaExec

plugins {
    kotlin("jvm") version "2.1.20"
    application
}

group = "io.xsddiagramx"
version = "0.1.0"

repositories {
    mavenCentral()
}

// ---------------------------------------------------------------------------
// JavaFX (Plugin OpenJFX no usado: dependencias directas de Maven Central)
// ---------------------------------------------------------------------------

val javafxVersion = "21.0.5"

val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch").lowercase()
val javafxPlatform = when {
    osName.contains("win") -> "win"
    osName.contains("mac") -> if (!osArch.contains("aarch64")) "mac" else "mac-aarch64"
    else -> if (!osArch.contains("aarch64")) "linux" else "linux-aarch64"
}

val javafxModules = listOf("base", "graphics", "controls", "swing")

dependencies {
    for (module in javafxModules) {
        implementation("org.openjfx:javafx-$module:$javafxVersion:$javafxPlatform")
    }
    // Sin dependencias de terceros: el parsing XML usa javax.xml (JDK),
    // el HTTP usa java.net (JDK).
    testImplementation(kotlin("test"))
}

// JavaFX exige --module-path / --add-modules al ejecutar.
tasks.withType<JavaExec>().configureEach {
    doFirst {
        val requiredJars = configurations.getByName("runtimeClasspath")
            .filter { it.name.startsWith("javafx-") && it.name.endsWith(".jar") }
            .files
        jvmArgs(
            "--module-path", requiredJars.joinToString(File.pathSeparator) { it.absolutePath },
            "--add-modules", "javafx.controls",
            // Tema oscuro: los POPUPS (combos/menús) son Scenes aparte que no
            // heredan los stylesheets de la ventana; el tema se instala como
            // "user agent stylesheet" vía StyleManager (JavaFX interno).
            "--add-exports", "javafx.graphics/com.sun.javafx.css=ALL-UNNAMED"
        )
    }
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("xsddiagramx.app.XsdDiagramAppKt")
    // Tema oscuro: estos flags van también en las corridas directas del IDE
    // (run de Gradle los agrega en JavaExec; ver tasks.withType<JavaExec>).
    applicationDefaultJvmArgs = listOf(
        "--add-exports", "javafx.graphics/com.sun.javafx.css=ALL-UNNAMED"
    )
}

tasks.withType<Test> {
    useJUnitPlatform()
}
