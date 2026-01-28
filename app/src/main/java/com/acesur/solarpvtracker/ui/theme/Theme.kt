
package com.acesur.solarpvtracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SolarDarkColorScheme = darkColorScheme(
    primary = EnergyOrangeLight,
    onPrimary = Color.Black, // Orange light is bright enough for black text
    primaryContainer = Color(0xFF662C00), // Darker, less saturated orange for better text contrast
    onPrimaryContainer = Color(0xFFFFDBC9),
    
    secondary = TechBlueLight,
    onSecondary = Color.Black,
    secondaryContainer = TechBlueDark,
    onSecondaryContainer = Color(0xFFD1E4FF),
    
    tertiary = SunGold,
    onTertiary = Color.Black,
    tertiaryContainer = SunGoldDark,
    onTertiaryContainer = Color(0xFFFFEFA8),
    
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    
    error = WarningRed,
    onError = Color.White
)

private val SolarLightColorScheme = lightColorScheme(
    primary = EnergyOrange,
    onPrimary = LightOnPrimary,
    primaryContainer = Color(0xFFFFDBCA), // Very light orange
    onPrimaryContainer = Color(0xFF331500), // Dark brown/orange
    
    secondary = TechBlue,
    onSecondary = LightOnSecondary,
    secondaryContainer = Color(0xFFD1E4FF),
    onSecondaryContainer = Color(0xFF001D36),
    
    tertiary = SunGold,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFF9C4),
    onTertiaryContainer = Color(0xFF332F00),
    
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    
    error = WarningRed,
    onError = Color.White
)

@Composable
fun SolarPVTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Defaulting to false to enforce our custom Solar identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> SolarDarkColorScheme
        else -> SolarLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}