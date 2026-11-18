package com.example.tunar.data.audio

import be.tarsos.dsp.pitch.FastYin
import be.tarsos.dsp.pitch.McLeodPitchMethod
import be.tarsos.dsp.pitch.PitchDetector
import be.tarsos.dsp.pitch.Yin
import com.example.tunar.domain.model.DetectionConfig
import com.example.tunar.domain.model.PitchAlgorithm
import com.example.tunar.domain.model.PitchSample
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Análisis de audio independiente de Android: ventana deslizante + detector de TarsosDSP.
 *
 * El micrófono entrega bloques de [DetectionConfig.hopSize] muestras; cada bloque se agrega al
 * final de una ventana de [DetectionConfig.bufferSize] muestras (descartando las más viejas) y se
 * analiza la ventana completa. Así se obtiene una lectura cada ~23 ms aunque la ventana dure ~93 ms.
 *
 * Al no depender de Android, la misma clase se usa en la app y en el banco de pruebas JVM
 * (tests con tonos sintéticos).
 */
class PitchAnalyzer(
    private val config: DetectionConfig,
    private val clock: () -> Long = System::nanoTime,
) {
    private val detector: PitchDetector = createDetector(config)
    private val window = FloatArray(config.bufferSize)
    private var filled = 0

    /**
     * Agrega [count] muestras nuevas y, si la ventana está completa, la analiza.
     * @return la lectura, o null mientras la ventana todavía se está llenando.
     */
    fun push(block: FloatArray, count: Int, capturedAtNanos: Long): PitchSample? {
        val size = window.size
        if (count >= size) {
            System.arraycopy(block, count - size, window, 0, size)
        } else {
            System.arraycopy(window, count, window, 0, size - count)
            System.arraycopy(block, 0, window, size - count, count)
        }
        filled = minOf(size, filled + count)
        if (filled < size) return null

        val start = clock()
        val result = detector.getPitch(window)
        val processing = clock() - start

        val frequency = if (result.isPitched && result.pitch > 0f) {
            result.pitch.toDouble() * config.sampleRateCorrection
        } else null

        return PitchSample(
            frequencyHz = frequency,
            probability = result.probability.toDouble(),
            levelDb = rmsDb(block, count),
            capturedAtNanos = capturedAtNanos,
            processingNanos = processing,
        )
    }

    fun reset() {
        window.fill(0f)
        filled = 0
    }

    companion object {
        /** Piso de nivel para el silencio digital absoluto. */
        const val MIN_DB = -120.0

        fun createDetector(config: DetectionConfig): PitchDetector {
            val rate = config.sampleRate.toFloat()
            val size = config.bufferSize
            val threshold = config.algorithmThreshold
            return when (config.algorithm) {
                PitchAlgorithm.YIN -> Yin(rate, size, threshold)
                PitchAlgorithm.FAST_YIN -> FastYin(rate, size, threshold)
                PitchAlgorithm.MPM -> McLeodPitchMethod(rate, size, threshold)
            }
        }

        /** Nivel RMS en dBFS de las primeras [count] muestras (rango -1..1). */
        fun rmsDb(samples: FloatArray, count: Int): Double {
            if (count <= 0) return MIN_DB
            var sum = 0.0
            for (i in 0 until count) sum += samples[i] * samples[i]
            val rms = sqrt(sum / count)
            return if (rms <= 0.0) MIN_DB else maxOf(MIN_DB, 20.0 * log10(rms))
        }
    }
}
