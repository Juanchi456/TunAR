package com.example.tunar.presentation.tuner.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tunar.R
import com.example.tunar.domain.model.TunerReading
import com.example.tunar.domain.model.TuningStatus
import com.example.tunar.presentation.theme.CentsTextStyle
import com.example.tunar.presentation.theme.LocalTunerColors
import com.example.tunar.presentation.theme.NoteTextStyle
import com.example.tunar.presentation.theme.OctaveTextStyle
import com.example.tunar.presentation.tuner.TunerDisplay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Panel central: muestra la lectura o el estado correspondiente (permiso, carga, error…). */
@Composable
fun TunerPanel(
    display: TunerDisplay,
    stringCount: Int,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (display) {
            TunerDisplay.NeedsPermission -> StateMessage(
                icon = Icons.Filled.MicOff,
                title = stringResource(R.string.permission_title),
                body = stringResource(R.string.permission_body),
                action = stringResource(R.string.permission_action) to onRequestPermission,
            )

            TunerDisplay.PermissionBlocked -> StateMessage(
                icon = Icons.Filled.MicOff,
                title = stringResource(R.string.permission_blocked_title),
                body = stringResource(R.string.permission_blocked_body),
                action = stringResource(R.string.permission_open_settings) to onOpenSettings,
            )

            is TunerDisplay.Error -> StateMessage(
                icon = Icons.Filled.ErrorOutline,
                title = stringResource(R.string.mic_error_title),
                body = display.message,
                action = stringResource(R.string.retry) to onRetry,
                tint = MaterialTheme.colorScheme.error,
            )

            TunerDisplay.Starting -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.starting_mic), style = MaterialTheme.typography.titleMedium)
            }

            TunerDisplay.Listening -> IdleReading(
                icon = Icons.Filled.GraphicEq,
                text = stringResource(R.string.listening),
            )

            TunerDisplay.WeakSignal -> IdleReading(
                icon = Icons.Filled.VolumeDown,
                text = stringResource(R.string.weak_signal),
            )

            is TunerDisplay.OutOfRange -> IdleReading(
                icon = Icons.Filled.ErrorOutline,
                text = stringResource(R.string.out_of_range, formatHz(display.frequencyHz)),
            )

            is TunerDisplay.Reading -> DetectedReading(display.reading, stringCount)
        }
    }
}

@Composable
private fun DetectedReading(reading: TunerReading.Detected, stringCount: Int) {
    val colors = LocalTunerColors.current
    val cents = reading.centsFromTarget
    val color = when {
        reading.status == TuningStatus.IN_TUNE -> colors.inTune
        abs(cents) <= CLOSE_CENTS -> colors.close
        else -> colors.off
    }
    val roundedCents = cents.roundToInt()
    val statusText = when (reading.status) {
        TuningStatus.IN_TUNE -> stringResource(R.string.status_in_tune)
        TuningStatus.FLAT -> stringResource(R.string.status_flat)
        TuningStatus.SHARP -> stringResource(R.string.status_sharp)
    }
    val stringNumber = stringCount - reading.targetStringIndex

    Row(verticalAlignment = Alignment.Bottom) {
        Text(reading.detectedPitch.note.symbol, style = NoteTextStyle, color = color)
        Text(
            reading.detectedPitch.octave.toString(),
            style = OctaveTextStyle,
            color = color,
            modifier = Modifier.padding(bottom = 14.dp),
        )
    }
    Text(
        text = stringResource(
            R.string.target_line, stringNumber, reading.targetPitch.label, formatHz(reading.targetFrequencyHz),
        ),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(20.dp))
    TuningMeter(
        cents = cents,
        needleColor = color,
        description = stringResource(R.string.meter_description, signed(roundedCents), statusText),
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.cents_value, signed(roundedCents)),
        style = CentsTextStyle,
        color = color,
    )
    Spacer(Modifier.height(4.dp))
    StatusLine(reading.status, statusText, color)
    Text(
        text = stringResource(R.string.detected_hz, formatHz(reading.frequencyHz)),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StatusLine(status: TuningStatus, text: String, color: Color) {
    val icon = when (status) {
        TuningStatus.IN_TUNE -> Icons.Filled.CheckCircle
        TuningStatus.FLAT -> Icons.AutoMirrored.Filled.ArrowForward // subir: aguja hacia la derecha
        TuningStatus.SHARP -> Icons.AutoMirrored.Filled.ArrowBack
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .heightIn(min = 40.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        Spacer(Modifier.size(8.dp))
        Text(text, style = MaterialTheme.typography.titleLarge, color = color)
    }
}

@Composable
private fun IdleReading(icon: ImageVector, text: String) {
    Text("–", style = NoteTextStyle, color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(20.dp))
    TuningMeter(cents = null, needleColor = MaterialTheme.colorScheme.outline, description = text)
    Spacer(Modifier.height(20.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun StateMessage(
    icon: ImageVector,
    title: String,
    body: String,
    action: Pair<String, () -> Unit>,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(56.dp))
    Spacer(Modifier.height(16.dp))
    Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(
        body,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    Spacer(Modifier.height(24.dp))
    Button(onClick = action.second, modifier = Modifier.heightIn(min = 48.dp)) { Text(action.first) }
}

/** Fuera de la tolerancia de afinado pero cerca: color intermedio. */
private const val CLOSE_CENTS = 15.0

private fun signed(value: Int) = if (value > 0) "+$value" else value.toString()

fun formatHz(hz: Double): String = String.format(Locale.getDefault(), "%.2f", hz)
