package com.aile.takip.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Blue,
    secondary = Green,
    tertiary = Orange,
    error = Red,
    background = LightBg,
    surface = LightSurface,
    surfaceVariant = LightBorder,
    onBackground = LightText,
    onSurface = LightText,
    onSurfaceVariant = LightTextSec,
    outline = LightBorder,
    inverseSurface = DarkSurface,
    inverseOnSurface = DarkText,
    inversePrimary = DarkBlue,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkBlue,
    secondary = DarkGreen,
    tertiary = DarkOrange,
    error = DarkRed,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkElevated,
    onBackground = DarkText,
    onSurface = DarkText,
    onSurfaceVariant = DarkTextSec,
    outline = DarkBorder,
    inverseSurface = LightSurface,
    inverseOnSurface = LightText,
    inversePrimary = Blue,
)

@Composable
fun AileTakipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Material You dynamic color (Android 12+)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        // Custom dark theme
        darkTheme -> DarkColorScheme
        // Custom light theme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
