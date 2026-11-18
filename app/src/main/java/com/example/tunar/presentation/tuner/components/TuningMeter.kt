package com.example.tunar.presentation.tuner.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tunar.domain.usecase.AnalyzePitchUseCase
import kotlin.math.abs

private const val RANGE_CENTS = 50f

/**
 * Indicador horizontal de -50 a +50 céntimos con la zona de afinado resaltada.
 *
 * @param cents desvío actual, o null si no hay lectura (la aguja se oculta).
 */
@Composable
fun TuningMeter(
    cents: Double?,
    needleColor: Color,
    description: String,
    modifier: Modifier = Modifier,
) {
    val target = (cents ?: 0.0).toFloat().coerceIn(-RANGE_CENTS, RANGE_CENTS)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "needle",
    )
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
    val zoneColor = needleColor.takeIf { cents != null } ?: MaterialTheme.colorScheme.outline
    val tolerance = AnalyzePitchUseCase.IN_TUNE_TOLERANCE_CENTS.toFloat()

    Column(modifier.semantics { contentDescription = description }) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(72.dp),
        ) {
            val centerY = size.height / 2
            fun x(c: Float) = size.width / 2 + c / RANGE_CENTS * (size.width / 2)

            // Zona de afinado
            drawRoundRect(
                color = zoneColor.copy(alpha = 0.18f),
                topLeft = Offset(x(-tolerance), 4.dp.toPx()),
                size = Size(x(tolerance) - x(-tolerance), size.height - 8.dp.toPx()),
                cornerRadius = CornerRadius(6.dp.toPx()),
            )
            // Escala
            drawLine(trackColor, Offset(0f, centerY), Offset(size.width, centerY), strokeWidth = 2.dp.toPx())
            for (c in -50..50 step 10) {
                val major = c == 0
                val half = if (major) 22.dp.toPx() else if (abs(c) == 50) 14.dp.toPx() else 9.dp.toPx()
                drawLine(
                    tickColor,
                    Offset(x(c.toFloat()), centerY - half),
                    Offset(x(c.toFloat()), centerY + half),
                    strokeWidth = (if (major) 3.dp else 1.5.dp).toPx(),
                    cap = StrokeCap.Round,
                )
            }
            // Aguja
            if (cents != null) {
                val nx = x(animated)
                drawLine(
                    needleColor,
                    Offset(nx, 2.dp.toPx()),
                    Offset(nx, size.height - 2.dp.toPx()),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                val tri = Path().apply {
                    moveTo(nx - 9.dp.toPx(), 0f)
                    lineTo(nx + 9.dp.toPx(), 0f)
                    lineTo(nx, 12.dp.toPx())
                    close()
                }
                drawPath(tri, needleColor)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("-50", "0", "+50").forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = when (it) {
                        "-50" -> TextAlign.Start
                        "0" -> TextAlign.Center
                        else -> TextAlign.End
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
