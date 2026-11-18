package com.example.tunar.presentation.tuner

import com.example.tunar.domain.model.BuiltInPresets
import com.example.tunar.domain.model.TunerReading
import com.example.tunar.domain.model.TuningProfile

enum class TunerMode { BASIC, PRO }

/** Estado principal del panel del afinador. Cada variante tiene su diseño y su acción. */
sealed interface TunerDisplay {
    /** Sin permiso todavía: explicación + botón "Permitir". */
    data object NeedsPermission : TunerDisplay

    /** Permiso rechazado de forma permanente: explicación + botón "Abrir ajustes". */
    data object PermissionBlocked : TunerDisplay

    /** Abriendo el micrófono (carga). */
    data object Starting : TunerDisplay

    /** Escuchando, sin sonido: "Tocá una cuerda". */
    data object Listening : TunerDisplay

    /** Sonido sin altura confiable: no se muestra ningún valor. */
    data object WeakSignal : TunerDisplay

    data class OutOfRange(val frequencyHz: Double) : TunerDisplay

    data class Reading(val reading: TunerReading.Detected) : TunerDisplay

    /** El micrófono falló (ocupado, no disponible): mensaje + "Reintentar". */
    data class Error(val message: String) : TunerDisplay
}

/** Mensajes puntuales (snackbar) por acciones de edición que no se pudieron aplicar. */
enum class TunerMessage { MAX_STRINGS, MIN_STRINGS, INVALID_REFERENCE, INVALID_NOTE }

data class TunerUiState(
    val mode: TunerMode,
    val profile: TuningProfile = BuiltInPresets.STANDARD,
    val presets: List<TuningProfile> = BuiltInPresets.ALL,
    /** Preset que coincide con el perfil actual, o null si es una afinación personalizada. */
    val matchingPreset: TuningProfile? = BuiltInPresets.STANDARD,
    val display: TunerDisplay = TunerDisplay.NeedsPermission,
    val lockedStringIndex: Int? = null,
    val isEditing: Boolean = false,
    /** Cuerda cuya nota se está eligiendo en el selector (modo edición). */
    val pickerStringIndex: Int? = null,
    val message: TunerMessage? = null,
) {
    val canEdit: Boolean get() = mode == TunerMode.PRO

    /** Cuerda que se resalta: la que se está afinando según la lectura actual. */
    val activeStringIndex: Int?
        get() = (display as? TunerDisplay.Reading)?.reading?.targetStringIndex ?: lockedStringIndex
}
