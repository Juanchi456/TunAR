package com.example.tunar.domain.signal

import com.example.tunar.domain.model.PitchEstimate
import com.example.tunar.domain.model.PitchSample
import com.example.tunar.domain.music.TuningMath
import kotlin.math.abs

/**
 * Convierte las lecturas crudas (una por ventana) en estimaciones estables.
 *
 * - **Mediana móvil** de las últimas [windowSize] frecuencias: reduce el temblor de la aguja.
 * - **Confirmación de saltos**: una lectura que se aleja más de [jumpToleranceCents] de la
 *   mediana se toma como candidata y solo se acepta si la siguiente coincide con ella. Así un
 *   error de octava aislado (típico en el ataque de la cuerda) nunca llega a la pantalla.
 * - **Histéresis de estados**: silencio y "señal poco clara" se informan recién después de
 *   varias ventanas seguidas, para que un único frame ruidoso no haga parpadear la interfaz.
 *
 * Devuelve `null` cuando no hay nada nuevo que mostrar (la UI conserva lo anterior).
 * No es thread-safe: cada flujo de audio usa su propia instancia.
 */
class PitchSignalFilter(
    private val windowSize: Int = 5,
    private val jumpToleranceCents: Double = 50.0,
    private val silentFramesToReport: Int = 2,
    private val unclearFramesToReport: Int = 3,
) {
    private val window = ArrayDeque<Double>()
    private var candidate: Double? = null
    private var silentCount = 0
    private var unclearCount = 0

    fun reset() {
        window.clear()
        candidate = null
        silentCount = 0
        unclearCount = 0
    }

    fun process(sample: PitchSample, isSilent: Boolean, isPitched: Boolean): PitchEstimate? {
        val frequency = sample.frequencyHz
        return when {
            isSilent -> {
                unclearCount = 0
                silentCount++
                if (silentCount >= silentFramesToReport) {
                    clearPitch()
                    PitchEstimate.Silence(sample)
                } else null
            }

            !isPitched || frequency == null -> {
                silentCount = 0
                unclearCount++
                if (unclearCount >= unclearFramesToReport) {
                    clearPitch()
                    PitchEstimate.Unclear(sample)
                } else null
            }

            else -> {
                silentCount = 0
                unclearCount = 0
                acceptPitch(frequency)?.let { PitchEstimate.Pitched(it, sample) }
            }
        }
    }

    private fun acceptPitch(frequency: Double): Double? {
        if (window.isNotEmpty() && isClose(frequency, median())) {
            window.addLast(frequency)
            if (window.size > windowSize) window.removeFirst()
            candidate = null
            return median()
        }
        // Salto (o primera lectura): se confirma con la lectura siguiente.
        val previousCandidate = candidate
        if (previousCandidate != null && isClose(frequency, previousCandidate)) {
            window.clear()
            window.addLast(previousCandidate)
            window.addLast(frequency)
            candidate = null
            return median()
        }
        candidate = frequency
        return null
    }

    private fun clearPitch() {
        window.clear()
        candidate = null
    }

    private fun isClose(a: Double, b: Double) =
        abs(TuningMath.centsBetween(a, b)) <= jumpToleranceCents

    private fun median(): Double {
        val sorted = window.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
    }
}
