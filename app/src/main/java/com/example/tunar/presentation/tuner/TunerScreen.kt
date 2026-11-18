package com.example.tunar.presentation.tuner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tunar.R
import com.example.tunar.domain.model.TuningProfile
import com.example.tunar.presentation.common.KeepScreenOn
import com.example.tunar.presentation.common.rememberMicPermission
import com.example.tunar.presentation.tuner.components.EditPanel
import com.example.tunar.presentation.tuner.components.NotePickerSheet
import com.example.tunar.presentation.tuner.components.PresetSelector
import com.example.tunar.presentation.tuner.components.StringSelector
import com.example.tunar.presentation.tuner.components.TunerPanel

/** Pantalla del afinador conectada a su ViewModel. */
@Composable
fun TunerRoute(
    viewModel: TunerViewModel,
    onOpenCalibration: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permission = rememberMicPermission(
        onChecked = viewModel::onPermissionChecked,
        onRequestResult = viewModel::onPermissionResult,
    )
    KeepScreenOn()
    TunerScreen(
        state = state,
        actions = TunerActions(
            onRequestPermission = permission.request,
            onOpenSettings = permission.openSettings,
            onRetry = viewModel::onRetry,
            onStringTapped = viewModel::onStringTapped,
            onToggleEditing = viewModel::onToggleEditing,
            onPresetSelected = viewModel::onPresetSelected,
            onReferenceChanged = viewModel::onReferenceChanged,
            onAddString = viewModel::onAddString,
            onRemoveString = viewModel::onRemoveString,
            onPitchPicked = viewModel::onPitchPicked,
            onPickerDismissed = viewModel::onPickerDismissed,
            onMessageShown = viewModel::onMessageShown,
            onOpenCalibration = onOpenCalibration,
        ),
    )
}

class TunerActions(
    val onRequestPermission: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onStringTapped: (Int) -> Unit = {},
    val onToggleEditing: () -> Unit = {},
    val onPresetSelected: (TuningProfile) -> Unit = {},
    val onReferenceChanged: (Double) -> Unit = {},
    val onAddString: () -> Unit = {},
    val onRemoveString: () -> Unit = {},
    val onPitchPicked: (com.example.tunar.domain.model.Pitch) -> Unit = {},
    val onPickerDismissed: () -> Unit = {},
    val onMessageShown: () -> Unit = {},
    val onOpenCalibration: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunerScreen(state: TunerUiState, actions: TunerActions) {
    val snackbar = remember { SnackbarHostState() }
    val messageText = state.message?.let { messageFor(it) }
    LaunchedEffect(state.message) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            actions.onMessageShown()
        }
    }

    Scaffold(
        // La barra inferior de navegación ya consume los insets de abajo.
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.mode == TunerMode.BASIC) R.string.title_basic else R.string.title_pro,
                        ),
                    )
                },
                actions = {
                    if (state.mode == TunerMode.PRO) {
                        IconButton(onClick = actions.onOpenCalibration) {
                            Icon(Icons.Outlined.Science, stringResource(R.string.open_calibration))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.mode == TunerMode.PRO) {
                PresetSelector(
                    presets = state.presets,
                    matchingPreset = state.matchingPreset,
                    onPresetSelected = actions.onPresetSelected,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    stringResource(R.string.basic_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            TunerPanel(
                display = state.display,
                stringCount = state.profile.strings.size,
                onRequestPermission = actions.onRequestPermission,
                onOpenSettings = actions.onOpenSettings,
                onRetry = actions.onRetry,
                modifier = Modifier
                    .heightIn(min = if (state.isEditing) 300.dp else 400.dp)
                    .padding(vertical = 8.dp),
            )

            StringSelector(
                strings = state.profile.strings,
                activeIndex = state.activeStringIndex,
                lockedIndex = state.lockedStringIndex,
                isEditing = state.isEditing,
                onStringTapped = actions.onStringTapped,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    when {
                        state.isEditing -> R.string.hint_editing
                        state.lockedStringIndex != null -> R.string.hint_locked
                        else -> R.string.hint_auto
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.canEdit) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    FilledTonalButton(onClick = actions.onToggleEditing, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(if (state.isEditing) Icons.Filled.Check else Icons.Filled.Edit, contentDescription = null)
                        Spacer(Modifier.padding(4.dp))
                        Text(stringResource(if (state.isEditing) R.string.edit_done else R.string.edit_start))
                    }
                }
                if (state.isEditing) {
                    Spacer(Modifier.height(12.dp))
                    EditPanel(
                        profile = state.profile,
                        onReferenceChanged = actions.onReferenceChanged,
                        onAddString = actions.onAddString,
                        onRemoveString = actions.onRemoveString,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    state.pickerStringIndex?.let { index ->
        NotePickerSheet(
            stringNumber = state.profile.strings.size - index,
            current = state.profile.strings[index].pitch,
            onConfirm = actions.onPitchPicked,
            onDismiss = actions.onPickerDismissed,
        )
    }
}

@Composable
private fun messageFor(message: TunerMessage): String = stringResource(
    when (message) {
        TunerMessage.MAX_STRINGS -> R.string.msg_max_strings
        TunerMessage.MIN_STRINGS -> R.string.msg_min_strings
        TunerMessage.INVALID_REFERENCE -> R.string.msg_invalid_reference
        TunerMessage.INVALID_NOTE -> R.string.msg_invalid_note
    },
)
