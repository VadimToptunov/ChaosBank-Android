package com.vadimtoptunov.chaosbank_android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ChaosColorScheme = darkColorScheme(
    primary = Palette.sand,
    onPrimary = Palette.bg,
    secondary = Palette.sand,
    background = Palette.bg,
    onBackground = Palette.text,
    surface = Palette.surface,
    onSurface = Palette.text,
    surfaceVariant = Palette.surface2,
    error = Palette.loss,
    outline = Palette.line,
)

/** ChaosBank is dark-only, brand-colored — no dynamic color. */
@Composable
fun ChaosBankAndroidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ChaosColorScheme,
        typography = Typography,
        content = content,
    )
}
