package com.example.tunar.data

import com.example.tunar.data.audio.PitchAnalyzer
import com.example.tunar.domain.model.DetectionConfig
import com.example.tunar.domain.model.NoteName
import com.example.tunar.domain.model.Pitch
import com.example.tunar.domain.model.PitchAlgorithm
import com.example.tunar.domain.music.TuningMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Banco de calibración offline: compara YIN, FastYin y MPM (TarsosDSP) con señales sintéticas
 * de frecuencia conocida, usando exactamente la misma ruta de análisis que la app ([PitchAnalyzer]).
 *
 * No reemplaza la medición en un teléfono físico (micrófono real, ruido real, tasa de muestreo
 * real), pero valida el algoritmo y la configuración antes de esa prueba.
 * El informe se escribe en `app/build/reports/calibration-bench.md`.
 */
class PitchDetectionBenchTest {

    private val sampleRate = 44_100
    private val notes = listOf(
        Pitch(NoteName.B, 0), Pitch(NoteName.E, 1), Pitch(NoteName.F_SHARP, 1), Pitch(NoteName.A, 1),
        Pitch(NoteName.B, 1), Pitch(NoteName.E, 2), Pitch(NoteName.A, 2), Pitch(NoteName.D, 3),
        Pitch(NoteName.G, 3), Pitch(NoteName.B, 3), Pitch(NoteName.E, 4), Pitch(NoteName.A, 4),
        Pitch(NoteName.E, 5),
    )

    private enum class Signal(val label: String) {
        SINE("Tono puro"),
        STRING("Cuerda sintética (fundamental débil)"),
        STRING_NOISE("Cuerda sintética + ruido (SNR 10 dB)"),
    }

    private data class Result(
        val note: Pitch,
        val readings: Int,
        val pitched: Int,
        val errors: List<Double>,
        val octaveErrors: Int,
        val otherNotes: Int,
        val processingMillis: Double,
    ) {
        val maxAbsError get() = errors.maxOfOrNull { abs(it) }
        fun pctWithin(limit: Double) = if (errors.isEmpty()) 0.0 else 100.0 * errors.count { abs(it) <= limit } / errors.size
        val stdDev: Double?
            get() = if (errors.size < 2) null else errors.average().let { m ->
                sqrt(errors.sumOf { (it - m).pow(2) } / (errors.size - 1))
            }
    }

    private fun generate(signal: Signal, frequency: Double, seconds: Double = 1.0, seed: Int = 7): FloatArray {
        val n = (sampleRate * seconds).toInt()
        val random = Random(seed)
        val out = FloatArray(n)
        val harmonics = (1..10).map { k ->
            val amplitude = when (k) { 1 -> 0.4; 2 -> 1.0; else -> 1.0 / k }
            Triple(k, amplitude, random.nextDouble(0.0, 2 * PI))
        }.filter { (k, _, _) -> k * frequency < sampleRate / 2.0 }
        val norm = harmonics.sumOf { it.second }
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val value = when (signal) {
                Signal.SINE -> 0.5 * sin(2 * PI * frequency * t)
                Signal.STRING, Signal.STRING_NOISE ->
                    0.6 * exp(-t / 1.5) * harmonics.sumOf { (k, a, phase) -> a * sin(2 * PI * k * frequency * t + phase) } / norm
            }
            out[i] = value.toFloat()
        }
        if (signal == Signal.STRING_NOISE) {
            val signalRms = sqrt(out.sumOf { it.toDouble() * it } / n)
            val noiseRms = signalRms / 10.0.pow(10.0 / 20.0)
            for (i in 0 until n) out[i] += (gaussian(random) * noiseRms).toFloat()
        }
        return out
    }

    private fun gaussian(random: Random): Double {
        // Box-Muller
        val u1 = random.nextDouble(1e-12, 1.0)
        val u2 = random.nextDouble()
        return sqrt(-2.0 * kotlin.math.ln(u1)) * kotlin.math.cos(2 * PI * u2)
    }

    private fun run(config: DetectionConfig, signal: Signal, note: Pitch): Result {
        val expected = TuningMath.frequencyOf(note, 440.0)
        val audio = generate(signal, expected)
        val analyzer = PitchAnalyzer(config)
        val block = FloatArray(config.hopSize)
        val errors = mutableListOf<Double>()
        var readings = 0
        var pitched = 0
        var octave = 0
        var other = 0
        var processing = 0L
        var offset = 0
        while (offset + config.hopSize <= audio.size) {
            System.arraycopy(audio, offset, block, 0, config.hopSize)
            offset += config.hopSize
            val sample = analyzer.push(block, config.hopSize, 0L) ?: continue
            readings++
            processing += sample.processingNanos
            val f = sample.frequencyHz?.takeIf { sample.probability >= config.minProbability } ?: continue
            pitched++
            val semitones = (12 * kotlin.math.log2(f / expected)).roundToInt()
            when {
                semitones == 0 -> errors += TuningMath.centsBetween(f, expected)
                semitones % 12 == 0 -> octave++
                else -> other++
            }
        }
        return Result(note, readings, pitched, errors, octave, other, processing / 1e6 / maxOf(1, readings))
    }

    private fun fmt(value: Double?, decimals: Int = 2) =
        value?.let { String.format(Locale.ROOT, "%.${decimals}f", it) } ?: "—"

    @Test
    fun `banco de calibracion con senales sinteticas`() {
        val report = StringBuilder()
        report.appendLine("# Banco de calibración (señales sintéticas)\n")
        report.appendLine("Tasa: $sampleRate Hz · 1 s por nota · lecturas cada hop. Error en céntimos sobre lecturas con la nota correcta.\n")

        val results = mutableMapOf<Pair<PitchAlgorithm, Signal>, List<Result>>()
        for (algorithm in PitchAlgorithm.entries) {
            val config = DetectionConfig(algorithm = algorithm)
            report.appendLine("## ${algorithm.label} (buffer ${config.bufferSize}, hop ${config.hopSize}, umbral ${config.algorithmThreshold})\n")
            for (signal in Signal.entries) {
                val rows = notes.map { run(config, signal, it) }
                results[algorithm to signal] = rows
                report.appendLine("### ${signal.label}\n")
                report.appendLine("| Nota | Hz | Lecturas | Con altura | ≤ ±2 ¢ | Error máx ¢ | Desvío ¢ | Octava | Otra nota | Proc. ms |")
                report.appendLine("|---|---|---|---|---|---|---|---|---|---|")
                rows.forEach { r ->
                    report.appendLine(
                        "| ${r.note} | ${fmt(TuningMath.frequencyOf(r.note, 440.0))} | ${r.readings} | ${r.pitched} | " +
                            "${fmt(r.pctWithin(2.0), 0)} % | ${fmt(r.maxAbsError)} | ${fmt(r.stdDev)} | " +
                            "${r.octaveErrors} | ${r.otherNotes} | ${fmt(r.processingMillis)} |",
                    )
                }
                report.appendLine()
            }
        }

        report.appendLine("## Barrido de buffer (YIN, cuerda sintética)\n")
        report.appendLine("| Buffer | Ventana ms | Hz mínima medible | Notas con lecturas válidas | Octava (total) |")
        report.appendLine("|---|---|---|---|---|")
        for (size in listOf(2048, 4096, 8192)) {
            val config = DetectionConfig(bufferSize = size, hopSize = 1024)
            val rows = notes.map { run(config, Signal.STRING, it) }
            report.appendLine(
                "| $size | ${fmt(config.windowMillis, 0)} | ${fmt(config.lowestMeasurableHz)} | " +
                    "${rows.count { it.errors.isNotEmpty() }}/${rows.size} | ${rows.sumOf { it.octaveErrors }} |",
            )
        }

        val file = File("build/reports/calibration-bench.md")
        file.parentFile?.mkdirs()
        file.writeText(report.toString())
        println(report)

        // Criterio de calidad: tono puro con error ≤ ±2 céntimos y sin errores de octava (YIN por defecto).
        results.getValue(PitchAlgorithm.YIN to Signal.SINE).forEach { r ->
            assertTrue("YIN no detectó ${r.note}", r.errors.isNotEmpty())
            assertTrue("YIN ${r.note}: error máx ${r.maxAbsError}", r.maxAbsError!! <= 2.0)
            assertEquals("YIN ${r.note}: errores de octava", 0, r.octaveErrors)
        }
    }
}
