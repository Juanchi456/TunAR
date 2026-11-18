package com.example.tunar.presentation.common

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Estado del permiso de micrófono tal como lo necesita la UI. */
enum class MicPermission {
    /** Todavía no se pidió (o no se sabe): se muestra la explicación y el botón para pedirlo. */
    UNKNOWN,
    GRANTED,

    /** El usuario lo rechazó, pero se puede volver a pedir. */
    DENIED,

    /** Rechazado con "no volver a preguntar": solo se puede habilitar desde Ajustes. */
    PERMANENTLY_DENIED,
}

class MicPermissionActions(val request: () -> Unit, val openSettings: () -> Unit)

/**
 * Conecta el permiso de micrófono del sistema con el ViewModel.
 *
 * - Al volver a la pantalla (ON_RESUME) informa si el permiso está concedido: cubre el caso en
 *   que el usuario lo habilitó desde Ajustes o lo revocó con la app en segundo plano.
 * - El resultado del diálogo distingue un rechazo simple de uno permanente.
 */
@Composable
fun rememberMicPermission(
    onChecked: (granted: Boolean) -> Unit,
    onRequestResult: (MicPermission) -> Unit,
): MicPermissionActions {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val currentOnChecked by rememberUpdatedState(onChecked)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val result = when {
            granted -> MicPermission.GRANTED
            activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.RECORD_AUDIO,
            ) -> MicPermission.DENIED
            else -> MicPermission.PERMANENTLY_DENIED
        }
        onRequestResult(result)
    }

    LifecycleResumeEffect(Unit) {
        currentOnChecked(context.hasMicPermission())
        onPauseOrDispose { }
    }

    return MicPermissionActions(
        request = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
        openSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        },
    )
}

fun Context.hasMicPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

/** Mantiene la pantalla encendida mientras el afinador está visible (el teléfono suele estar apoyado). */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
