package com.example.tunar.presentation

import com.example.tunar.data.repository.InMemoryDetectionConfigRepository
import com.example.tunar.data.repository.InMemoryTuningProfileRepository
import com.example.tunar.domain.model.AudioCaptureException
import com.example.tunar.domain.model.BuiltInPresets
import com.example.tunar.domain.model.DetectionConfig
import com.example.tunar.domain.model.NoteName
import com.example.tunar.domain.model.Pitch
import com.example.tunar.domain.model.PitchSample
import com.example.tunar.domain.model.SampleRateMeasurement
import com.example.tunar.domain.model.TuningStatus
import com.example.tunar.domain.music.TuningMath
import com.example.tunar.domain.repository.AudioRepository
import com.example.tunar.domain.usecase.AnalyzePitchUseCase
import com.example.tunar.domain.usecase.EditProProfileUseCase
import com.example.tunar.domain.usecase.ObservePitchUseCase
import com.example.tunar.presentation.common.MicPermission
import com.example.tunar.presentation.tuner.TunerDisplay
import com.example.tunar.presentation.tuner.TunerMode
import com.example.tunar.presentation.tuner.TunerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TunerViewModelTest {

    /** Micrófono falso: emite lo que el test le manda. */
    private class FakeAudio : AudioRepository {
        val samples = MutableSharedFlow<PitchSample>()
        var failWith: Throwable? = null
        override fun pitchSamples(config: DetectionConfig): Flow<PitchSample> = flow {
            failWith?.let { throw it }
            samples.collect { emit(it) }
        }
        override suspend fun measureSampleRate(nominalRate: Int, durationMillis: Long) =
            SampleRateMeasurement(nominalRate, nominalRate.toDouble(), durationMillis, "fake")
    }

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var audio: FakeAudio
    private lateinit var profiles: InMemoryTuningProfileRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        audio = FakeAudio()
        profiles = InMemoryTuningProfileRepository()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(mode: TunerMode) = TunerViewModel(
        mode = mode,
        observePitch = ObservePitchUseCase(audio),
        analyzePitch = AnalyzePitchUseCase(),
        editProProfile = EditProProfileUseCase(profiles),
        profileRepository = profiles,
        detectionConfigRepository = InMemoryDetectionConfigRepository(),
    )

    private fun pitched(hz: Double) = PitchSample(hz, 0.95, -20.0, 0L, 0L)

    @Test
    fun `sin permiso no abre el microfono y al concederlo muestra la lectura`() = runTest(dispatcher) {
        val vm = viewModel(TunerMode.BASIC)
        backgroundScope.launch { vm.uiState.collect {} }
        assertEquals(TunerDisplay.NeedsPermission, vm.uiState.value.display)

        vm.onPermissionResult(MicPermission.GRANTED)
        assertEquals(TunerDisplay.Starting, vm.uiState.value.display)

        val e2 = TuningMath.frequencyOf(Pitch(NoteName.E, 2), 440.0)
        audio.samples.emit(pitched(TuningMath.applyCents(e2, -20.0)))
        audio.samples.emit(pitched(TuningMath.applyCents(e2, -20.0)))
        val reading = (vm.uiState.value.display as TunerDisplay.Reading).reading
        assertEquals(0, reading.targetStringIndex)
        assertEquals(TuningStatus.FLAT, reading.status)
        assertEquals(-20.0, reading.centsFromTarget, 1e-6)
    }

    @Test
    fun `rechazo permanente y error de microfono`() = runTest(dispatcher) {
        val vm = viewModel(TunerMode.BASIC)
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onPermissionResult(MicPermission.PERMANENTLY_DENIED)
        assertEquals(TunerDisplay.PermissionBlocked, vm.uiState.value.display)

        audio.failWith = AudioCaptureException.Unavailable("ocupado")
        vm.onPermissionChecked(granted = true)
        assertTrue(vm.uiState.value.display is TunerDisplay.Error)

        audio.failWith = null
        vm.onRetry()
        assertEquals(TunerDisplay.Starting, vm.uiState.value.display)
    }

    @Test
    fun `modo Pro edita el perfil y el Basico queda en estandar`() = runTest(dispatcher) {
        val pro = viewModel(TunerMode.PRO)
        val basic = viewModel(TunerMode.BASIC)
        backgroundScope.launch { pro.uiState.collect {} }
        backgroundScope.launch { basic.uiState.collect {} }

        pro.onToggleEditing()
        pro.onAddString()
        pro.onStringTapped(0)
        pro.onPitchPicked(Pitch(NoteName.A, 1))
        assertEquals(7, pro.uiState.value.profile.strings.size)
        assertEquals("A1", pro.uiState.value.profile.strings.first().pitch.label)
        assertEquals(null, pro.uiState.value.matchingPreset)

        // Básico ignora la edición y sigue en estándar.
        basic.onAddString()
        assertEquals(BuiltInPresets.STANDARD.strings, basic.uiState.value.profile.strings)

        // RF03: un nuevo ViewModel Pro (volver al modo) recupera el último perfil.
        val proAgain = viewModel(TunerMode.PRO)
        backgroundScope.launch { proAgain.uiState.collect {} }
        assertEquals(7, proAgain.uiState.value.profile.strings.size)

        pro.onPresetSelected(BuiltInPresets.DROP_D)
        assertEquals(BuiltInPresets.DROP_D.id, pro.uiState.value.matchingPreset?.id)
    }

    @Test
    fun `fijar una cuerda cambia el objetivo`() = runTest(dispatcher) {
        val vm = viewModel(TunerMode.BASIC)
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onPermissionResult(MicPermission.GRANTED)
        vm.onStringTapped(1) // A2
        val e2 = TuningMath.frequencyOf(Pitch(NoteName.E, 2), 440.0)
        repeat(2) { audio.samples.emit(pitched(e2)) }
        val reading = (vm.uiState.value.display as TunerDisplay.Reading).reading
        assertEquals(1, reading.targetStringIndex)
        assertEquals(-500.0, reading.centsFromTarget, 1e-6)
    }
}
