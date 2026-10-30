package xsddiagramx

import javafx.scene.canvas.Canvas
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import javafx.scene.paint.Color
import java.awt.EventQueue
import java.awt.Taskbar
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/** Construye el icono visual de la aplicación. */
object AppLogo {
    /** Carga la variante PNG más grande incluida en icon.ico o usa el icono generado de respaldo. */
    fun applicationImage(): Image {
        val pngBytes = embeddedPngIcon()
        if (pngBytes != null) {
            val image = Image(ByteArrayInputStream(pngBytes))
            if (!image.isError) return image
        }
        return createImage()
    }

    /** Actualiza el icono del Dock de macOS con la imagen extraída de icon.ico. */
    fun applyDockIcon() {
        if (!System.getProperty("os.name").lowercase().contains("mac") || !Taskbar.isTaskbarSupported()) return
        val awtIcon = SwingFXUtils.fromFXImage(applicationImage(), null) ?: return
        EventQueue.invokeLater {
            try {
                val taskbar = Taskbar.getTaskbar()
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) taskbar.setIconImage(awtIcon)
            } catch (_: UnsupportedOperationException) {
                // Algunas implementaciones de macOS no ofrecen el cambio de icono en ejecución.
            }
        }
    }

    /** Extrae el recurso PNG de mayor tamaño que esté incrustado dentro del archivo ICO. */
    private fun embeddedPngIcon(): ByteArray? {
        val bytes = AppLogo::class.java.getResourceAsStream("/icons/icon.ico")?.use { it.readBytes() } ?: return null
        if (bytes.size < 6 || bytes[0].toInt() != 0 || bytes[1].toInt() != 0 || bytes[2].toInt() != 1 || bytes[3].toInt() != 0) return null
        /** Lee un entero sin signo de cuatro bytes en formato little endian. */
        fun uint(offset: Int): Long {
            if (offset + 4 > bytes.size) return -1
            return (bytes[offset].toLong() and 0xff) or
                    ((bytes[offset + 1].toLong() and 0xff) shl 8) or
                    ((bytes[offset + 2].toLong() and 0xff) shl 16) or
                    ((bytes[offset + 3].toLong() and 0xff) shl 24)
        }
        val count = (bytes[4].toInt() and 0xff) or ((bytes[5].toInt() and 0xff) shl 8)
        var best: ByteArray? = null
        var bestArea = -1
        repeat(count) { index ->
            val entry = 6 + index * 16
            if (entry + 16 > bytes.size) return@repeat
            val width = (bytes[entry].toInt() and 0xff).let { if (it == 0) 256 else it }
            val height = (bytes[entry + 1].toInt() and 0xff).let { if (it == 0) 256 else it }
            val size = uint(entry + 8).toInt()
            val offset = uint(entry + 12).toInt()
            if (size <= 8 || offset < 0 || offset + size > bytes.size) return@repeat
            val isPng = bytes[offset] == 0x89.toByte() && bytes[offset + 1] == 0x50.toByte() &&
                    bytes[offset + 2] == 0x4e.toByte() && bytes[offset + 3] == 0x47.toByte()
            val area = width * height
            if (isPng && area > bestArea) {
                val candidate = bytes.copyOfRange(offset, offset + size)
                if (runCatching { ImageIO.read(ByteArrayInputStream(candidate)) }.getOrNull() != null) {
                    best = candidate
                    bestArea = area
                }
            }
        }
        return best
    }

    /** Genera la imagen del icono de la aplicación. */
    fun createImage(): Image {
        val icon = Canvas(64.0, 64.0); val g = icon.graphicsContext2D
        g.fill = Color.rgb(37, 67, 98); g.fillRoundRect(1.0, 1.0, 62.0, 62.0, 12.0, 12.0)
        g.stroke = Color.WHITE; g.lineWidth = 3.0; g.strokeRoundRect(12.0, 13.0, 40.0, 38.0, 4.0, 4.0)
        g.strokeLine(12.0, 24.0, 52.0, 24.0); g.strokeLine(25.0, 24.0, 25.0, 51.0); g.strokeLine(39.0, 24.0, 39.0, 51.0)
        return icon.snapshot(null, null)
    }
}