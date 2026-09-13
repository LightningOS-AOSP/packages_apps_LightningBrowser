package com.lightning.browser.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalLightningColors = staticCompositionLocalOf { lightColors }

enum class ThemeMode { LIGHT, DARK, SYSTEM }

fun lightningColorsFor(dark: Boolean) = if (dark) darkColors else lightColors

@Composable
fun LightningTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val colors = lightningColorsFor(dark)
    val colorScheme = if (dark) {
        darkColorScheme(
            surface = colors.surface,
            surfaceContainer = colors.surfaceContainer,
            surfaceContainerHigh = colors.surfaceContainerHigh,
            onSurface = colors.onSurface,
            onSurfaceVariant = colors.onSurfaceVariant,
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.onPrimaryContainer,
            secondaryContainer = colors.secondaryContainer,
            onSecondaryContainer = colors.onSecondaryContainer,
            tertiaryContainer = colors.tertiaryContainer,
            onTertiaryContainer = colors.onTertiaryContainer,
            outline = colors.outline,
        )
    } else {
        lightColorScheme(
            surface = colors.surface,
            surfaceContainer = colors.surfaceContainer,
            surfaceContainerHigh = colors.surfaceContainerHigh,
            onSurface = colors.onSurface,
            onSurfaceVariant = colors.onSurfaceVariant,
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.onPrimaryContainer,
            secondaryContainer = colors.secondaryContainer,
            onSecondaryContainer = colors.onSecondaryContainer,
            tertiaryContainer = colors.tertiaryContainer,
            onTertiaryContainer = colors.onTertiaryContainer,
            outline = colors.outline,
        )
    }

    CompositionLocalProvider(LocalLightningColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LightningTypography,
            shapes = LightningShapes,
            content = content,
        )
    }
}

object LightningTheme {
    val colors: LightningColors
        @Composable get() = LocalLightningColors.current
}