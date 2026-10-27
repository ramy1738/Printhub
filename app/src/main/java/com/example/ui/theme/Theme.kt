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
    primary = PrintBlueLight,
    onPrimary = Color.White,
    primaryContainer = PrintBlueDark,
    onPrimaryContainer = PrintBlueContainer,
    secondary = PrintTealLight,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = PrintTealContainer,
    tertiary = PrintAmber,
    background = Slate900,
    surface = Slate800,
    surfaceVariant = Color(0xFF283548),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    outline = Color(0xFF475569)
)

private val LightColorScheme = lightColorScheme(
    primary = PrintBluePrimary,
    onPrimary = Color.White,
    primaryContainer = PrintBlueContainer,
    onPrimaryContainer = OnPrintBlueContainer,
    secondary = PrintTealSecondary,
    onSecondary = Color.White,
    secondaryContainer = PrintTealContainer,
    onSecondaryContainer = OnPrintTealContainer,
    tertiary = PrintAmber,
    background = Slate50,
    surface = Color.White,
    surfaceVariant = Slate100,
    onBackground = Slate900,
    onSurface = Slate900,
    outline = Slate200
)

@Composable
fun ImprimeDocTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our signature print theme by default
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

// Backward-compatible alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = ImprimeDocTheme(darkTheme, dynamicColor, content)
