//    XSD Diagram - Kotlin/JavaFX port
//    Port de Options.cs (parseo de la línea de comandos del C#).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
//
//    Diferencias:
//      - IsRunningOnMono es siempre false (no hay Mono en Java; el C# lo
//        detectaba por reflexión).
//      - El C# leía Environment.GetCommandLineArgs() (incluye el ejecutable en
//        [0] y lo salta); aquí se llama a [init] desde XsdDiagramApp con
//        Application.getParameters().raw (sin el ejecutable).
package xsddiagramx

/**
 * ≈ Options: opciones estáticas de la línea de comandos (GUI y modo batch
 * de Program.cs; el modo batch/consola es opcional, pero el parseo completo
 * se porta tal cual).
 */
object Options {

    /** ≈ inputFile (opcional "-i"; el C# tomaba el primer argumento sin opción). */
    var inputFile: String? = null

    /** ≈ outputFile ("-o"): fichero de salida en modo batch. */
    var outputFile: String? = null

    /** ≈ outputOnStdOut ("-os"): escribir el diagrama a salida estándar. */
    var outputOnStdOut = false

    /** ≈ outputOnStdOutExtension (extensión tras "-os"); por defecto "png". */
    var outputOnStdOutExtension = "png"

    /** ≈ rootElements ("-r"): elementos raíz a dibujar ("name@namespace"). */
    val rootElements = ArrayList<String>()

    /** ≈ expandLevel ("-e"): niveles de expansión inicial. */
    var expandLevel = 0

    /** ≈ showDocumentation ("-d"). */
    var showDocumentation = false

    /** ≈ zoom ("-z", porcentaje enteros; default 100). */
    var zoom = 100.0f

    /** ≈ forceHugeImageGeneration ("-y"). */
    var forceHugeImageGeneration = false

    /** ≈ requestHelp ("-h"). */
    var requestHelp = false

    /** Siempre false (no hay Mono en Java). */
    var isRunningOnMono = false

    /** ≈ username ("-u") de autenticación HTTP. */
    var username: String? = null

    /** ≈ password ("-p") de autenticación HTTP. */
    var password: String? = null

    /** ≈ textOutputFields ("-f", separados por coma). */
    val textOutputFields = ArrayList<String>()

    /** ≈ displayAttributes ("-a"). */
    var displayAttributes = false

    /** -no-gui del port (el C# siempre lanzaba la GUI desde Program.cs). */
    var noGUI = false

    private var initialized = false

    /** ≈ el constructor estático del C#: normaliza y parsea los argumentos. */
    fun init(commandLineArgs: List<String>) {
        if (initialized) return
        initialized = true

        val arguments = ArrayList<String>()
        // Convert "--option:params" or "/option params" to "-option params"
        for (argument in commandLineArgs) {
            var command: String? = null
            when {
                argument.startsWith("/") -> command = argument.substring(1)
                argument.startsWith("--") -> command = argument.substring(2)
                argument.startsWith("-") -> command = argument.substring(1)
            }
            if (!command.isNullOrEmpty()) {
                val indexOfColon = command.indexOf(':')
                if (indexOfColon > 0) {
                    val parameter = command.substring(indexOfColon + 1)
                    command = command.substring(0, indexOfColon)
                    arguments.add("-$command")
                    arguments.add(parameter)
                } else {
                    arguments.add("-$command")
                }
            } else {
                arguments.add(argument)
            }
        }

        var currentArgument = 0
        while (currentArgument < arguments.size) {
            val argument = arguments[currentArgument++]
            if (argument.equals("-h", ignoreCase = true)) {
                requestHelp = true
            } else if (argument.equals("-o", ignoreCase = true)) {
                if (currentArgument < arguments.size) outputFile = arguments[currentArgument++]
            } else if (argument.equals("-os", ignoreCase = true)) {
                outputOnStdOut = true
                if (currentArgument < arguments.size) outputOnStdOutExtension = arguments[currentArgument++]
            } else if (argument.equals("-r", ignoreCase = true)) {
                if (currentArgument < arguments.size) rootElements.add(arguments[currentArgument++])
            } else if (argument.equals("-e", ignoreCase = true)) {
                if (currentArgument < arguments.size) {
                    try {
                        expandLevel = arguments[currentArgument++].toInt()
                    } catch (ex: Exception) {
                    }
                }
            } else if (argument.equals("-d", ignoreCase = true)) {
                showDocumentation = true
            } else if (argument.equals("-z", ignoreCase = true)) {
                if (currentArgument < arguments.size) {
                    try {
                        zoom = arguments[currentArgument++].toInt().toFloat()
                    } catch (ex: Exception) {
                    }
                }
            } else if (argument.equals("-y", ignoreCase = true)) {
                forceHugeImageGeneration = true
            } else if (argument.equals("-u", ignoreCase = true)) {
                if (currentArgument < arguments.size) username = arguments[currentArgument++]
            } else if (argument.equals("-p", ignoreCase = true)) {
                if (currentArgument < arguments.size) password = arguments[currentArgument++]
            } else if (argument.equals("-f", ignoreCase = true)) {
                if (currentArgument < arguments.size) {
                    val fieldsArgument = arguments[currentArgument++]
                    for (field in fieldsArgument.split(",")) {
                        textOutputFields.add(field.trim())
                    }
                }
            } else if (argument.equals("-a", ignoreCase = true)) {
                displayAttributes = true
            } else if (argument.equals("-no-gui", ignoreCase = true)) {
                noGUI = true
            } else {
                inputFile = argument
            }
        }
    }

}
