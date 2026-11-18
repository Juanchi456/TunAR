package com.example.tunar.domain

import com.example.tunar.domain.model.BuiltInPresets
import com.example.tunar.domain.model.NoteName
import com.example.tunar.domain.model.Pitch
import com.example.tunar.domain.model.TunerReading
import com.example.tunar.domain.model.TuningProfile
import com.example.tunar.domain.model.TuningStatus
import com.example.tunar.domain.music.TuningMath
import com.example.tunar.domain.usecase.AnalyzePitchUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyzePitchUseCaseTest {

    private val analyze = AnalyzePitchUseCase()
    private val standard = BuiltInPresets.STANDARD
    private val e2 = TuningMath.frequencyOf(Pitch(NoteName.E, 2), 440.0)
    private val a2 = TuningMath.frequencyOf(Pitch(NoteName.A, 2), 440.0)

    private fun detected(freq: Double, locked: Int? = null, profile: TuningProfile = standard) =
        analyze.analyze(freq, profile, locked) as TunerReading.Detected

    @Test
    fun `elige la cuerda mas cercana`() {
        assertEquals(0, detected(e2).targetStringIndex)
        assertEquals(1, detected(TuningMath.applyCents(a2, -30.0)).targetStringIndex)
        assertEquals(5, detected(330.0).targetStringIndex)
    }

    @Test
    fun `estado segun tolerancia`() {
        assertEquals(TuningStatus.IN_TUNE, detected(TuningMath.applyCents(e2, 4.9)).status)
        assertEquals(TuningStatus.IN_TUNE, detected(TuningMath.applyCents(e2, -4.9)).status)
        assertEquals(TuningStatus.SHARP, detected(TuningMath.applyCents(e2, 12.0)).status)
        assertEquals(TuningStatus.FLAT, detected(TuningMath.applyCents(e2, -12.0)).status)
        assertEquals(-12.0, detected(TuningMath.applyCents(e2, -12.0)).centsFromTarget, 1e-6)
    }

    @Test
    fun `cuerda fijada por el usuario`() {
        // Suena un A2 pero el usuario fijó la sexta cuerda (E2): está 500 céntimos agudo.
        val r = detected(a2, locked = 0)
        assertEquals(0, r.targetStringIndex)
        assertEquals(500.0, r.centsFromTarget, 1e-6)
        assertEquals(Pitch(NoteName.A, 2), r.detectedPitch)
        assertEquals(TuningStatus.SHARP, r.status)
    }

    @Test
    fun `la referencia del perfil cambia el objetivo`() {
        val at432 = standard.withReference(432.0)
        val e2at432 = TuningMath.frequencyOf(Pitch(NoteName.E, 2), 432.0)
        assertEquals(0.0, detected(e2at432, profile = at432).centsFromTarget, 1e-6)
        assertEquals(TuningStatus.SHARP, detected(e2, profile = at432).status)
    }

    @Test
    fun `fuera de rango`() {
        assertTrue(analyze.analyze(25.0, standard, null) is TunerReading.OutOfRange)
        assertTrue(analyze.analyze(1000.0, standard, null) is TunerReading.OutOfRange)
        assertTrue(analyze.analyze(30.87, standard, null) is TunerReading.Detected)
    }
}
