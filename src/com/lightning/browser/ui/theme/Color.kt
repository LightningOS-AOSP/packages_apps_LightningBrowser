package com.lightning.browser.ui.theme

import androidx.compose.ui.graphics.Color

// The two token sets from the brand spec. Everything else in the app reads
// through these (or through LightningTheme.colors) so a screen never hardcodes
// a color.

data class LightningColors(
    val surface: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerHighest: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
)

val lightColors = LightningColors(
    surface = Color(0xFFFFFBFE),
    surfaceContainer = Color(0xFFF7F2FA),
    surfaceContainerHigh = Color(0xFFE7E0EC),
    onSurface = Color(0xFF1D1B20),
    onSurfaceVariant = Color(0xFF79747E),
    primary = Color(0xFFF5A623),
    onPrimary = Color(0xFF412402),
    primaryContainer = Color(0xFFFFDDA6),
    onPrimaryContainer = Color(0xFF412402),
    secondaryContainer = Color(0xFFEADDFF),
    onSecondaryContainer = Color(0xFF21005D),
    tertiaryContainer = Color(0xFFCEECEC),
    onTertiaryContainer = Color(0xFF00201F),
    outline = Color(0xFFCAC4D0),
    outlineVariant = Color(0xFFC9C5D0),
    surfaceContainerLow = Color(0xFFFBF7FD),
    surfaceContainerHighest = Color(0xFFDDD6E2),
    secondary = Color(0xFF6B4FA3),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF2E7D7D),
    onTertiary = Color(0xFFFFFFFF),
)

val darkColors = LightningColors(
    surface = Color(0xFF141218),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    onSurface = Color(0xFFE6E0E9),
    onSurfaceVariant = Color(0xFF938F99),
    primary = Color(0xFFF5A623),
    onPrimary = Color(0xFF412402),
    primaryContainer = Color(0xFF4A3300),
    onPrimaryContainer = Color(0xFFFFDDA6),
    secondaryContainer = Color(0xFF332D41),
    onSecondaryContainer = Color(0xFFD0BCFF),
    tertiaryContainer = Color(0xFF004F58),
    onTertiaryContainer = Color(0xFF9DEEFF),
    outline = Color(0xFF49454F),
    outlineVariant = Color(0xFF494550),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainerHighest = Color(0xFF36343B),
    secondary = Color(0xFFD0BCFF),
    onSecondary = Color(0xFF381E72),
    tertiary = Color(0xFF80D5D3),
    onTertiary = Color(0xFF003736),
)

data class Tone(val container: Color, val content: Color)

/** Cycles primary / secondary / tertiary / solid-primary tones by index. */
fun LightningColors.tone(index: Int): Tone = when (index.mod(4)) {
    0 -> Tone(primaryContainer, onPrimaryContainer)
    1 -> Tone(secondaryContainer, onSecondaryContainer)
    2 -> Tone(tertiaryContainer, onTertiaryContainer)
    else -> Tone(primary, onPrimary)
}
