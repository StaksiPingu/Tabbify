package com.tabbify.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TabbifyPrimary = Color(0xFF6750A4)
val TabbifySecondary = Color(0xFF625B71)
val TabbifyTertiary = Color(0xFF7D5260)
val TabbifyBackground = Color(0xFF1C1B1F)
val TabbifySurface = Color(0xFF2B2930)
val TabbifyAccent = Color(0xFFD0BCFF)
val TabbifySuccess = Color(0xFF4CAF50)
val TabbifyWarning = Color(0xFFFF9800)
val TabbifyError = Color(0xFFF44336)

private val DarkColorScheme = darkColorScheme(
    primary = TabbifyAccent,
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    background = TabbifyBackground,
    onBackground = Color(0xFFE6E1E5),
    surface = TabbifySurface,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    error = TabbifyError,
    tertiary = Color(0xFFEFB8C8)
)

private val LightColorScheme = lightColorScheme(
    primary = TabbifyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = TabbifySecondary,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    error = TabbifyError
)

@Composable
fun TabbifyTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
