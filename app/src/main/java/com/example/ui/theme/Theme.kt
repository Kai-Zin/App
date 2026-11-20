package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalUiScale = staticCompositionLocalOf { 0.90f }

@Composable
fun scaledDp(base: Dp): Dp = base * LocalUiScale.current

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
    background = Color.White,
    onBackground = InkPrimary,
    surface = Color.White,
    onSurface = InkPrimary,
    surfaceVariant = SoftSurface,
    onSurfaceVariant = MutedText,
    outline = LineBorder,
    outlineVariant = Color(0xFFDCE2EA)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(9.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun MyApplicationTheme(
    uiScale: Float = 0.90f,
    content: @Composable () -> Unit
) {
    val safeScale = uiScale.coerceIn(0.75f, 1.25f)
    val baseDensity = LocalDensity.current
    val scaledDensity = Density(
        density = baseDensity.density * (0.94f + (safeScale - 0.90f) * 0.45f),
        fontScale = baseDensity.fontScale * safeScale
    )

    CompositionLocalProvider(
        LocalUiScale provides safeScale,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = buildAppTypography(1.0f),
            shapes = AppShapes,
            content = content
        )
    }
}
