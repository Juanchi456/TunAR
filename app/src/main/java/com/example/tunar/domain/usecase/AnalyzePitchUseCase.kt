package com.example.tunar.domain.usecase

import com.example.tunar.domain.model.Pitch
import com.example.tunar.domain.model.PitchEstimate
import com.example.tunar.domain.model.TunerReading
import com.example.tunar.domain.model.TuningProfile
import com.example.tunar.domain.model.TuningStatus
import com.example.tunar.domain.music.TuningMath
import kotlin.math.abs

/**
 * Regla central del afinador: dada una frecuencia y un perfil, decide qué cuerda se está
 * afinando, cuánto se desvía y si está afinada.
 *
 * - Si el usuario fijó una cuerda ([lockedStringIndex]), se compara siempre contra esa.
 * - Si no, se elige la cuerda cuya nota objetivo está más cerca (en céntimos).
 */
class AnalyzePitchUseCase(
    private val inTuneToleranceCents: Double = IN_TUNE_TOLERANCE_CENTS,
) {
    operator fun invoke(
        estimate: PitchEstimate,
        profile: TuningProfile,
        lockedStringIndex: Int? = null,
    ): TunerReading = when (estimate) {
        is PitchEstimate.Silence -> TunerReading.Silence
        is PitchEstimate.Unclear -> TunerReading.WeakSignal
        is PitchEstimate.Pitched -> analyze(estimate.frequencyHz, profile, lockedStringIndex)
    }

    fun analyze(frequencyHz: Double, profile: TuningProfile, lockedStringIndex: Int?): TunerReading {
        if (!isInSupportedRange(frequencyHz, profile)) return TunerReading.OutOfRange(frequencyHz)

        val targets = profile.strings.map {
            TuningMath.frequencyOf(it.pitch, profile.referenceA4Hz, profile.tuningSystem, it.centsOffset)
        }
        val index = lockedStringIndex?.takeIf { it in targets.indices }
            ?: targets.indices.minBy { abs(TuningMath.centsBetween(frequencyHz, targets[it])) }

        val cents = TuningMath.centsBetween(frequencyHz, targets[index])
        val status = when {
            abs(cents) <= inTuneToleranceCents -> TuningStatus.IN_TUNE
            cents < 0 -> TuningStatus.FLAT
            else -> TuningStatus.SHARP
        }
        return TunerReading.Detected(
            frequencyHz = frequencyHz,
            detectedPitch = TuningMath.nearestPitch(frequencyHz, profile.referenceA4Hz, profile.tuningSystem),
            targetStringIndex = index,
            targetPitch = profile.strings[index].pitch,
            targetFrequencyHz = targets[index],
            centsFromTarget = cents,
            status = status,
        )
    }

    /** Rango soportado: de B0 a E5, con medio semitono de margen hacia afuera. */
    private fun isInSupportedRange(frequencyHz: Double, profile: TuningProfile): Boolean {
        val low = TuningMath.applyCents(
            TuningMath.frequencyOf(Pitch.LOWEST_SUPPORTED, profile.referenceA4Hz), -RANGE_MARGIN_CENTS,
        )
        val high = TuningMath.applyCents(
            TuningMath.frequencyOf(Pitch.HIGHEST_SUPPORTED, profile.referenceA4Hz), RANGE_MARGIN_CENTS,
        )
        return frequencyHz in low..high
    }

    companion object {
        /** Tolerancia para considerar una cuerda afinada (a ratificar con la calibración). */
        const val IN_TUNE_TOLERANCE_CENTS = 5.0
        private const val RANGE_MARGIN_CENTS = 50.0
    }
}
