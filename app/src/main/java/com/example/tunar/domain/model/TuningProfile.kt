package com.example.tunar.domain.model

/**
 * Sistema de afinación con el que se calculan las frecuencias objetivo.
 *
 * En la Fase 1 solo existe el temperamento igual. Los temperamentos justo y
 * pitagórico están planificados para la Fase 2; el tipo ya forma parte del
 * perfil para no tener que migrar el modelo ni la base de datos más adelante.
 */
enum class TuningSystem { EQUAL_TEMPERAMENT }

/**
 * Configuración de una cuerda.
 *
 * @param centsOffset ajuste fino respecto de la nota (Fase 2). En la Fase 1 siempre es 0.
 */
data class StringConfig(
    val pitch: Pitch,
    val centsOffset: Double = 0.0,
)

/**
 * Perfil (o preset) de afinación: qué nota se busca en cada cuerda y con qué referencia.
 *
 * Las cuerdas se ordenan de la más grave a la más aguda. El modelo es inmutable:
 * cada operación de edición devuelve un perfil nuevo y valida las reglas de negocio.
 */
data class TuningProfile(
    val id: String,
    val name: String,
    val strings: List<StringConfig>,
    val referenceA4Hz: Double = DEFAULT_REFERENCE_HZ,
    val tuningSystem: TuningSystem = TuningSystem.EQUAL_TEMPERAMENT,
) {
    init {
        require(strings.size in MIN_STRINGS..MAX_STRINGS) {
            "Un perfil debe tener entre $MIN_STRINGS y $MAX_STRINGS cuerdas (tiene ${strings.size})"
        }
        require(referenceA4Hz in MIN_REFERENCE_HZ..MAX_REFERENCE_HZ) {
            "La referencia debe estar entre $MIN_REFERENCE_HZ y $MAX_REFERENCE_HZ Hz"
        }
        require(strings.all { Pitch.isSupported(it.pitch) }) {
            "Todas las cuerdas deben estar entre ${Pitch.LOWEST_SUPPORTED} y ${Pitch.HIGHEST_SUPPORTED}"
        }
    }

    val canAddString: Boolean get() = strings.size < MAX_STRINGS
    val canRemoveString: Boolean get() = strings.size > MIN_STRINGS

    fun withStringPitch(index: Int, pitch: Pitch): TuningProfile =
        copy(strings = strings.toMutableList().also { it[index] = it[index].copy(pitch = pitch) })

    /**
     * Agrega una cuerda grave una cuarta justa por debajo de la más grave
     * (como pasar de guitarra de 6 a 7 cuerdas: E2 → B1). Si eso sale del
     * rango soportado, la agrega una cuarta por encima de la más aguda.
     */
    fun withAddedString(): TuningProfile {
        check(canAddString) { "Ya hay $MAX_STRINGS cuerdas" }
        val lower = strings.first().pitch.transpose(-5)
        return if (Pitch.isSupported(lower)) {
            copy(strings = listOf(StringConfig(lower)) + strings)
        } else {
            val upper = strings.last().pitch.transpose(5)
                .coerceAtMost(Pitch.HIGHEST_SUPPORTED)
            copy(strings = strings + StringConfig(upper))
        }
    }

    /** Quita la cuerda más grave (operación inversa de [withAddedString]). */
    fun withRemovedString(): TuningProfile {
        check(canRemoveString) { "El mínimo es $MIN_STRINGS cuerdas" }
        return copy(strings = strings.drop(1))
    }

    fun withReference(referenceHz: Double): TuningProfile = copy(referenceA4Hz = referenceHz)

    /** Compara solo el contenido musical, ignorando id y nombre. */
    fun hasSameTuningAs(other: TuningProfile): Boolean =
        strings == other.strings &&
            referenceA4Hz == other.referenceA4Hz &&
            tuningSystem == other.tuningSystem

    companion object {
        const val MIN_STRINGS = 4
        const val MAX_STRINGS = 8
        const val DEFAULT_REFERENCE_HZ = 440.0
        const val MIN_REFERENCE_HZ = 400.0
        const val MAX_REFERENCE_HZ = 480.0
    }
}
