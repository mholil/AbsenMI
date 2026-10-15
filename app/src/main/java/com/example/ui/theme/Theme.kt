package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MiLightColorScheme = lightColorScheme(
    primary = MiEmerald,
    onPrimary = Color.White,
    primaryContainer = MiMintSoft,
    onPrimaryContainer = MiDarkGreen,
    secondary = MiEmeraldLight,
    onSecondary = Color.White,
    secondaryContainer = MiMintSoft,
    onSecondaryContainer = MiEmeraldLight,
    tertiary = Color(0xFF72C7FF),
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
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MiLightColorScheme,
        typography = Typography,
        shapes = MiShapes
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalTextStyle provides Typography.bodyMedium
        ) {
            content()
        }
    }
}
