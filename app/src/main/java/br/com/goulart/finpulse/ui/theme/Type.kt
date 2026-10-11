package br.com.goulart.finpulse.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun type(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = height.sp,
    letterSpacing = 0.sp,
)

val FinPulseTypography = Typography(
    displaySmall = type(38, 46, FontWeight.Bold).copy(letterSpacing = (-1.5).sp),
    headlineLarge = type(30, 38, FontWeight.Bold).copy(letterSpacing = (-0.8).sp),
    headlineSmall = type(23, 30, FontWeight.SemiBold).copy(letterSpacing = (-0.5).sp),
    titleLarge = type(22, 28, FontWeight.Bold),
    titleMedium = type(16, 24, FontWeight.SemiBold),
    titleSmall = type(14, 20, FontWeight.SemiBold),
    bodyLarge = type(16, 24),
    bodyMedium = type(14, 20),
    bodySmall = type(12, 18),
    labelLarge = type(14, 20, FontWeight.Medium),
    labelMedium = type(12, 16, FontWeight.Medium),
    labelSmall = type(11, 16, FontWeight.Medium),
)
