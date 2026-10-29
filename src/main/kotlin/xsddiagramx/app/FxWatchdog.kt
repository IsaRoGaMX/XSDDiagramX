//    XSD Diagram - Kotlin/JavaFX port
//    Watchdog de diagnóstico v2. Vigila el FX Application Thread de forma
//    activa y pasiva:
//      1) Latido pasivo: un AnimationTimer actualiza lastBeat cada frame.
//      2) Sonda activa: cada 200 ms envía un runLater y mide REALMENTE cuánto
//         tarda en ejecutarse (round trip). Detecta stalls de Java aunque el
//         hilo parezca "vivo".
//      3) En cada anomalía (> umbral) vuelca: FX stack, latencia, y TODOS los
//         hilos de la JVM con su estado (para pillar a QuantumRenderer/glass).
//    Log: %TEMP%/opencode/xsddiagramx-fxwatch.log  (-Dxsddiagramx.fxwatch=1)
//
//    Licencias del original: LGPL / MS-PL.
package xsddiagramx.app

import javafx.animation.AnimationTimer
import javafx.application.Platform
import javafx.stage.Stage
import java.io.File
import java.lang.System.nanoTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

object FxWatchdog {

    private const val LOG_NAME = "xsddiagramx-fxwatch.log"

    fun install(stage: Stage) {
        if ("1" != System.getProperty("xsddiagramx.fxwatch")) return

        val logFile = File(System.getProperty("java.io.tmpdir"), "opencode/$LOG_NAME")
        logFile.parentFile?.mkdirs()

        fun log(line: String) = synchronized(FxWatchdog) { logFile.appendText("$line\n") }
        fun dumpAll(tag: String) {
            val sb = StringBuilder()
            sb.appendLine("==== $tag @ ${System.currentTimeMillis()} ====")
            for ((t, st) in Thread.getAllStackTraces()) {
                if (st.isEmpty()) continue
                sb.appendLine("- ${t.name} [${t.state}]")
                for (f in st) sb.appendLine("    at $f")
            }
            sb.appendLine()
            log(sb.toString())
        }

        log("FxWatchdog v2 installed (pid ${ProcessHandle.current().pid()})")

        val lastBeat = AtomicLong(nanoTime())
        val heart = object : AnimationTimer() {
            override fun handle(now: Long) {
                lastBeat.set(nanoTime())
            }
        }
        stage.showingProperty().addListener { _, _, showing ->
            if (showing) heart.start() else heart.stop()
        }

        // Registro de cambios de tamaño de la ventana (¿llegan al resize nativo?).
        stage.widthProperty().addListener { _, _, w ->
            log("stage.width=${w.toDouble()}")
        }
        stage.heightProperty().addListener { _, _, h ->
            log("stage.height=${h.toDouble()}")
        }

        val dumping = AtomicBoolean(false)
        Thread({
            val slowWarn = 400_000_000L   // Round trip > 400 ms: aviso
            val hardFreeze = 2_000_000_000L // Round trip > 2 s: dump completo
            while (true) {
                Thread.sleep(200)
                val t0 = nanoTime()
                val latch = CountDownLatch(1)
                try {
                    Platform.runLater { latch.countDown() }
                } catch (_: Throwable) {
                    continue
                }
                val waited = latch.await(10, java.util.concurrent.TimeUnit.SECONDS)
                val roundtrip = nanoTime() - t0

                if (!waited || roundtrip > hardFreeze) {
                    if (dumping.compareAndSet(false, true)) {
                        log(
                            ">>>> HARD FREEZE: round trip runLater=" +
                                "${roundtrip / 1_000_000} ms (await ok=$waited)"
                        )
                        val sinceBeat = (nanoTime() - lastBeat.get()) / 1_000_000
                        log("last FX Animation beat was $sinceBeat ms ago")
                        val fx = Thread.getAllStackTraces().keys.firstOrNull {
                            it.name == "JavaFX Application Thread"
                        }
                        if (fx != null) {
                            log("- FX stack:")
                            for (f in fx.stackTrace) log("    at $f")
                        }
                        dumpAll("THREAD DUMP on hard freeze")
                    }
                } else {
                    if (roundtrip > slowWarn) {
                        log("slow: runLater round trip = ${roundtrip / 1_000_000} ms")
                    }
                    if (dumping.get()) {
                        log("---- recovered (${System.currentTimeMillis()}) ----")
                        dumping.set(false)
                    }
                }
            }
        }, "FxWatchdog").apply { isDaemon = true; start() }
    }
}
