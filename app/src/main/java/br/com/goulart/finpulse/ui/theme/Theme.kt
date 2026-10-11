package br.com.goulart.finpulse.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val FinPulseColors = lightColorScheme(
    primary = FinBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F2FF),
    onPrimaryContainer = FinBlue,
    background = FinBackground,
    onBackground = FinInk,
    surface = Color.White,
    onSurface = FinInk,
    surfaceVariant = Color(0xFFEBEBF0),
    onSurfaceVariant = FinSecondary,
    outline = FinSecondary,
    outlineVariant = FinDivider,
    error = FinNegative,
)

/** The approved light palette is independent of system and wallpaper colors. */
@Composable
fun FinPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FinPulseColors,
        typography = FinPulseTypography,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(24.dp),
        ),
        content = content,
    )
}
