package com.example.tunar.domain.repository

import com.example.tunar.domain.model.DetectionConfig
import com.example.tunar.domain.model.PitchSample
import com.example.tunar.domain.model.SampleRateMeasurement
import com.example.tunar.domain.model.TuningProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fuente de audio analizado. El audio se procesa en memoria y nunca se guarda ni se transmite.
 * El micrófono está abierto solo mientras haya alguien recolectando el flujo.
 */
interface AudioRepository {
    /** Flujo continuo de análisis. Falla con [com.example.tunar.domain.model.AudioCaptureException]. */
    fun pitchSamples(config: DetectionConfig): Flow<PitchSample>

    /** Mide la tasa de muestreo real del micrófono durante [durationMillis]. */
    suspend fun measureSampleRate(nominalRate: Int, durationMillis: Long): SampleRateMeasurement
}

/**
 * Perfiles de afinación.
 *
 * Fase 1: presets fijos y perfil del Modo Pro en memoria.
 * Fase 1b: la implementación pasa a Room sin cambiar este contrato.
 */
interface TuningProfileRepository {
    val presets: StateFlow<List<TuningProfile>>

    /** Último perfil usado en el Modo Pro (RF03: se restaura al volver al Modo Pro). */
    val activeProProfile: StateFlow<TuningProfile>

    suspend fun setActiveProProfile(profile: TuningProfile)
}

/** Configuración de detección vigente (resultado de la calibración). */
interface DetectionConfigRepository {
    val config: StateFlow<DetectionConfig>

    suspend fun update(config: DetectionConfig)
}
