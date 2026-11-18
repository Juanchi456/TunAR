package com.example.tunar.presentation.calibration

import android.os.Build
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tunar.domain.calibration.CalibrationStats
import com.example.tunar.domain.calibration.CalibrationSummary
import com.example.tunar.domain.calibration.ReadingKind
import com.example.tunar.domain.model.DetectionConfig
import com.example.tunar.domain.model.NoteName
import com.example.tunar.domain.model.Pitch
import com.example.tunar.domain.model.PitchEstimate
import com.example.tunar.domain.model.SampleRateMeasurement
import com.example.tunar.domain.music.TuningMath
import com.example.tunar.domain.repository.AudioRepository
import com.example.tunar.domain.repository.DetectionConfigRepository
import com.example.tunar.domain.usecase.ObservePitchUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LiveSignal { IDLE, SILENCE, UNCLEAR, PITCHED }

data class CalibrationUiState(
    val config: DetectionConfig,
    val permissionGranted: Boolean = false,
    val running: Boolean = false,
    val expectedPitch: Pitch = Pitch(NoteName.A, 2),
    val liveSignal: LiveSignal = LiveSignal.IDLE,
    val liveFrequencyHz: Double? = null,
    val liveCentsFromExpected: Double? = null,
    val liveKind: ReadingKind? = null,
    val liveLevelDb: Double? = null,
    val summary: CalibrationSummary = CalibrationSummary.EMPTY,
    val measuringSampleRate: Boolean = false,
    val sampleRateMeasurement: SampleRateMeasurement? = null,
    val error: String? = null,
) {
    /** Latencia estimada: media ventana hasta que domina la nota nueva + 2 hops de confirmación + proceso. */
    val estimatedLatencyP95Millis: Double?
        get() = summary.pipelineP95Millis?.let { config.windowMillis / 2 + 2 * config.hopMillis + it }
}

/**
 * Banco de calibración en el dispositivo (paso 3 del plan de la Fase 1).
 *
 * Permite variar algoritmo, buffer, hop y umbrales mientras suena una fuente conocida
 * ([CalibrationUiState.expectedPitch]), y mide precisión, estabilidad, errores de octava,
 * tiempos de proceso y la tasa de muestreo real. La configuración elegida queda vigente
 * para el afinador a través de [DetectionConfigRepository].
 */
class CalibrationViewModel(
    private val observePitch: ObservePitchUseCase,
    private val audioRepository: AudioRepository,
    private val configRepository: DetectionConfigRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CalibrationUiState(config = configRepository.config.value))
    val state: StateFlow<CalibrationUiState> = _state.asStateFlow()

    private var captureJob: Job? = null
    private var stats = newStats()
    private var readingsSinceSummary = 0

    fun onPermissionChecked(granted: Boolean) {
        _state.update { it.copy(permissionGranted = granted) }
        if (!granted) stop()
    }

    fun start() {
        if (!_state.value.permissionGranted || captureJob?.isActive == true) return
        _state.update { it.copy(running = true, error = null) }
        captureJob = viewModelScope.launch {
            observePitch(_state.value.config)
                .catch { e -> _state.update { it.copy(error = e.message ?: e.toString(), running = false) } }
                .collect(::onEstimate)
        }
    }

    fun stop() {
        captureJob?.cancel()
        captureJob = null
        _state.update { it.copy(running = false, liveSignal = LiveSignal.IDLE) }
    }

    fun resetStats() {
        stats = newStats()
        readingsSinceSummary = 0
        _state.update { it.copy(summary = CalibrationSummary.EMPTY) }
    }

    fun onExpectedPitchChanged(semitones: Int) {
        val next = _state.value.expectedPitch.transpose(semitones)
        if (!Pitch.isSupported(next)) return
        _state.update { it.copy(expectedPitch = next) }
        resetStats()
    }

    fun onConfigChanged(config: DetectionConfig) {
        val wasRunning = captureJob?.isActive == true
        stop()
        viewModelScope.launch { configRepository.update(config) }
        _state.update { it.copy(config = config) }
        resetStats()
        if (wasRunning) start()
    }

    fun measureSampleRate() {
        stop()
        _state.update { it.copy(measuringSampleRate = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                audioRepository.measureSampleRate(_state.value.config.sampleRate, MEASURE_MILLIS)
            }
            _state.update {
                it.copy(
                    measuringSampleRate = false,
                    sampleRateMeasurement = result.getOrNull() ?: it.sampleRateMeasurement,
                    error = result.exceptionOrNull()?.let { e -> e.message ?: e.toString() },
                )
            }
        }
    }

    fun applySampleRateCorrection() {
        val m = _state.value.sampleRateMeasurement ?: return
        if (m.nominalRate != _state.value.config.sampleRate) return
        onConfigChanged(_state.value.config.copy(sampleRateCorrection = m.correctionFactor))
    }

    fun clearSampleRateCorrection() =
        onConfigChanged(_state.value.config.copy(sampleRateCorrection = 1.0))

    /** Texto para pegar en el informe de pruebas. */
    fun buildReport(): String {
        val s = _state.value
        val c = s.config
        val sum = s.summary
        fun f(v: Double?, d: Int = 2) = v?.let { String.format(Locale.ROOT, "%.${d}f", it) } ?: "—"
        return buildString {
            appendLine("TunAR · Calibración · ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())}")
            appendLine("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})")
            appendLine("Algoritmo: ${c.algorithm.label} · umbral ${f(c.algorithmThreshold)} · prob. mín. ${f(c.minProbability)}")
            appendLine("Tasa: ${c.sampleRate} Hz · buffer ${c.bufferSize} (${f(c.windowMillis, 0)} ms) · hop ${c.hopSize} (${f(c.hopMillis, 1)} ms)")
            appendLine("Silencio < ${f(c.silenceThresholdDb, 0)} dBFS · corrección de tasa ×${f(c.sampleRateCorrection, 6)}")
            s.sampleRateMeasurement?.let {
                appendLine("Tasa medida: ${f(it.measuredRate, 1)} Hz (${it.method}, ${it.durationMillis} ms) → error ${f(it.errorCents)} ¢")
            }
            appendLine("Nota esperada: ${s.expectedPitch} (${f(TuningMath.frequencyOf(s.expectedPitch, 440.0))} Hz)")
            appendLine("Lecturas: ${sum.readings} (nota correcta ${sum.correctReadings})")
            appendLine("Error medio: ${f(sum.meanErrorCents)} ¢ · desvío ${f(sum.stdDevCents)} ¢ · desvío último segundo ${f(sum.recentStdDevCents)} ¢")
            appendLine("Dentro de ±2 ¢: ${f(sum.within2CentsPct, 1)} % · ±5 ¢: ${f(sum.within5CentsPct, 1)} % · ±8 ¢: ${f(sum.within8CentsPct, 1)} %")
            appendLine("Errores de octava: ${f(sum.octaveErrorPct, 1)} % · otra nota: ${f(sum.otherNotePct, 1)} %")
            appendLine("Proceso p95: ${f(sum.processingP95Millis)} ms · pipeline p95: ${f(sum.pipelineP95Millis)} ms · latencia estimada p95: ${f(s.estimatedLatencyP95Millis, 0)} ms")
        }
    }

    private fun onEstimate(estimate: PitchEstimate) {
        val sample = estimate.sample
        when (estimate) {
            is PitchEstimate.Silence -> _state.update {
                it.copy(liveSignal = LiveSignal.SILENCE, liveFrequencyHz = null, liveCentsFromExpected = null,
                    liveKind = null, liveLevelDb = sample.levelDb)
            }
            is PitchEstimate.Unclear -> _state.update {
                it.copy(liveSignal = LiveSignal.UNCLEAR, liveFrequencyHz = null, liveCentsFromExpected = null,
                    liveKind = null, liveLevelDb = sample.levelDb)
            }
            is PitchEstimate.Pitched -> {
                val pipelineMs = (SystemClock.elapsedRealtimeNanos() - sample.capturedAtNanos) / 1e6
                val kind = stats.add(estimate.frequencyHz, sample.processingNanos / 1e6, pipelineMs)
                val expectedHz = TuningMath.frequencyOf(_state.value.expectedPitch, 440.0)
                val refreshSummary = ++readingsSinceSummary >= SUMMARY_EVERY
                if (refreshSummary) readingsSinceSummary = 0
                _state.update {
                    it.copy(
                        liveSignal = LiveSignal.PITCHED,
                        liveFrequencyHz = estimate.frequencyHz,
                        liveCentsFromExpected = TuningMath.centsBetween(estimate.frequencyHz, expectedHz),
                        liveKind = kind,
                        liveLevelDb = sample.levelDb,
                        summary = if (refreshSummary) stats.summary() else it.summary,
                    )
                }
            }
        }
    }

    private fun newStats() = CalibrationStats(_state.value.expectedPitch, 440.0)

    override fun onCleared() {
        captureJob?.cancel()
    }

    private companion object {
        const val MEASURE_MILLIS = 10_000L
        const val SUMMARY_EVERY = 5
    }
}
