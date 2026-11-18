package com.example.tunar.domain

import com.example.tunar.domain.calibration.CalibrationStats
import com.example.tunar.domain.calibration.ReadingKind
import com.example.tunar.domain.model.NoteName
import com.example.tunar.domain.model.Pitch
import com.example.tunar.domain.music.TuningMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalibrationStatsTest {

    private val a2 = Pitch(NoteName.A, 2)
    private val a2Hz = TuningMath.frequencyOf(a2, 440.0)

    @Test
    fun `clasifica octava y otra nota`() {
        val stats = CalibrationStats(a2, 440.0)
        assertEquals(ReadingKind.CORRECT_NOTE, stats.classify(a2Hz))
        assertEquals(ReadingKind.OCTAVE_ERROR, stats.classify(a2Hz * 2))
        assertEquals(ReadingKind.OCTAVE_ERROR, stats.classify(a2Hz / 2))
        assertEquals(ReadingKind.OTHER_NOTE, stats.classify(a2Hz * 1.5))
    }

    @Test
    fun `metricas de precision`() {
        val stats = CalibrationStats(a2, 440.0)
        listOf(1.0, -1.0, 3.0, -3.0, 6.0, -10.0).forEach {
            stats.add(TuningMath.applyCents(a2Hz, it), processingMillis = 2.0, pipelineMillis = 5.0)
        }
        stats.add(a2Hz * 2, 2.0, 5.0)
        val s = stats.summary()
        assertEquals(7, s.readings)
        assertEquals(6, s.correctReadings)
        assertEquals(-4.0 / 6, s.meanErrorCents!!, 1e-6)
        assertEquals(100.0 * 2 / 6, s.within2CentsPct!!, 1e-6)
        assertEquals(100.0 * 4 / 6, s.within5CentsPct!!, 1e-6)
        assertEquals(100.0 * 5 / 6, s.within8CentsPct!!, 1e-6)
        assertEquals(100.0 / 7, s.octaveErrorPct!!, 1e-6)
        assertEquals(5.0, s.pipelineP95Millis!!, 1e-9)
    }

    @Test
    fun `percentil y desvio`() {
        val values = (1..100).map { it.toDouble() }
        assertEquals(95.0, CalibrationStats.percentile(values, 95.0)!!, 1e-9)
        assertEquals(1.0, CalibrationStats.percentile(values, 0.0)!!, 1e-9)
        assertNull(CalibrationStats.percentile(emptyList(), 95.0))
        assertEquals(1.0, CalibrationStats.stdDev(listOf(1.0, 2.0, 3.0))!!, 1e-9)
        assertNull(CalibrationStats.stdDev(listOf(1.0)))
    }
}
