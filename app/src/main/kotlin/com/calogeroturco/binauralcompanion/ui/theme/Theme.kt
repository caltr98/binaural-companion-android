package com.calogeroturco.binauralcompanion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DeepInk = Color(0xFF091018)
val DeepSurface = Color(0xFF101B2A)
val RaisedSurface = Color(0xFF172438)
val Mint = Color(0xFF73E6C5)
val Periwinkle = Color(0xFF9AAEFF)
val Warm = Color(0xFFFFC978)
val TextPrimary = Color(0xFFF5F7FB)
val TextSecondary = Color(0xFFB8C3D5)

private val Colors = darkColorScheme(
    primary = Mint,
    onPrimary = DeepInk,
    primaryContainer = Color(0xFF16483E),
    onPrimaryContainer = Color(0xFFD4FFF1),
    secondary = Periwinkle,
    onSecondary = DeepInk,
    secondaryContainer = Color(0xFF2B3764),
    onSecondaryContainer = Color(0xFFE2E7FF),
    tertiary = Warm,
    onTertiary = DeepInk,
    background = DeepInk,
    onBackground = TextPrimary,
    surface = DeepSurface,
    onSurface = TextPrimary,
    surfaceVariant = RaisedSurface,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF526078),
    error = Color(0xFFFFB4AB),
)

@Composable
fun BinauralCompanionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        content = content,
    )
}
