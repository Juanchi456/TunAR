package com.example.tunar.domain.calibration

import com.example.tunar.domain.model.Pitch
import com.example.tunar.domain.music.TuningMath
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt

/** Clasificación de una lectura respecto de la nota que se sabe que está sonando. */
enum class ReadingKind { CORRECT_NOTE, OCTAVE_ERROR, OTHER_NOTE }

data class CalibrationSummary(
    val readings: Int,
    val correctReadings: Int,
    val meanErrorCents: Double?,
    val stdDevCents: Double?,
    val recentStdDevCents: Double?,
    val within2CentsPct: Double?,
    val within5CentsPct: Double?,
    val within8CentsPct: Double?,
    val octaveErrorPct: Double?,
    val otherNotePct: Double?,
    val processingP95Millis: Double?,
    val pipelineP95Millis: Double?,
) {
    companion object {
        val EMPTY = CalibrationSummary(0, 0, null, null, null, null, null, null, null, null, null, null)
    }
}

/**
 * Acumula las lecturas de una sesión del banco de calibración y calcula las métricas
 * definidas en los criterios de calidad (precisión, estabilidad, errores de octava, latencia).
 *
 * Se usa con una fuente de referencia conocida (generador de tonos o cuerda afinada con otro
 * afinador): [expectedPitch] es la nota que se sabe que está sonando.
 */
class CalibrationStats(
    private val expectedPitch: Pitch,
    private val referenceA4Hz: Double,
    private val recentWindow: Int = 43, // ≈ 1 s de lecturas con hop de 1024 a 44,1 kHz
) {
    private val errors = mutableListOf<Double>()
    private val processing = mutableListOf<Double>()
    private val pipeline = mutableListOf<Double>()
    private var octaveErrors = 0
    private var otherNotes = 0

    private val expectedHz = TuningMath.frequencyOf(expectedPitch, referenceA4Hz)

    fun classify(frequencyHz: Double): ReadingKind {
        val detected = TuningMath.nearestPitch(frequencyHz, referenceA4Hz)
        val diff = detected.midiNumber - expectedPitch.midiNumber
        return when {
            diff == 0 -> ReadingKind.CORRECT_NOTE
            diff % 12 == 0 -> ReadingKind.OCTAVE_ERROR
            else -> ReadingKind.OTHER_NOTE
        }
    }

    fun add(frequencyHz: Double, processingMillis: Double, pipelineMillis: Double): ReadingKind {
        val kind = classify(frequencyHz)
        when (kind) {
            ReadingKind.CORRECT_NOTE -> errors += TuningMath.centsBetween(frequencyHz, expectedHz)
            ReadingKind.OCTAVE_ERROR -> octaveErrors++
            ReadingKind.OTHER_NOTE -> otherNotes++
        }
        processing += processingMillis
        pipeline += pipelineMillis
        return kind
    }

    fun summary(): CalibrationSummary {
        val total = errors.size + octaveErrors + otherNotes
        if (total == 0) return CalibrationSummary.EMPTY
        fun pctWithin(limit: Double) =
            if (errors.isEmpty()) null else 100.0 * errors.count { abs(it) <= limit } / errors.size
        return CalibrationSummary(
            readings = total,
            correctReadings = errors.size,
            meanErrorCents = errors.takeIf { it.isNotEmpty() }?.average(),
            stdDevCents = stdDev(errors),
            recentStdDevCents = stdDev(errors.takeLast(recentWindow)),
            within2CentsPct = pctWithin(2.0),
            within5CentsPct = pctWithin(5.0),
            within8CentsPct = pctWithin(8.0),
            octaveErrorPct = 100.0 * octaveErrors / total,
            otherNotePct = 100.0 * otherNotes / total,
            processingP95Millis = percentile(processing, 95.0),
            pipelineP95Millis = percentile(pipeline, 95.0),
        )
    }

    companion object {
        fun stdDev(values: List<Double>): Double? {
            if (values.size < 2) return null
            val mean = values.average()
            return sqrt(values.sumOf { (it - mean) * (it - mean) } / (values.size - 1))
        }

        /** Percentil por el método del rango más cercano. */
        fun percentile(values: List<Double>, p: Double): Double? {
            if (values.isEmpty()) return null
            val sorted = values.sorted()
            val rank = ceil(p / 100.0 * sorted.size).toInt().coerceIn(1, sorted.size)
            return sorted[rank - 1]
        }
    }
}
