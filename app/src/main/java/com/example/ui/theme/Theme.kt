package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CosmicDarkColorScheme = darkColorScheme(
    primary = NeonMint,
    secondary = SoftCyan,
    background = ObsidianBg,
    surface = BentoCoal,
    surfaceVariant = BentoCardGlow,
    onPrimary = ObsidianPure,
    onSecondary = ObsidianPure,
    onBackground = SlateTextPrimary,
    onSurface = SlateTextPrimary,
    outline = BorderHighTech,
    error = ErrorRed
)

private val CosmicLightColorScheme = androidx.compose.material3.lightColorScheme(
    primary = NeonTealLight,
    secondary = SoftCyanLight,
    background = BentoLightBg,
    surface = BentoCardWhite,
    surfaceVariant = BentoCardLightGlow,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DarkCharcoalText,
    onSurface = DarkCharcoalText,
    outline = LightBorder,
    error = ErrorRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Custom theme toggle
    dynamicColor: Boolean = false, // Keep themed branding consistent
    content: @Composable () -> Unit
) {
    // Elegant toggle between light and dark custom bento themes
    val colorScheme = if (darkTheme) CosmicDarkColorScheme else CosmicLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
