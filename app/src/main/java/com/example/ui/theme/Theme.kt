package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldAlpha,
    onPrimary = Color(0xFF00241A),
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = Color(0xFF7CF7D0),
    secondary = SovereignGold,
    onSecondary = Color(0xFF271B00),
    secondaryContainer = SovereignGoldContainerDark,
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = ElectricCyan,
    onTertiary = Color(0xFF002233),
    background = ObsidianBg,
    onBackground = IvoryText,
    surface = ObsidianSurface,
    onSurface = IvoryText,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = SlateTextMuted,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceHighlight,
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF1C2B42),
    error = CrimsonVolatility,
    onError = Color(0xFF3A060B),
    errorContainer = CrimsonContainerDark,
    onErrorContainer = Color(0xFFFFB4B9)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldLightPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldLightContainer,
    onPrimaryContainer = Color(0xFF022C22),
    secondary = GoldLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = GoldLightContainer,
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = AlabasterBg,
    onBackground = MidnightInk,
    surface = AlabasterSurface,
    onSurface = MidnightInk,
    surfaceVariant = AlabasterSurfaceElevated,
    onSurfaceVariant = SlateInkMuted,
    surfaceContainerLow = AlabasterSurface,
    surfaceContainer = AlabasterSurfaceElevated,
    surfaceContainerHigh = Color(0xFFE2E8F0),
    outline = AlabasterOutline,
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

val FinAdvisorShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FinAdvisorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FinAdvisorTypography,
        shapes = FinAdvisorShapes,
        content = content
    )
}
