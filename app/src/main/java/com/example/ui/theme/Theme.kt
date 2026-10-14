package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MiLightColorScheme = lightColorScheme(
    primary = MiDarkGreen,
    onPrimary = Color.White,
    primaryContainer = MiMintSoft,
    onPrimaryContainer = MiDarkGreen,
    secondary = MiEmerald,
    onSecondary = Color.White,
    secondaryContainer = MiMintSoft,
    onSecondaryContainer = MiEmerald,
    tertiary = MiEmeraldLight,
    onTertiary = Color.White,
    background = MiBackground,
    onBackground = MiTextPrimary,
    surface = MiSurface,
    onSurface = MiTextPrimary,
    surfaceVariant = MiSurfaceVariant,
    onSurfaceVariant = MiTextSecondary,
    outline = MiBorderColor,
    outlineVariant = MiMintBorder,
    error = StatusAlpaText,
    onError = Color.White,
    errorContainer = StatusAlpaBg,
    onErrorContainer = StatusAlpaText
)

val MiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MiLightColorScheme,
        typography = Typography,
        shapes = MiShapes,
        content = content
    )
}
