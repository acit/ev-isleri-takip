package com.aile.takip.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Blue, secondary = Green, tertiary = Orange, error = Red,
    background = LightBg, surface = LightSurface,
    onBackground = LightText, onSurface = LightText,
    surfaceVariant = LightBorder, outline = LightBorder,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkBlue, secondary = DarkGreen, tertiary = DarkOrange, error = DarkRed,
    background = DarkBg, surface = DarkSurface,
    onBackground = DarkText, onSurface = DarkText,
    surfaceVariant = DarkElevated, outline = DarkBorder,
)

@Composable
fun AileTakipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
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
    MaterialTheme(colorScheme = colorScheme, content = content)
}
