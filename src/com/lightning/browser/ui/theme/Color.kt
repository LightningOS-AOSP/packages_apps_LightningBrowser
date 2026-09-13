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
)