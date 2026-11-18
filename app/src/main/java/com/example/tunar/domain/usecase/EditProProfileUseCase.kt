package com.example.tunar.domain.usecase

import com.example.tunar.domain.model.Pitch
import com.example.tunar.domain.model.TuningProfile
import com.example.tunar.domain.repository.TuningProfileRepository

/**
 * Edición del perfil del Modo Pro (RF02). Cada operación valida las reglas del
 * [TuningProfile] (4 a 8 cuerdas, rango de notas y de referencia) y, si son válidas,
 * reemplaza el perfil activo, con lo que el afinador usa la nueva configuración al instante.
 */
class EditProProfileUseCase(private val repository: TuningProfileRepository) {

    suspend fun selectPreset(preset: TuningProfile) = repository.setActiveProProfile(preset)

    suspend fun changeString(index: Int, pitch: Pitch): Result<TuningProfile> =
        edit { it.withStringPitch(index, pitch) }

    suspend fun addString(): Result<TuningProfile> = edit { it.withAddedString() }

    suspend fun removeString(): Result<TuningProfile> = edit { it.withRemovedString() }

    suspend fun setReference(referenceHz: Double): Result<TuningProfile> =
        edit { it.withReference(referenceHz) }

    private suspend fun edit(change: (TuningProfile) -> TuningProfile): Result<TuningProfile> =
        runCatching {
            change(repository.activeProProfile.value)
                .copy(id = CUSTOM_PROFILE_ID, name = CUSTOM_PROFILE_NAME)
        }.onSuccess { repository.setActiveProProfile(it) }

    companion object {
        const val CUSTOM_PROFILE_ID = "custom"
        const val CUSTOM_PROFILE_NAME = "Personalizado"
    }
}
