package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    secondary = CyberCyan,
    tertiary = DeepOrchid,
    background = Obsidian,
    surface = Charcoal,
    surfaceVariant = CardBackground,
    outline = BorderColor,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = OffWhite,
    onSurface = OffWhite,
    onSurfaceVariant = TextGray,
    primaryContainer = ElectricViolet.copy(alpha = 0.2f),
    onPrimaryContainer = OffWhite,
    secondaryContainer = CyberCyan.copy(alpha = 0.2f),
    onSecondaryContainer = OffWhite,
    surfaceContainer = Charcoal,
    surfaceContainerLow = Obsidian,
    surfaceContainerHigh = CardBackground
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurface,
    outline = LightBorder,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = LightText,
    onSurface = LightText,
    onSurfaceVariant = LightTextMuted,
    primaryContainer = LightPrimary.copy(alpha = 0.12f),
    onPrimaryContainer = LightPrimary,
    secondaryContainer = LightSecondary.copy(alpha = 0.12f),
    onSecondaryContainer = LightSecondary,
    surfaceContainer = LightSurface,
    surfaceContainerLow = LightBackground,
    surfaceContainerHigh = LightSurface
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to prioritize our premium design system colors
    content: @Composable () -> Unit,
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
        typography = Typography,
        content = content
    )
}
