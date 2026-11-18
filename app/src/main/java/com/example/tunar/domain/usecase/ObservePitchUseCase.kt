package com.example.tunar.domain.usecase

import com.example.tunar.domain.model.DetectionConfig
import com.example.tunar.domain.model.PitchEstimate
import com.example.tunar.domain.repository.AudioRepository
import com.example.tunar.domain.signal.PitchSignalFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Flujo de estimaciones estables a partir del micrófono.
 *
 * Aplica los umbrales de la calibración (silencio y probabilidad mínima) y el
 * [PitchSignalFilter]. Cada recolección abre su propio micrófono y su propio filtro.
 */
class ObservePitchUseCase(
    private val audioRepository: AudioRepository,
    private val filterFactory: () -> PitchSignalFilter = { PitchSignalFilter() },
) {
    operator fun invoke(config: DetectionConfig): Flow<PitchEstimate> = flow {
        val filter = filterFactory()
        audioRepository.pitchSamples(config).collect { sample ->
            val isSilent = sample.levelDb < config.silenceThresholdDb
            val isPitched = sample.frequencyHz != null && sample.probability >= config.minProbability
            filter.process(sample, isSilent, isPitched)?.let { emit(it) }
        }
    }
}
