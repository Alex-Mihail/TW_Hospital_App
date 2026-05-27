package com.example.proiecttw_android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = TwBlue40,
    onPrimary = Color.White,
    primaryContainer = TwBlue80,
    onPrimaryContainer = TwBlueDeep,
    secondary = TwAccent40,
    onSecondary = Color.White,
    secondaryContainer = TwAccent80,
    onSecondaryContainer = TwAccent40,
    background = TwBg,
    onBackground = TwText,
    surface = Color.White,
    onSurface = TwText
)

private val DarkColorScheme = darkColorScheme(
    primary = TwBlue80,
    onPrimary = TwBlueDeep,
    primaryContainer = TwBlue40,
    onPrimaryContainer = TwBlue80,
    secondary = TwAccent80,
    onSecondary = TwAccent40,
    background = Color(0xFF121A22),
    onBackground = Color(0xFFE5ECF2),
    surface = Color(0xFF1A2530),
    onSurface = Color(0xFFE5ECF2)
)

@Composable
fun ProiectTW_AndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
