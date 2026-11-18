package com.example.tunar.presentation.calibration

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tunar.domain.calibration.ReadingKind
import com.example.tunar.domain.model.DetectionConfig
import com.example.tunar.domain.model.PitchAlgorithm
import com.example.tunar.domain.music.TuningMath
import com.example.tunar.presentation.common.KeepScreenOn
import com.example.tunar.presentation.common.rememberMicPermission
import java.util.Locale

/*
 * Pantalla técnica para el banco de calibración. Sus textos no se traducen: es una
 * herramienta de medición para el desarrollo, accesible desde el Modo Pro.
 */

@Composable
fun CalibrationRoute(viewModel: CalibrationViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permission = rememberMicPermission(
        onChecked = viewModel::onPermissionChecked,
        onRequestResult = { viewModel.onPermissionChecked(it == com.example.tunar.presentation.common.MicPermission.GRANTED) },
    )
    // Privacidad: no se sigue capturando con la app en segundo plano.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.stop() }
    KeepScreenOn()
    val context = LocalContext.current
    CalibrationScreen(
        state = state,
        onBack = onBack,
        onRequestPermission = permission.request,
        onStart = viewModel::start,
        onStop = viewModel::stop,
        onReset = viewModel::resetStats,
        onExpectedChanged = viewModel::onExpectedPitchChanged,
        onConfigChanged = viewModel::onConfigChanged,
        onMeasureRate = viewModel::measureSampleRate,
        onApplyCorrection = viewModel::applySampleRateCorrection,
        onClearCorrection = viewModel::clearSampleRateCorrection,
        onCopyReport = { copyToClipboard(context, viewModel.buildReport()) },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalibrationScreen(
    state: CalibrationUiState,
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onExpectedChanged: (Int) -> Unit,
    onConfigChanged: (DetectionConfig) -> Unit,
    onMeasureRate: () -> Unit,
    onApplyCorrection: () -> Unit,
    onClearCorrection: () -> Unit,
    onCopyReport: () -> Unit,
) {
    val c = state.config
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calibración") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                },
                actions = {
                    IconButton(onClick = onCopyReport) { Icon(Icons.Filled.ContentCopy, "Copiar informe") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!state.permissionGranted) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Se necesita el permiso de micrófono para medir.")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onRequestPermission) { Text("Permitir micrófono") }
                    }
                }
            }
            state.error?.let {
                Text("Error: $it", color = MaterialTheme.colorScheme.error)
            }

            // --- Fuente conocida y lectura en vivo ---
            Section("Fuente de referencia") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { onExpectedChanged(-1) }) { Icon(Icons.Filled.Remove, "Semitono abajo") }
                    Text(
                        "${state.expectedPitch}  ·  ${fmt(TuningMath.frequencyOf(state.expectedPitch, 440.0))} Hz",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    FilledTonalIconButton(onClick = { onExpectedChanged(1) }) { Icon(Icons.Filled.Add, "Semitono arriba") }
                }
                Text(
                    "Hacé sonar esta nota con un generador de tonos o una cuerda afinada (referencia A4 = 440 Hz).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.running) {
                        Button(onClick = onStop) { Icon(Icons.Filled.Stop, null); Text(" Detener") }
                    } else {
                        Button(onClick = onStart, enabled = state.permissionGranted && !state.measuringSampleRate) {
                            Icon(Icons.Filled.PlayArrow, null); Text(" Medir")
                        }
                    }
                    OutlinedButton(onClick = onReset) { Text("Reiniciar estadísticas") }
                }
                Spacer(Modifier.height(8.dp))
                val live = when (state.liveSignal) {
                    LiveSignal.IDLE -> "Detenido"
                    LiveSignal.SILENCE -> "Silencio"
                    LiveSignal.UNCLEAR -> "Señal poco clara"
                    LiveSignal.PITCHED -> buildString {
                        append("${fmt(state.liveFrequencyHz)} Hz · ${signedFmt(state.liveCentsFromExpected)} ¢")
                        when (state.liveKind) {
                            ReadingKind.OCTAVE_ERROR -> append(" · ERROR DE OCTAVA")
                            ReadingKind.OTHER_NOTE -> append(" · otra nota")
                            else -> Unit
                        }
                    }
                }
                Text(live, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace)
                state.liveLevelDb?.let { Text("Nivel: ${fmt(it, 0)} dBFS", style = MaterialTheme.typography.bodySmall) }
            }

            // --- Resultados ---
            Section("Resultados") {
                val s = state.summary
                Metric("Lecturas (nota correcta)", "${s.readings} (${s.correctReadings})")
                Metric("Error medio", "${signedFmt(s.meanErrorCents)} ¢")
                Metric("Desvío estándar (total / último s)", "${fmt(s.stdDevCents)} / ${fmt(s.recentStdDevCents)} ¢")
                Metric("Dentro de ±2 / ±5 / ±8 ¢", "${fmt(s.within2CentsPct, 0)} / ${fmt(s.within5CentsPct, 0)} / ${fmt(s.within8CentsPct, 0)} %")
                Metric("Errores de octava / otra nota", "${fmt(s.octaveErrorPct, 1)} / ${fmt(s.otherNotePct, 1)} %")
                Metric("Proceso p95 (hop = ${fmt(c.hopMillis, 1)} ms)", "${fmt(s.processingP95Millis)} ms")
                Metric("Pipeline p95", "${fmt(s.pipelineP95Millis)} ms")
                Metric("Latencia estimada p95", "${fmt(state.estimatedLatencyP95Millis, 0)} ms")
                if ((s.processingP95Millis ?: 0.0) > c.hopMillis) {
                    Text(
                        "El análisis tarda más que un hop: el audio se acumula. Probá FastYin o un hop mayor.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            // --- Tasa de muestreo ---
            Section("Tasa de muestreo real") {
                if (state.measuringSampleRate) {
                    Text("Midiendo durante 10 s…")
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                } else {
                    state.sampleRateMeasurement?.let {
                        Metric("Medida (${it.method})", "${fmt(it.measuredRate, 1)} Hz")
                        Metric("Factor / error sin corregir", "×${fmt(it.correctionFactor, 6)} / ${signedFmt(it.errorCents)} ¢")
                    }
                    Metric("Corrección aplicada", "×${fmt(c.sampleRateCorrection, 6)}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onMeasureRate, enabled = state.permissionGranted) { Text("Medir") }
                        TextButton(
                            onClick = onApplyCorrection,
                            enabled = state.sampleRateMeasurement?.nominalRate == c.sampleRate,
                        ) { Text("Aplicar") }
                        TextButton(onClick = onClearCorrection, enabled = c.sampleRateCorrection != 1.0) { Text("Quitar") }
                    }
                }
            }

            // --- Parámetros ---
            Section("Parámetros de detección") {
                Label("Algoritmo")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PitchAlgorithm.entries.forEach { a ->
                        FilterChip(
                            selected = a == c.algorithm,
                            onClick = { onConfigChanged(c.copy(algorithm = a, algorithmThreshold = a.defaultThreshold)) },
                            label = { Text(a.label) },
                        )
                    }
                }
                Label("Tasa de muestreo nominal")
                ChipRow(listOf(44_100, 48_000), c.sampleRate, { "$it Hz" }) {
                    onConfigChanged(c.copy(sampleRate = it, sampleRateCorrection = 1.0))
                }
                Label("Buffer (ventana ${fmt(c.windowMillis, 0)} ms · mínimo medible ${fmt(c.lowestMeasurableHz, 1)} Hz)")
                ChipRow(listOf(2048, 4096, 8192), c.bufferSize, { it.toString() }) {
                    onConfigChanged(c.copy(bufferSize = it, hopSize = minOf(c.hopSize, it)))
                }
                Label("Hop (lectura cada ${fmt(c.hopMillis, 1)} ms)")
                ChipRow(listOf(512, 1024, 2048), c.hopSize, { it.toString() }) {
                    onConfigChanged(c.copy(hopSize = minOf(it, c.bufferSize)))
                }
                val thresholdRange = if (c.algorithm == PitchAlgorithm.MPM) 0.80f..0.99f else 0.05f..0.40f
                LabeledSlider("Umbral del algoritmo", c.algorithmThreshold, thresholdRange) {
                    onConfigChanged(c.copy(algorithmThreshold = it))
                }
                LabeledSlider("Probabilidad mínima", c.minProbability, 0.5f..0.99f) {
                    onConfigChanged(c.copy(minProbability = it))
                }
                LabeledSlider("Umbral de silencio (dBFS)", c.silenceThresholdDb, -70f..-30f, decimals = 0) {
                    onConfigChanged(c.copy(silenceThresholdDb = it))
                }
                Text(
                    "Los cambios se aplican también al afinador.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun Label(text: String) =
    Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))

@Composable
private fun Metric(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(label(o)) })
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Double,
    range: ClosedFloatingPointRange<Float>,
    decimals: Int = 2,
    onChange: (Double) -> Unit,
) {
    // Valor local mientras se arrastra: la configuración (y el micrófono) se reinicia al soltar.
    var local by remember(value) { mutableFloatStateOf(value.toFloat().coerceIn(range)) }
    Label("$label: ${fmt(local.toDouble(), decimals)}")
    Slider(
        value = local,
        onValueChange = { local = it },
        onValueChangeFinished = { onChange(local.toDouble()) },
        valueRange = range,
    )
}

private fun fmt(v: Double?, decimals: Int = 2) =
    v?.let { String.format(Locale.ROOT, "%.${decimals}f", it) } ?: "—"

private fun signedFmt(v: Double?) = v?.let { (if (it > 0) "+" else "") + fmt(it) } ?: "—"

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText("TunAR calibración", text))
    Toast.makeText(context, "Informe copiado", Toast.LENGTH_SHORT).show()
}
