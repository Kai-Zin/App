package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueSoft,
    onPrimaryContainer = BrandBlueDark,
    secondary = ProfitGreen,
    onSecondary = Color.White,
    secondaryContainer = ProfitGreenSoft,
    onSecondaryContainer = ProfitGreen,
    tertiary = WarningAmber,
    onTertiary = Color.White,
    tertiaryContainer = WarningAmberSoft,
    onTertiaryContainer = WarningAmberText,
    error = LossRed,
    onError = Color.White,
    errorContainer = LossRedSoft,
    onErrorContainer = LossRed,
    background = SurfaceBg,
    onBackground = InkPrimary,
    surface = CardBg,
    onSurface = InkPrimary,
    surfaceVariant = Color(0xFFF7F9FC),
    onSurfaceVariant = MutedText,
    outline = LineBorder,
    outlineVariant = Color(0xFFDCE2EA)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkBlue,
    onPrimary = Color(0xFF0B1E40),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = DarkGreen,
    onSecondary = Color(0xFF062E20),
    secondaryContainer = Color(0xFF0F3D2E),
    onSecondaryContainer = DarkGreen,
    tertiary = WarningAmber,
    onTertiary = Color(0xFF3E2404),
    tertiaryContainer = Color(0xFF452B0A),
    onTertiaryContainer = Color(0xFFFCD34D),
    error = DarkRed,
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF4C1D24),
    onErrorContainer = DarkRed,
    background = DarkBg,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = DarkLine,
    outlineVariant = Color(0xFF475569)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
