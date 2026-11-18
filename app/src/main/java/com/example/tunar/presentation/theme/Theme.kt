package com.example.tunar.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    secondary = Amber80,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceContainer = Neutral12,
    surfaceContainerHigh = Neutral20,
)

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Amber40,
    background = Neutral99,
    onBackground = Neutral12,
    surface = Neutral99,
    onSurface = Neutral12,
    surfaceContainer = Neutral95,
)

/** Colores semánticos del afinador, distintos para tema claro y oscuro. */
@Immutable
data class TunerColors(val inTune: Color, val close: Color, val off: Color)

val LocalTunerColors = staticCompositionLocalOf {
    TunerColors(InTuneLight, CloseLight, OffLight)
}

/**
 * Tema de TunAR. No usa color dinámico (Material You) para que los colores de estado
 * del afinador mantengan siempre el mismo significado y contraste.
 */
@Composable
fun TunARTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val tunerColors = if (darkTheme) {
        TunerColors(InTuneDark, CloseDark, OffDark)
    } else {
        TunerColors(InTuneLight, CloseLight, OffLight)
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalTunerColors provides tunerColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
