package com.example.tunar.presentation.tuner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tunar.domain.model.AudioCaptureException
import com.example.tunar.domain.model.BuiltInPresets
import com.example.tunar.domain.model.Pitch
import com.example.tunar.domain.model.PitchEstimate
import com.example.tunar.domain.model.TunerReading
import com.example.tunar.domain.model.TuningProfile
import com.example.tunar.domain.repository.DetectionConfigRepository
import com.example.tunar.domain.repository.TuningProfileRepository
import com.example.tunar.domain.usecase.AnalyzePitchUseCase
import com.example.tunar.domain.usecase.EditProProfileUseCase
import com.example.tunar.domain.usecase.ObservePitchUseCase
import com.example.tunar.presentation.common.MicPermission
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla del afinador, para el Modo Básico y el Modo Pro.
 *
 * Flujo de datos: micrófono → [ObservePitchUseCase] → [AnalyzePitchUseCase] (con el perfil
 * activo) → [TunerUiState] → Compose. El micrófono solo está abierto mientras la UI recolecta
 * el estado (`WhileSubscribed`): al salir de la pantalla o pasar a segundo plano se libera.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TunerViewModel(
    private val mode: TunerMode,
    private val observePitch: ObservePitchUseCase,
    private val analyzePitch: AnalyzePitchUseCase,
    private val editProProfile: EditProProfileUseCase,
    profileRepository: TuningProfileRepository,
    detectionConfigRepository: DetectionConfigRepository,
) : ViewModel() {

    /** Lo que llega del micrófono, antes de compararlo con el perfil. */
    private sealed interface AudioState {
        data object NeedsPermission : AudioState
        data object Blocked : AudioState
        data object Starting : AudioState
        data class Estimate(val estimate: PitchEstimate) : AudioState
        data class Failed(val error: Throwable) : AudioState
    }

    private data class LocalState(
        val locked: Int? = null,
        val editing: Boolean = false,
        val picker: Int? = null,
        val message: TunerMessage? = null,
    )

    private val permission = MutableStateFlow(MicPermission.UNKNOWN)
    private val retry = MutableStateFlow(0)
    private val local = MutableStateFlow(LocalState())

    /** RF03: el Modo Básico usa siempre la afinación estándar; el Pro, el último perfil editado. */
    private val profile: StateFlow<TuningProfile> = when (mode) {
        TunerMode.BASIC -> MutableStateFlow(BuiltInPresets.STANDARD)
        TunerMode.PRO -> profileRepository.activeProProfile
    }

    private val audio: Flow<AudioState> =
        combine(permission, retry, detectionConfigRepository.config) { p, _, config -> p to config }
            .flatMapLatest { (p, config) ->
                when (p) {
                    MicPermission.GRANTED -> observePitch(config)
                        .map<PitchEstimate, AudioState> { AudioState.Estimate(it) }
                        .onStart { emit(AudioState.Starting) }
                        .catch { emit(AudioState.Failed(it)) }
                    MicPermission.PERMANENTLY_DENIED -> flowOf(AudioState.Blocked)
                    else -> flowOf(AudioState.NeedsPermission)
                }
            }

    val uiState: StateFlow<TunerUiState> =
        combine(audio, profile, profileRepository.presets, local) { audio, profile, presets, local ->
            val locked = local.locked?.takeIf { it in profile.strings.indices }
            TunerUiState(
                mode = mode,
                profile = profile,
                presets = presets,
                matchingPreset = presets.firstOrNull { it.hasSameTuningAs(profile) },
                display = toDisplay(audio, profile, locked),
                lockedStringIndex = locked,
                isEditing = local.editing && mode == TunerMode.PRO,
                pickerStringIndex = local.picker,
                message = local.message,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = TunerUiState(mode = mode),
        )

    private fun toDisplay(audio: AudioState, profile: TuningProfile, locked: Int?): TunerDisplay = when (audio) {
        AudioState.NeedsPermission -> TunerDisplay.NeedsPermission
        AudioState.Blocked -> TunerDisplay.PermissionBlocked
        AudioState.Starting -> TunerDisplay.Starting
        is AudioState.Failed ->
            if (audio.error is AudioCaptureException.PermissionMissing) TunerDisplay.NeedsPermission
            else TunerDisplay.Error(audio.error.message ?: audio.error.toString())
        is AudioState.Estimate -> when (val reading = analyzePitch(audio.estimate, profile, locked)) {
            TunerReading.Silence -> TunerDisplay.Listening
            TunerReading.WeakSignal -> TunerDisplay.WeakSignal
            is TunerReading.OutOfRange -> TunerDisplay.OutOfRange(reading.frequencyHz)
            is TunerReading.Detected -> TunerDisplay.Reading(reading)
        }
    }

    // --- Permiso -------------------------------------------------------------------------

    /** Chequeo al volver a la pantalla: detecta permisos concedidos desde Ajustes o revocados. */
    fun onPermissionChecked(granted: Boolean) {
        permission.update { current ->
            when {
                granted -> MicPermission.GRANTED
                current == MicPermission.GRANTED -> MicPermission.DENIED
                else -> current
            }
        }
    }

    fun onPermissionResult(result: MicPermission) {
        permission.value = result
    }

    fun onRetry() {
        retry.update { it + 1 }
    }

    // --- Cuerdas ---------------------------------------------------------------------------

    /** Fuera de edición fija/libera la cuerda objetivo; en edición abre el selector de nota. */
    fun onStringTapped(index: Int) {
        local.update { s ->
            if (s.editing && mode == TunerMode.PRO) s.copy(picker = index)
            else s.copy(locked = if (s.locked == index) null else index)
        }
    }

    fun onPickerDismissed() = local.update { it.copy(picker = null) }

    fun onPitchPicked(pitch: Pitch) {
        val index = local.value.picker ?: return
        local.update { it.copy(picker = null) }
        launchEdit(TunerMessage.INVALID_NOTE) { editProProfile.changeString(index, pitch) }
    }

    // --- Modo Pro --------------------------------------------------------------------------

    fun onToggleEditing() {
        if (mode != TunerMode.PRO) return
        local.update { it.copy(editing = !it.editing, picker = null) }
    }

    fun onPresetSelected(preset: TuningProfile) {
        if (mode != TunerMode.PRO) return
        local.update { it.copy(locked = null) }
        viewModelScope.launch { editProProfile.selectPreset(preset) }
    }

    fun onAddString() {
        local.update { it.copy(locked = null) }
        launchEdit(TunerMessage.MAX_STRINGS) { editProProfile.addString() }
    }

    fun onRemoveString() {
        local.update { it.copy(locked = null) }
        launchEdit(TunerMessage.MIN_STRINGS) { editProProfile.removeString() }
    }

    fun onReferenceChanged(referenceHz: Double) =
        launchEdit(TunerMessage.INVALID_REFERENCE) { editProProfile.setReference(referenceHz) }

    fun onMessageShown() = local.update { it.copy(message = null) }

    private fun launchEdit(onFailure: TunerMessage, action: suspend () -> Result<TuningProfile>) {
        if (mode != TunerMode.PRO) return
        viewModelScope.launch {
            action().onFailure { local.update { it.copy(message = onFailure) } }
        }
    }

    private companion object {
        /** Margen para no cerrar y reabrir el micrófono en una rotación de pantalla. */
        const val STOP_TIMEOUT_MILLIS = 1_000L
    }
}
