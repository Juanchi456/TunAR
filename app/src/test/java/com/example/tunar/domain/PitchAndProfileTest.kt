package com.example.tunar.domain

import com.example.tunar.domain.model.BuiltInPresets
import com.example.tunar.domain.model.NoteName
import com.example.tunar.domain.model.Pitch
import com.example.tunar.domain.model.StringConfig
import com.example.tunar.domain.model.TuningProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchAndProfileTest {

    @Test
    fun `numero MIDI y conversion inversa`() {
        assertEquals(69, Pitch(NoteName.A, 4).midiNumber)
        assertEquals(23, Pitch(NoteName.B, 0).midiNumber)
        assertEquals(40, Pitch(NoteName.E, 2).midiNumber)
        for (midi in 0..127) assertEquals(midi, Pitch.fromMidi(midi).midiNumber)
        assertEquals(Pitch(NoteName.B, 1), Pitch(NoteName.E, 2).transpose(-5))
        assertEquals("F#1", Pitch(NoteName.F_SHARP, 1).label)
    }

    @Test
    fun `presets fijos son validos`() {
        assertEquals(listOf("E2", "A2", "D3", "G3", "B3", "E4"), BuiltInPresets.STANDARD.strings.map { it.pitch.label })
        assertEquals("D2", BuiltInPresets.DROP_D.strings.first().pitch.label)
        assertEquals(listOf("D2", "A2", "D3", "F#3", "A3", "D4"), BuiltInPresets.OPEN_D.strings.map { it.pitch.label })
        assertTrue(BuiltInPresets.ALL.all { p -> p.strings.all { it.centsOffset == 0.0 } })
    }

    @Test
    fun `agregar cuerdas hasta 8 baja por cuartas`() {
        var p = BuiltInPresets.STANDARD.withAddedString()
        assertEquals("B1", p.strings.first().pitch.label) // guitarra de 7
        p = p.withAddedString()
        assertEquals("F#1", p.strings.first().pitch.label) // guitarra de 8
        assertEquals(8, p.strings.size)
        assertFalse(p.canAddString)
        assertTrue(runCatching { p.withAddedString() }.isFailure)
    }

    @Test
    fun `quitar cuerdas hasta 4`() {
        var p = BuiltInPresets.STANDARD.withRemovedString().withRemovedString()
        assertEquals(listOf("D3", "G3", "B3", "E4"), p.strings.map { it.pitch.label })
        assertFalse(p.canRemoveString)
        assertTrue(runCatching { p.withRemovedString() }.isFailure)
    }

    @Test
    fun `si no hay lugar abajo se agrega arriba`() {
        val bass = TuningProfile(
            "b", "Bajo 5",
            listOf(Pitch(NoteName.B, 0), Pitch(NoteName.E, 1), Pitch(NoteName.A, 1), Pitch(NoteName.D, 2), Pitch(NoteName.G, 2))
                .map { StringConfig(it) },
        )
        val six = bass.withAddedString()
        assertEquals("B0", six.strings.first().pitch.label)
        assertEquals("C3", six.strings.last().pitch.label)
    }

    @Test
    fun `reglas de validacion`() {
        assertTrue(runCatching { BuiltInPresets.STANDARD.withReference(300.0) }.isFailure)
        assertTrue(runCatching { BuiltInPresets.STANDARD.withStringPitch(0, Pitch(NoteName.A, 0)) }.isFailure)
        assertTrue(runCatching { BuiltInPresets.STANDARD.withStringPitch(0, Pitch(NoteName.F, 5)) }.isFailure)
        val edited = BuiltInPresets.STANDARD.withStringPitch(0, Pitch(NoteName.D, 2))
        assertTrue(edited.hasSameTuningAs(BuiltInPresets.DROP_D))
    }
}
