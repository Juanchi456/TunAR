package com.example.tunar.di

import android.content.Context
import com.example.tunar.data.audio.MicrophoneDataSource
import com.example.tunar.data.repository.AudioRepositoryImpl
import com.example.tunar.data.repository.InMemoryDetectionConfigRepository
import com.example.tunar.data.repository.InMemoryTuningProfileRepository
import com.example.tunar.domain.repository.AudioRepository
import com.example.tunar.domain.repository.DetectionConfigRepository
import com.example.tunar.domain.repository.TuningProfileRepository
import com.example.tunar.domain.usecase.AnalyzePitchUseCase
import com.example.tunar.domain.usecase.EditProProfileUseCase
import com.example.tunar.domain.usecase.ObservePitchUseCase

/**
 * Inyección de dependencias manual: un único lugar donde se decide qué implementación
 * concreta recibe cada interfaz del dominio. Con pocas dependencias no se justifica Hilt;
 * cambiar a Room en la Fase 1b implica modificar solo esta clase.
 */
class AppContainer(context: Context) {

    val audioRepository: AudioRepository =
        AudioRepositoryImpl(MicrophoneDataSource(context.applicationContext))

    val tuningProfileRepository: TuningProfileRepository = InMemoryTuningProfileRepository()

    val detectionConfigRepository: DetectionConfigRepository = InMemoryDetectionConfigRepository()

    val analyzePitch = AnalyzePitchUseCase()

    val editProProfile = EditProProfileUseCase(tuningProfileRepository)

    fun observePitch() = ObservePitchUseCase(audioRepository)
}
