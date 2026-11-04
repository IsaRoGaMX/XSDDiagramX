//    XSD Diagram - Kotlin/JavaFX port
//    SchemaLoader.kt: cargador de esquemas .xsd (fichero o URL) con sus dependencias
//    include/import, y su helper de TLS TrustAllCerts (port de la clase Schema de
//    XSDDiagrams\Parsing\Schema.cs; también conserva el alias `Schema` del C#).
//
//    Licencia: GNU GPL v2 o posterior (ver LICENSE, en la raiz del repo).
//    Autores originales: Regis Cosnier (inicial), Paul Selormey (refactorización)
package xsddiagramx.parsing

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Cargador de esquemas: lee un .xsd (fichero o URL) y todas sus dependencias
 * (xs:include / xs:import) de forma recursiva, las descarga si son remotas y
 * construye los índices ElementsByName / AttributesByName.
 *
 * Port fiel de la clase Schema de Schema.cs. Los errores no se lanzan: se
 * acumulan en [loadErrors] (igual que en el original). El evento/implementación
 * de credenciales se inyecta por constructor (en la GUI será un diálogo, en la
 * consola las opciones -u/-p).
 */
class SchemaLoader(
    /** Delegado para pedir credenciales ante 401/403 (GUI o -u/-p); null si no se quiere. */
    private val requestCredential: RequestCredentialHandler? = null,
) {

    /** Componentes globales sobre restricta ᾿element`` de la carga, en document order. */
    private val elementsList = ArrayList<XSDObject>()
    /** Índice ElementsByName: clave fullName [XSDObject.fullName] -> objeto ("" -> null siempre existe). */
    private val hashtableElementsByName = LinkedHashMap<String, XSDObject?>()
    /** Índice AttributesByName: atributos y attributeGroups globales por su clave. */
    private val hashtableAttributesByName = LinkedHashMap<String, Any?>()
    /** Primer xs:element top-level del primer esquema cargado (≈ FirstElement del C#). */
    private var firstElement: XSDObject? = null
    /** Errores acumulados de la carga (no se lanzan, igual que en el C#. */
    private val loadError = ArrayList<String>()
    /** Ficheros .xsd ya cargados (canonizados) para cortar los ciclos de include/import. */
    private val listOfXsdFilename = ArrayList<String>()

    /** Reader del DOM: port del XmlSerializer del C# (parsea el .xsd al modelo). */
    private val reader = XmlSchemaReader()

    init {
        // Igual que en el C#: la clave "" existe (a null) y TLS no valida certificados.
        hashtableElementsByName[""] = null
        hashtableAttributesByName[""] = null
        TrustAllCerts.install()
    }

    // ------------------------------------------------------------------
    // Progreso y cancelación de la carga (nuevo; el C# cargaba en el hilo
    // de la GUI y no necesitaba nada de esto)
    // ------------------------------------------------------------------

    /** Se notifica en el hilo de ejecución de la carga (no es el de la GUI). */
    interface SchemaLoadProgressListener {
        /** Justo antes de descargar [url] (puede repetirse si falla y reintenta con credenciales). */
        fun onDownloadStart(url: String) {}

        /** Tras terminar la descarga de [url] (con éxito o con error registrado). */
        fun onDownloadEnd(url: String, failed: Boolean) {}

        /** Si devuelve true se aborta la carga completa cuanto antes. */
        val cancelRequested: Boolean get() = false
    }

    /** Excepción interna para desenrollar la recursión de import al cancelar. */
    class SchemaLoadCancelledException : RuntimeException("Schema load cancelled by the user")

    /** Listener opcional de progreso/cancelación (lo asigna la GUI antes de cargar). */
    var progressListener: SchemaLoadProgressListener? = null

    /** Bandera de cancelación pedida desde fuera (volatile para verse entre hilos). */
    @Volatile
    private var cancelRequestedInternal = false

    /** Pide cancelar la carga en curso (se hace efectiva en la próxima dependencia). */
    fun requestCancel() {
        cancelRequestedInternal = true
    }

    /** Lanza [SchemaLoadCancelledException] si ya se pidió cancelar (mata la recursión). */
    private fun checkCancelled() {
        if (cancelRequestedInternal) throw SchemaLoadCancelledException()
    }

    /** Lista plana de componentes globales (elements), en document order. */
    val elements: List<XSDObject> get() = elementsList
    /** ≈ IDictionary<string, XSDObject> del C#: compartido por referencia con Diagram. */
    val elementsByName: MutableMap<String, XSDObject?> get() = hashtableElementsByName
    /** ≈ IDictionary<string, object> del C#: atributos y attributeGroups globales. */
    val attributesByName: Map<String, Any?> get() = hashtableAttributesByName
    /** Errores acumulados durante la última carga (equivale a LoadError del C#). */
    val loadErrors: List<String> get() = loadError
    /** Ficheros .xsd procesados (incluidas dependencias), en orden de carga. */
    val xsdFilenames: List<String> get() = listOfXsdFilename

    /** Primer elemento del esquema (equivale a FirstElement en C#). */
    fun firstElementOrNull(): XSDObject? = firstElement

    /** Punto de entrada: limpia el estado y carga [fileName] (path o URL http/https). */
    fun loadSchema(fileName: String) {
        cleanup()
        cancelRequestedInternal = false

        var target = fileName.trim()
        var baseUrl = ""
        if (target.startsWith("http://") || target.startsWith("https://")) {
            val basePath = System.getProperty("java.io.tmpdir")
            val loaded = loadSchemaFromUrl(basePath, target)
            if (loaded != null && loaded.first != null) {
                target = loaded.first!!
                baseUrl = loaded.second
            }
            // Si la descarga falla, en el C# fileName quedaba a null y el
            // ImportSchema(undefined) generaba el error correspondiente; aquí el
            // loadError ya lo han registrado loadSchemaFromUrl y no re-importamos.
        }
        importSchema(target, baseUrl)
    }

    /** Resetea todos los índices y estado de la carga (≈ InitSchema del C#). */
    fun cleanup() {
        firstElement = null
        elementsList.clear()
        hashtableElementsByName.clear()
        hashtableElementsByName[""] = null
        hashtableAttributesByName.clear()
        hashtableAttributesByName[""] = null
        loadError.clear()
        listOfXsdFilename.clear()
    }

    /** Indica si hay algún esquema cargado (true tras un [loadSchema] con éxito o parcial). */
    fun isLoaded(): Boolean = listOfXsdFilename.isNotEmpty()

    // ------------------------------------------------------------------
    // Import recursivo
    // ------------------------------------------------------------------

    /** Importa recursivamente [fileName]: parsea el .xsd y lista los errores (nunca lanza, salvo cancel). */
    private fun importSchema(fileName: String, baseUrl: String) {
        checkCancelled()

        val fullPath = try {
            File(fileName).canonicalPath
        } catch (_: Exception) {
            fileName
        }

        // Check if the file is already loaded to avoid the cycle
        if (listOfXsdFilename.contains(fullPath)) return

        try {
            val dom = reader.read(File(fullPath))
            listOfXsdFilename.add(fullPath)
            parseSchema(fullPath, baseUrl, dom)
        } catch (e: IOException) {
            loadError.add(e.message ?: e.toString())
        } catch (e: org.xml.sax.SAXException) {
            loadError.add("${e.message ?: e}\r\n($fullPath)")
        } catch (e: javax.xml.parsers.ParserConfigurationException) {
            loadError.add("${e.message ?: e}\r\n($fullPath)")
        } catch (e: Exception) {
            loadError.add("${e.message ?: e}\r\n${e.cause?.message ?: ""}")
        }
    }

    /** Procesa un esquema ya parseado: resuelve sus include/import recursivos y luego indexa sus globales. */
    private fun parseSchema(fileName: String, baseUrl: String, schemaDOM: schema) {
        val basePath = File(fileName).parent ?: "."
        val separator = File.separatorChar

        // 1) Resuelve las dependencias (include / import) recursivamente.
        for (openAttrs in schemaDOM.items) {
            val schemaLocationRaw: String? = when (openAttrs) {
                is include -> openAttrs.schemaLocation
                is schemaImport -> openAttrs.schemaLocation
                else -> null
            }
            val schemaLocation = schemaLocationRaw ?: continue

            val unescaped = try {
                URLDecoder.decode(schemaLocation, "UTF-8")
            } catch (_: Exception) {
                schemaLocation
            }

            var loadedFileName: String? = basePath + separator + unescaped.replace('/', separator)
            var baseUrlForChildImport = baseUrl

            val url = schemaLocation.trim()
            if (url.startsWith("http://") || url.startsWith("https://")) {
                checkCancelled()
                loadSchemaFromUrl(basePath, url)?.let { (lf, bu) ->
                    loadedFileName = lf
                    baseUrlForChildImport = bu // The baseUrl change for this file
                }
            } else if (!File(loadedFileName!!).exists() && baseUrlForChildImport.isNotEmpty()) {
                // The relative file does not exist, so try to download it
                // from the web with the baseUrl
                val childUrl = "$baseUrlForChildImport/$schemaLocation"
                checkCancelled()
                loadSchemaFromUrl(basePath, childUrl)?.let { (lf, bu) ->
                    loadedFileName = lf
                    baseUrlForChildImport = bu
                }
            }

            // Igual que en el C#: sólo se re-importa si hay fichero cargado.
            checkCancelled()
            if (loadedFileName != null) importSchema(loadedFileName!!, baseUrlForChildImport)
        }

        // 2) Indexa las declaraciones globales.
        val nameSpace = schemaDOM.targetNamespace ?: ""
        for (openAttrs in schemaDOM.items1) {
            when (openAttrs) {
                is element -> {
                    val xsdObject = XSDObject(fileName, openAttrs.name ?: "", nameSpace, "element", openAttrs)
                    hashtableElementsByName[xsdObject.fullName] = xsdObject
                    if (firstElement == null) firstElement = xsdObject
                    elementsList.add(xsdObject)
                }
                is group -> {
                    val xsdObject = XSDObject(fileName, openAttrs.name ?: "", nameSpace, "group", openAttrs)
                    hashtableElementsByName[xsdObject.fullName] = xsdObject
                    elementsList.add(xsdObject)
                }
                is simpleType -> {
                    val xsdObject = XSDObject(fileName, openAttrs.name ?: "", nameSpace, "simpleType", openAttrs)
                    hashtableElementsByName[xsdObject.fullName] = xsdObject
                    elementsList.add(xsdObject)
                }
                is complexType -> {
                    val xsdObject = XSDObject(fileName, openAttrs.name ?: "", nameSpace, "complexType", openAttrs)
                    hashtableElementsByName[xsdObject.fullName] = xsdObject
                    elementsList.add(xsdObject)
                }
                is attribute -> {
                    val xsdAttribute = XSDAttribute(
                        fileName,
                        openAttrs.name ?: "",
                        nameSpace,
                        "attribute",
                        openAttrs.ref != null,
                        openAttrs.defaultValue,
                        openAttrs.use,
                        openAttrs
                    )
                    hashtableAttributesByName[xsdAttribute.fullName] = xsdAttribute
                }
                is attributeGroup -> {
                    val xsdAttributeGroup = XSDAttributeGroup(
                        fileName,
                        openAttrs.name ?: "",
                        nameSpace,
                        "attributeGroup",
                        openAttrs.ref != null,
                        openAttrs
                    )
                    hashtableAttributesByName[xsdAttributeGroup.fullName] = xsdAttributeGroup
                }
                else -> {}
            }
        }
    }

    // ------------------------------------------------------------------
    // Descarga de dependencias por URL
    // ------------------------------------------------------------------

    /**
     * Descarga un XSD remoto y lo deja en el directorio temporal [basePath].
     * Devuelve Pair(localFileName, baseUrlSinFichero), o null si falló la descarga
     * (equivalente a los `out` del C#, que devolvían loadedFilename = null en error).
     */
    private fun loadSchemaFromUrl(basePath: String, url: String): Pair<String?, String>? {
        val uri: URI = try {
            URI(url)
        } catch (_: Exception) {
            throw RuntimeException("Cannot read the URL '$url'!")
        }
        if (uri.rawPath.isNullOrEmpty()) return null

        val fileNameToImport = uri.rawPath!!.substringAfterLast('/')
        val baseUrl = url.substringBeforeLast('/')

        val localFile = File(basePath, fileNameToImport)
        var loadedFilename: String? = localFile.path
        if (!localFile.exists()) {
            var credentials: Credentials? = null
            var tryAgain = false
            var attemptCount = 0
            do {
                tryAgain = false
                checkCancelled()
                progressListener?.onDownloadStart(url)
                try {
                    val importedXsdFile = downloadString(url, credentials)

                    // Igual que en el C#: se elimina la declaración <!DOCTYPE ...>
                    // porque el XmlSerializer no la aceptaba.
                    val withoutDtd = Regex("<!DOCTYPE[^>]*>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                        .replace(importedXsdFile, "")
                    localFile.writeText(withoutDtd, Charsets.UTF_8)
                    progressListener?.onDownloadEnd(url, false)
                } catch (e: WebException) {
                    // Equivale al tratamiento de HttpWebResponse 401/403 del C#.
                    val code = e.httpCode
                    if (code != null &&
                        (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN)
                    ) {
                        val requested = requestCredential?.invoke(url, "", ++attemptCount)
                        if (requested != null) {
                            credentials = requested
                            tryAgain = true
                        }
                    }
                    if (!tryAgain) {
                        loadError.add("Cannot load the dependency: $uri, error: $e")
                        loadedFilename = null
                        progressListener?.onDownloadEnd(url, true)
                    }
                } catch (e: SchemaLoadCancelledException) {
                    throw e
                } catch (e: Exception) {
                    loadError.add("Cannot load the dependency: $uri, error: $e")
                    loadedFilename = null
                    progressListener?.onDownloadEnd(url, true)
                }
            } while (tryAgain)
        }
        return loadedFilename to baseUrl
    }

    // ------------------------------------------------------------------
    // Descarga HTTP cruda
    // ------------------------------------------------------------------

    /** GET HTTP textual con soporte de autenticación básica; lanza [WebException] si el código no es 2xx. */
    private fun downloadString(url: String, credentials: Credentials?): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 30_000
        connection.readTimeout = 30_000
        credentials?.let {
            val token = Base64.getEncoder().encodeToString(
                "${it.username}:${it.password}".toByteArray(StandardCharsets.UTF_8)
            )
            connection.setRequestProperty("Authorization", "Basic $token")
        }
        try {
            val code = connection.responseCode
            if (code in 200..299) {
                return connection.inputStream.readBytes().toString(StandardCharsets.UTF_8)
            }
            throw WebException(
                code,
                "HTTP $code ${connection.responseMessage ?: ""} on '$url'"
            )
        } finally {
            connection.disconnect()
        }
    }
}

/**
 * Alias de sinónimo (compile-time) que conserva el nombre original `Schema` del
 * C# para las siguientes fases del port. No genera fichero .class, así que no
 * colisiona en Windows con la clase `schema` del modelo (XMLSchema.schema).
 *
 * Nota: vive en este archivo (junto a [SchemaLoader]) porque en Windows no
 * pueden coexistir los ficheros `Schema.kt` y `schema.kt` (mismo nombre sin
 * distinguir mayúsculas en NTFS).
 */
typealias Schema = SchemaLoader

/**
 * Acepta cualquier certificado TLS (equivale a
 * `ServicePointManager.ServerCertificateValidationCallback = true` en el
 * Schema() de C#).
 *
 * FIXME(security): igual que en el original, esto es inseguro; se mantiene como
 * port 1:1 de una decisión de 2006 y debe reescribirse en fases posteriores.
 *
 * Nota: sigue siendo `private` dentro de este archivo para preservar la
 * visibilidad fichero-privada que tenía al lado de la clase Schema; es un
 * helper exclusivo de [SchemaLoader] y no rompe la regla "una clase por
 * archivo" para la clase pública de este fichero.
 */
private object TrustAllCerts {
    /** Evita instalar el trust-all más de una vez (es un efecto global del JVM). */
    private var installed = false

    /** Instala el TrustManager que acepta cualquier cadena y el hostname-verifier que acepta todo. */
    fun install() {
        if (installed) return
        installed = true
        try {
            val trustAll = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
            val context = SSLContext.getInstance("TLS")
            context.init(null, arrayOf<TrustManager>(trustAll), null)
            HttpsURLConnection.setDefaultSSLSocketFactory(context.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        } catch (_: Exception) {
            // Ignorado: si no se puede instalar, se usará el validador por defecto.
        }
    }
}
