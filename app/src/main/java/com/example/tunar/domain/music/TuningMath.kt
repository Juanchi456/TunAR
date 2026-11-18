package com.example.tunar.domain.music

import com.example.tunar.domain.model.Pitch
import com.example.tunar.domain.model.TuningSystem
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Cálculos musicales puros: frecuencia de una nota, nota más cercana y desvío en céntimos.
 *
 * La frecuencia de referencia (A4) y el sistema de afinación son parámetros, de modo que
 * el algoritmo de detección no sabe nada de ellos y la Fase 2 (temperamentos alternativos)
 * solo requiere agregar ramas a los `when`.
 */
object TuningMath {

    const val CENTS_PER_OCTAVE = 1200.0
    const val CENTS_PER_SEMITONE = 100.0

    /** Frecuencia objetivo de [pitch], con un ajuste fino opcional en céntimos. */
    fun frequencyOf(
        pitch: Pitch,
        referenceA4Hz: Double,
        system: TuningSystem = TuningSystem.EQUAL_TEMPERAMENT,
        centsOffset: Double = 0.0,
    ): Double {
        val base = when (system) {
            TuningSystem.EQUAL_TEMPERAMENT ->
                referenceA4Hz * 2.0.pow((pitch.midiNumber - Pitch.A4_MIDI) / 12.0)
        }
        return applyCents(base, centsOffset)
    }

    /** Desvío de [frequencyHz] respecto de [targetHz]: positivo = más agudo, negativo = más grave. */
    fun centsBetween(frequencyHz: Double, targetHz: Double): Double =
        CENTS_PER_OCTAVE * log2(frequencyHz / targetHz)

    fun applyCents(frequencyHz: Double, cents: Double): Double =
        frequencyHz * 2.0.pow(cents / CENTS_PER_OCTAVE)

    /** Nota más cercana a [frequencyHz] en el sistema indicado. */
    fun nearestPitch(
        frequencyHz: Double,
        referenceA4Hz: Double,
        system: TuningSystem = TuningSystem.EQUAL_TEMPERAMENT,
    ): Pitch = when (system) {
        TuningSystem.EQUAL_TEMPERAMENT -> {
            val semitonesFromA4 = 12.0 * log2(frequencyHz / referenceA4Hz)
            Pitch.fromMidi(Pitch.A4_MIDI + semitonesFromA4.roundToInt())
        }
    }
}
