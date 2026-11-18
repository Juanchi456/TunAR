package com.example.tunar.data.repository

import com.example.tunar.data.audio.MicrophoneDataSource
import com.example.tunar.domain.model.BuiltInPresets
import com.example.tunar.domain.model.DetectionConfig
import com.example.tunar.domain.model.PitchSample
import com.example.tunar.domain.model.SampleRateMeasurement
import com.example.tunar.domain.model.TuningProfile
import com.example.tunar.domain.repository.AudioRepository
import com.example.tunar.domain.repository.DetectionConfigRepository
import com.example.tunar.domain.repository.TuningProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioRepositoryImpl(private val microphone: MicrophoneDataSource) : AudioRepository {

    override fun pitchSamples(config: DetectionConfig): Flow<PitchSample> =
        microphone.pitchSamples(config)

    override suspend fun measureSampleRate(nominalRate: Int, durationMillis: Long): SampleRateMeasurement =
        microphone.measureSampleRate(nominalRate, durationMillis)
}

/**
 * Fase 1: presets fijos y perfil Pro en memoria (se pierde al cerrar la app).
 * Fase 1b: se reemplaza por una implementación con Room sin tocar dominio ni presentación.
 */
class InMemoryTuningProfileRepository : TuningProfileRepository {

    override val presets: StateFlow<List<TuningProfile>> = MutableStateFlow(BuiltInPresets.ALL)

    private val activePro = MutableStateFlow(BuiltInPresets.STANDARD)
    override val activeProProfile: StateFlow<TuningProfile> = activePro.asStateFlow()

    override suspend fun setActiveProProfile(profile: TuningProfile) {
        activePro.value = profile
    }
}

/** Configuración de detección en memoria (valores por defecto hasta completar la calibración). */
class InMemoryDetectionConfigRepository(
    initial: DetectionConfig = DetectionConfig(),
) : DetectionConfigRepository {

    private val state = MutableStateFlow(initial)
    override val config: StateFlow<DetectionConfig> = state.asStateFlow()

    override suspend fun update(config: DetectionConfig) {
        state.value = config
    }
}
