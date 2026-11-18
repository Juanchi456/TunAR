package com.example.tunar.domain.model

/** Las 12 clases de altura del sistema occidental, en orden cromático desde Do. */
enum class NoteName(val symbol: String) {
    C("C"), C_SHARP("C#"), D("D"), D_SHARP("D#"), E("E"), F("F"),
    F_SHARP("F#"), G("G"), G_SHARP("G#"), A("A"), A_SHARP("A#"), B("B");
}

/**
 * Una nota concreta: clase de altura + octava en notación científica (A4 = La central).
 *
 * Se identifica internamente con el número MIDI (A4 = 69), lo que permite
 * comparar notas y calcular distancias en semitonos con aritmética entera.
 */
data class Pitch(val note: NoteName, val octave: Int) : Comparable<Pitch> {

    val midiNumber: Int get() = (octave + 1) * 12 + note.ordinal

    /** Etiqueta corta, por ejemplo "E2" o "F#1". */
    val label: String get() = "${note.symbol}$octave"

    fun transpose(semitones: Int): Pitch = fromMidi(midiNumber + semitones)

    override fun compareTo(other: Pitch): Int = midiNumber.compareTo(other.midiNumber)

    override fun toString(): String = label

    companion object {
        const val A4_MIDI = 69

        fun fromMidi(midi: Int): Pitch =
            Pitch(NoteName.entries[Math.floorMod(midi, 12)], Math.floorDiv(midi, 12) - 1)

        /** Límite inferior obligatorio del rango (B0 ≈ 30,87 Hz, cuerda grave del bajo de 5). */
        val LOWEST_SUPPORTED = Pitch(NoteName.B, 0)

        /** Límite superior deseable del rango (E5 ≈ 659,26 Hz). */
        val HIGHEST_SUPPORTED = Pitch(NoteName.E, 5)

        fun isSupported(pitch: Pitch): Boolean = pitch in LOWEST_SUPPORTED..HIGHEST_SUPPORTED
    }
}
