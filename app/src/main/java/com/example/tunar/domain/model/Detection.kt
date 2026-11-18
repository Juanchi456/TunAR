package com.example.tunar.domain.model

/**
 * Algoritmos de detección de frecuencia fundamental disponibles.
 *
 * El dominio solo los nombra; la implementación concreta (TarsosDSP) vive en la capa de datos.
 * [defaultThreshold] es el umbral del algoritmo: para YIN/FastYin, el máximo de aperiodicidad
 * aceptado (menor = más estricto); para MPM, el "cutoff" de claridad (mayor = más estricto).
 */
enum class PitchAlgorithm(val label: String, val defaultThreshold: Double) {
    YIN("YIN", 0.15),
    FAST_YIN("FastYin", 0.15),
    MPM("MPM", 0.97),
}

/**
 * Parámetros de captura y detección. Son el resultado de la calibración y los consume el afinador.
 *
 * Con YIN, el período máximo medible es la mitad del buffer: para B0 (30,87 Hz) a 44,1 kHz
 * el período es de ~1429 muestras, así que el buffer debe ser de al menos 4096 (~93 ms).
 *
 * @param hopSize muestras nuevas entre dos detecciones consecutivas (ventana deslizante).
 * @param sampleRateCorrection factor medido (tasa real / tasa nominal). Corrige el error
 *        constante en céntimos que produce un reloj de audio que no coincide con el nominal.
 */
data class DetectionConfig(
    val algorithm: PitchAlgorithm = PitchAlgorithm.YIN,
    val sampleRate: Int = 44_100,
    val bufferSize: Int = 4096,
    val hopSize: Int = 1024,
    val algorithmThreshold: Double = algorithm.defaultThreshold,
    val minProbability: Double = 0.80,
    val silenceThresholdDb: Double = -50.0,
    val sampleRateCorrection: Double = 1.0,
) {
    init {
        require(hopSize in 1..bufferSize) { "hopSize debe estar entre 1 y bufferSize" }
    }

    val windowMillis: Double get() = bufferSize * 1000.0 / sampleRate
    val hopMillis: Double get() = hopSize * 1000.0 / sampleRate

    /** Frecuencia más baja que el algoritmo puede medir con este buffer (período ≤ buffer/2). */
    val lowestMeasurableHz: Double get() = sampleRate / (bufferSize / 2.0)
}

/**
 * Resultado crudo de analizar una ventana de audio.
 *
 * @param frequencyHz frecuencia detectada (ya corregida por la calibración), o null si la
 *        ventana no tiene una altura definida.
 * @param levelDb nivel RMS de las muestras más recientes, en dBFS (0 = máximo).
 * @param capturedAtNanos instante (reloj monotónico) en que terminó la lectura del micrófono.
 * @param processingNanos tiempo que llevó la detección sobre la ventana.
 */
data class PitchSample(
    val frequencyHz: Double?,
    val probability: Double,
    val levelDb: Double,
    val capturedAtNanos: Long,
    val processingNanos: Long,
)

/** Estimación ya filtrada (estable) que consume el afinador. */
sealed interface PitchEstimate {
    val sample: PitchSample

    /** Nivel por debajo del umbral de silencio. */
    data class Silence(override val sample: PitchSample) : PitchEstimate

    /** Hay sonido pero no una altura confiable (ruido, varias cuerdas, señal débil). */
    data class Unclear(override val sample: PitchSample) : PitchEstimate

    /** Frecuencia estable. */
    data class Pitched(val frequencyHz: Double, override val sample: PitchSample) : PitchEstimate
}

/** Medición de la tasa de muestreo efectiva del micrófono. */
data class SampleRateMeasurement(
    val nominalRate: Int,
    val measuredRate: Double,
    val durationMillis: Long,
    val method: String,
) {
    val correctionFactor: Double get() = measuredRate / nominalRate

    /** Error constante (en céntimos) que se cometería sin corregir. */
    val errorCents: Double
        get() = 1200.0 * kotlin.math.log2(correctionFactor)
}

/** Fallas del micrófono que el usuario debe ver explicadas. */
sealed class AudioCaptureException(message: String) : Exception(message) {
    class PermissionMissing : AudioCaptureException("Falta el permiso de micrófono")
    class Unavailable(detail: String) : AudioCaptureException("Micrófono no disponible: $detail")
    class ReadFailed(code: Int) : AudioCaptureException("Error leyendo el micrófono (código $code)")
}
