package com.example.tunar.domain.model

import com.example.tunar.domain.model.NoteName.A
import com.example.tunar.domain.model.NoteName.B
import com.example.tunar.domain.model.NoteName.D
import com.example.tunar.domain.model.NoteName.E
import com.example.tunar.domain.model.NoteName.F_SHARP
import com.example.tunar.domain.model.NoteName.G

/** Presets fijos de la Fase 1 (guitarra de 6 cuerdas, de grave a aguda). */
object BuiltInPresets {

    val STANDARD = preset(
        "standard", "Estándar",
        Pitch(E, 2), Pitch(A, 2), Pitch(D, 3), Pitch(G, 3), Pitch(B, 3), Pitch(E, 4),
    )

    val DROP_D = preset(
        "drop_d", "Drop D",
        Pitch(D, 2), Pitch(A, 2), Pitch(D, 3), Pitch(G, 3), Pitch(B, 3), Pitch(E, 4),
    )

    val OPEN_D = preset(
        "open_d", "Open D",
        Pitch(D, 2), Pitch(A, 2), Pitch(D, 3), Pitch(F_SHARP, 3), Pitch(A, 3), Pitch(D, 4),
    )

    val ALL: List<TuningProfile> = listOf(STANDARD, DROP_D, OPEN_D)

    private fun preset(id: String, name: String, vararg pitches: Pitch) =
        TuningProfile(id = id, name = name, strings = pitches.map { StringConfig(it) })
}
