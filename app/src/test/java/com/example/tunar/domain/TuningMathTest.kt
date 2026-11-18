package com.example.tunar.domain

import com.example.tunar.domain.model.NoteName
import com.example.tunar.domain.model.Pitch
import com.example.tunar.domain.music.TuningMath
import org.junit.Assert.assertEquals
import org.junit.Test

class TuningMathTest {

    private val delta = 0.01

    @Test
    fun `A4 es la referencia`() {
        assertEquals(440.0, TuningMath.frequencyOf(Pitch(NoteName.A, 4), 440.0), delta)
        assertEquals(432.0, TuningMath.frequencyOf(Pitch(NoteName.A, 4), 432.0), delta)
    }

    @Test
    fun `frecuencias de cuerdas conocidas a 440 Hz`() {
        assertEquals(30.87, TuningMath.frequencyOf(Pitch(NoteName.B, 0), 440.0), delta)
        assertEquals(46.25, TuningMath.frequencyOf(Pitch(NoteName.F_SHARP, 1), 440.0), delta)
        assertEquals(82.41, TuningMath.frequencyOf(Pitch(NoteName.E, 2), 440.0), delta)
        assertEquals(329.63, TuningMath.frequencyOf(Pitch(NoteName.E, 4), 440.0), delta)
        assertEquals(659.26, TuningMath.frequencyOf(Pitch(NoteName.E, 5), 440.0), delta)
    }

    @Test
    fun `cambiar la referencia desplaza todas las notas por igual`() {
        val e2at440 = TuningMath.frequencyOf(Pitch(NoteName.E, 2), 440.0)
        val e2at432 = TuningMath.frequencyOf(Pitch(NoteName.E, 2), 432.0)
        assertEquals(TuningMath.centsBetween(432.0, 440.0), TuningMath.centsBetween(e2at432, e2at440), 1e-9)
    }

    @Test
    fun `centimos entre frecuencias`() {
        assertEquals(0.0, TuningMath.centsBetween(440.0, 440.0), 1e-9)
        assertEquals(1200.0, TuningMath.centsBetween(880.0, 440.0), 1e-9)
        assertEquals(-1200.0, TuningMath.centsBetween(220.0, 440.0), 1e-9)
        assertEquals(100.0, TuningMath.centsBetween(TuningMath.frequencyOf(Pitch(NoteName.A_SHARP, 4), 440.0), 440.0), 1e-9)
        assertEquals(-31.77, TuningMath.centsBetween(432.0, 440.0), delta)
    }

    @Test
    fun `ajuste fino en centimos`() {
        val f = TuningMath.frequencyOf(Pitch(NoteName.A, 4), 440.0, centsOffset = 10.0)
        assertEquals(10.0, TuningMath.centsBetween(f, 440.0), 1e-9)
    }

    @Test
    fun `nota mas cercana`() {
        assertEquals(Pitch(NoteName.A, 4), TuningMath.nearestPitch(445.0, 440.0))
        assertEquals(Pitch(NoteName.E, 2), TuningMath.nearestPitch(80.5, 440.0))
        assertEquals(Pitch(NoteName.B, 0), TuningMath.nearestPitch(31.0, 440.0))
        // 49 céntimos por encima de A4 sigue siendo A4; 51 ya es A#4.
        assertEquals(Pitch(NoteName.A, 4), TuningMath.nearestPitch(TuningMath.applyCents(440.0, 49.0), 440.0))
        assertEquals(Pitch(NoteName.A_SHARP, 4), TuningMath.nearestPitch(TuningMath.applyCents(440.0, 51.0), 440.0))
        // Con referencia 432, 432 Hz es A4.
        assertEquals(Pitch(NoteName.A, 4), TuningMath.nearestPitch(432.0, 432.0))
    }
}
