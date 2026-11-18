package com.example.tunar.presentation.tuner.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tunar.R
import com.example.tunar.domain.model.TuningProfile
import java.util.Locale

/** Selector de preset (Modo Pro). Muestra "Personalizado" si el perfil no coincide con ninguno. */
@Composable
fun PresetSelector(
    presets: List<TuningProfile>,
    matchingPreset: TuningProfile?,
    onPresetSelected: (TuningProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.preset_label,
                    matchingPreset?.name ?: stringResource(R.string.preset_custom),
                ),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.preset_choose))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            presets.forEach { preset ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(preset.name)
                            Text(
                                preset.strings.joinToString(" ") { it.pitch.label },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    leadingIcon = {
                        if (preset.id == matchingPreset?.id) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        onPresetSelected(preset)
                    },
                )
            }
        }
    }
}

/** Panel de edición del Modo Pro: frecuencia de referencia y cantidad de cuerdas. */
@Composable
fun EditPanel(
    profile: TuningProfile,
    onReferenceChanged: (Double) -> Unit,
    onAddString: () -> Unit,
    onRemoveString: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.reference_title), style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = { onReferenceChanged(profile.referenceA4Hz - 1) }) {
                    Icon(Icons.Filled.Remove, stringResource(R.string.reference_decrease))
                }
                Text(
                    text = stringResource(R.string.reference_value, formatReference(profile.referenceA4Hz)),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(120.dp),
                )
                FilledTonalIconButton(onClick = { onReferenceChanged(profile.referenceA4Hz + 1) }) {
                    Icon(Icons.Filled.Add, stringResource(R.string.reference_increase))
                }
                Spacer(Modifier.weight(1f))
                listOf(440.0, 432.0).forEach { hz ->
                    AssistChip(
                        onClick = { onReferenceChanged(hz) },
                        label = { Text(formatReference(hz)) },
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
            Spacer(Modifier.padding(4.dp))
            Text(stringResource(R.string.strings_title), style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
                FilledTonalIconButton(onClick = onRemoveString, enabled = profile.canRemoveString) {
                    Icon(Icons.Filled.Remove, stringResource(R.string.strings_remove))
                }
                Text(
                    text = stringResource(R.string.strings_count, profile.strings.size),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(120.dp),
                )
                FilledTonalIconButton(onClick = onAddString, enabled = profile.canAddString) {
                    Icon(Icons.Filled.Add, stringResource(R.string.strings_add))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.strings_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatReference(hz: Double): String =
    if (hz % 1.0 == 0.0) hz.toInt().toString() else String.format(Locale.getDefault(), "%.1f", hz)
