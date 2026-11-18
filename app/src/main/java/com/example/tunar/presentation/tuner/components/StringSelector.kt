package com.example.tunar.presentation.tuner.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tunar.R
import com.example.tunar.domain.model.StringConfig

/**
 * Fila de cuerdas, de la más grave (izquierda) a la más aguda (derecha).
 *
 * Resalta la cuerda que se está afinando. Al tocar una cuerda se fija como objetivo
 * (candado) o, en modo edición, se abre el selector de nota.
 */
@Composable
fun StringSelector(
    strings: List<StringConfig>,
    activeIndex: Int?,
    lockedIndex: Int?,
    isEditing: Boolean,
    onStringTapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        strings.forEachIndexed { index, string ->
            val active = index == activeIndex
            val locked = index == lockedIndex
            val number = strings.size - index
            val description = when {
                isEditing -> stringResource(R.string.string_edit_description, number, string.pitch.label)
                locked -> stringResource(R.string.string_locked_description, number, string.pitch.label)
                else -> stringResource(R.string.string_description, number, string.pitch.label)
            }
            OutlinedCard(
                onClick = { onStringTapped(index) },
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (active) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                ),
                border = BorderStroke(
                    width = if (active || locked) 2.dp else 1.dp,
                    color = if (active || locked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp)
                    .semantics {
                        contentDescription = description
                        role = Role.Button
                        selected = active
                    },
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                ) {
                    Text(
                        text = string.pitch.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                    )
                    when {
                        isEditing -> Icon(Icons.Filled.Edit, null, Modifier.size(16.dp))
                        locked -> Icon(Icons.Filled.Lock, null, Modifier.size(16.dp))
                        else -> Text(
                            text = number.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
