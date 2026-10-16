package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AcademicDarkPrimary,
    onPrimary = AcademicDarkOnPrimary,
    primaryContainer = AcademicDarkPrimaryContainer,
    onPrimaryContainer = AcademicDarkOnPrimaryContainer,
    secondary = AcademicDarkSecondary,
    onSecondary = AcademicDarkOnSecondary,
    secondaryContainer = AcademicDarkSecondaryContainer,
    onSecondaryContainer = AcademicDarkOnSecondaryContainer,
    tertiary = AcademicDarkTertiary,
    onTertiary = AcademicDarkOnTertiary,
    tertiaryContainer = AcademicDarkTertiaryContainer,
    onTertiaryContainer = AcademicDarkOnTertiaryContainer,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = AcademicBluePrimary,
    onPrimary = AcademicBlueOnPrimary,
    primaryContainer = AcademicBluePrimaryContainer,
    onPrimaryContainer = AcademicBlueOnPrimaryContainer,
    secondary = AcademicSecondary,
    onSecondary = AcademicOnSecondary,
    secondaryContainer = AcademicSecondaryContainer,
    onSecondaryContainer = AcademicOnSecondaryContainer,
    tertiary = AcademicTertiary,
    onTertiary = AcademicOnTertiary,
    tertiaryContainer = AcademicTertiaryContainer,
    onTertiaryContainer = AcademicOnTertiaryContainer,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant
)

@Composable
fun GradeTrackerProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent academic brand identity
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
        typography = Typography,
        content = content
    )
}
