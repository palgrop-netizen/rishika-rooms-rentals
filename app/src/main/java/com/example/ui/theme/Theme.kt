package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RrrLightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = NavySurfaceVariant,
    onPrimaryContainer = NavyPrimary,
    secondary = GoldAccent,
    onSecondary = NavyPrimary,
    secondaryContainer = GoldLight,
    onSecondaryContainer = GoldDark,
    tertiary = NavyTertiary,
    onTertiary = Color.White,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = OutlineSubtle,
    error = StatusRedBorder,
    onError = Color.White,
    errorContainer = StatusRedBg,
    onErrorContainer = StatusRedText
)

@Composable
fun RrrTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RrrLightColorScheme,
        typography = Typography,
        content = content
    )
}
