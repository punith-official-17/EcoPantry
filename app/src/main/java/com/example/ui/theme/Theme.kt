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
    primary = EmeraldGreenLight,
    onPrimary = Color(0xFF003912),
    primaryContainer = MintContainerDark,
    onPrimaryContainer = Color(0xFF9FF6A6),
    secondary = SageGreen,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF334B38),
    onSecondaryContainer = Color(0xFFD6E8D8),
    tertiary = ExpiryWarning,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    error = ExpiryCritical,
    outline = OutlineLight
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldGreen,
    onPrimary = Color.White,
    primaryContainer = MintContainer,
    onPrimaryContainer = ForestGreen,
    secondary = SageGreen,
    onSecondary = Color.White,
    secondaryContainer = SageContainer,
    onSecondaryContainer = Color(0xFF142618),
    tertiary = ExpiryWarning,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    error = ExpiryCritical,
    outline = OutlineLight
)

@Composable
fun SmartPantryTheme(
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
        typography = Typography,
        content = content
    )
}

