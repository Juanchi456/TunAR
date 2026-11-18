package com.example.tunar.domain

import com.example.tunar.domain.model.PitchEstimate
import com.example.tunar.domain.model.PitchSample
import com.example.tunar.domain.signal.PitchSignalFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchSignalFilterTest {

    private fun sample(f: Double?) = PitchSample(f, 0.95, -20.0, 0L, 0L)
    private fun PitchSignalFilter.pitched(f: Double) = process(sample(f), isSilent = false, isPitched = true)
    private fun PitchSignalFilter.silent() = process(sample(null), isSilent = true, isPitched = false)
    private fun PitchSignalFilter.unclear() = process(sample(null), isSilent = false, isPitched = false)

    @Test
    fun `la primera lectura se confirma con la segunda`() {
        val filter = PitchSignalFilter()
        assertNull(filter.pitched(82.4))
        val estimate = filter.pitched(82.5) as PitchEstimate.Pitched
        assertEquals(82.45, estimate.frequencyHz, 1e-9)
    }

    @Test
    fun `un error de octava aislado no llega a la salida`() {
        val filter = PitchSignalFilter()
        repeat(5) { filter.pitched(82.4) }
        assertNull(filter.pitched(164.8)) // octava arriba, aislada
        val next = filter.pitched(82.4) as PitchEstimate.Pitched
        assertEquals(82.4, next.frequencyHz, 1e-9)
    }

    @Test
    fun `un cambio de nota sostenido se acepta`() {
        val filter = PitchSignalFilter()
        repeat(5) { filter.pitched(82.4) }
        assertNull(filter.pitched(110.0))
        val next = filter.pitched(110.0) as PitchEstimate.Pitched
        assertEquals(110.0, next.frequencyHz, 1e-9)
    }

    @Test
    fun `la mediana descarta valores extremos dentro de la tolerancia`() {
        val filter = PitchSignalFilter()
        listOf(110.0, 110.0, 110.0, 110.0).forEach { filter.pitched(it) }
        val e = filter.pitched(112.0) as PitchEstimate.Pitched // +31 céntimos: entra a la ventana
        assertEquals(110.0, e.frequencyHz, 1e-9)
    }

    @Test
    fun `silencio y senal poco clara con histeresis`() {
        val filter = PitchSignalFilter(silentFramesToReport = 2, unclearFramesToReport = 3)
        repeat(3) { filter.pitched(82.4) }
        assertNull(filter.silent())
        assertTrue(filter.silent() is PitchEstimate.Silence)

        repeat(3) { filter.pitched(82.4) }
        assertNull(filter.unclear())
        assertNull(filter.unclear())
        assertTrue(filter.unclear() is PitchEstimate.Unclear)
    }

    @Test
    fun `tras silencio la ventana se reinicia`() {
        val filter = PitchSignalFilter()
        repeat(5) { filter.pitched(82.4) }
        repeat(2) { filter.silent() }
        assertNull(filter.pitched(110.0)) // nueva nota: necesita confirmación
        assertEquals(110.0, (filter.pitched(110.0) as PitchEstimate.Pitched).frequencyHz, 1e-9)
    }
}
