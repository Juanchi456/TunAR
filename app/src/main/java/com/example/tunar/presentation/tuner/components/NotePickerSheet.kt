package com.example.tunar.presentation.tuner.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tunar.R
import com.example.tunar.domain.model.NoteName
import com.example.tunar.domain.model.Pitch

/**
 * Selector de nota y octava para una cuerda (flujo del diseño en Figma: nota → octava → confirmar).
 * Solo habilita las combinaciones dentro del rango soportado (B0 a E5).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotePickerSheet(
    stringNumber: Int,
    current: Pitch,
    onConfirm: (Pitch) -> Unit,
    onDismiss: () -> Unit,
) {
    var note by remember(current) { mutableStateOf(current.note) }
    var octave by remember(current) { mutableStateOf(current.octave) }
    val candidate = Pitch(note, octave)
    val octaves = Pitch.LOWEST_SUPPORTED.octave..Pitch.HIGHEST_SUPPORTED.octave

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                stringResource(R.string.picker_title, stringNumber),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.picker_note), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NoteName.entries.forEach { n ->
                    FilterChip(
                        selected = n == note,
                        onClick = {
                            note = n
                            // Si la octava elegida queda fuera de rango, se ajusta a la más cercana válida.
                            if (!Pitch.isSupported(Pitch(n, octave))) {
                                octave = octaves.minBy { o ->
                                    if (Pitch.isSupported(Pitch(n, o))) kotlin.math.abs(o - octave) else Int.MAX_VALUE
                                }
                            }
                        },
                        label = { Text(n.symbol, style = MaterialTheme.typography.titleMedium) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.picker_octave), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                octaves.forEach { o ->
                    FilterChip(
                        selected = o == octave,
                        enabled = Pitch.isSupported(Pitch(note, o)),
                        onClick = { octave = o },
                        label = { Text(o.toString(), style = MaterialTheme.typography.titleMedium) },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onConfirm(candidate) },
                    enabled = Pitch.isSupported(candidate),
                ) { Text(stringResource(R.string.picker_confirm, candidate.label)) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
