package com.example.tunar.domain.model

enum class TuningStatus { IN_TUNE, FLAT, SHARP }

/** Lo que el afinador tiene para mostrar en un instante dado. */
sealed interface TunerReading {

    /** No hay sonido suficiente: se invita al usuario a tocar una cuerda. */
    data object Silence : TunerReading

    /** Hay sonido pero no una altura confiable: no se muestra ningún valor para no confundir. */
    data object WeakSignal : TunerReading

    /** Altura detectada fuera del rango soportado por la app. */
    data class OutOfRange(val frequencyHz: Double) : TunerReading

    /**
     * Altura detectada y comparada con la cuerda objetivo.
     *
     * @param detectedPitch nota más cercana a lo que suena (puede no ser la de la cuerda).
     * @param centsFromTarget desvío respecto de la nota de la cuerda: es lo que se afina.
     */
    data class Detected(
        val frequencyHz: Double,
        val detectedPitch: Pitch,
        val targetStringIndex: Int,
        val targetPitch: Pitch,
        val targetFrequencyHz: Double,
        val centsFromTarget: Double,
        val status: TuningStatus,
    ) : TunerReading
}
