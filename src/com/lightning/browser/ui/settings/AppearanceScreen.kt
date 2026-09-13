package com.lightning.browser.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.ThemeMode
import com.lightning.browser.ui.theme.lightningColorsFor

@Composable
fun AppearanceScreen(
    themeMode: ThemeMode,
    onThemeModeSelect: (ThemeMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        SettingsHeader(stringResource(R.string.appearance_title), onBack)
        Spacer(Modifier.height(12.dp))
        SegmentedThemePicker(themeMode, onThemeModeSelect)
        Spacer(Modifier.height(28.dp))
        ThemePreview(themeMode)
    }
}

@Composable
private fun SegmentedThemePicker(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val colors = LightningTheme.colors
    Surface(
        color = colors.surfaceContainerHigh,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(4.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                val isSelected = mode == selected
                Surface(
                    onClick = { onSelect(mode) },
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) colors.primary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = mode.label(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.ThemePreview(themeMode: ThemeMode) {
    // The phone mock renders with the picked mode's token set so the choice
    // is felt before it is applied.
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val preview = lightningColorsFor(dark)
    Surface(
        color = preview.surface,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, preview.outline),
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .width(220.dp)
            .height(340.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(preview.primary, CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.appearance_preview_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = preview.onSurface,
                )
            }
            Spacer(Modifier.height(14.dp))
            Surface(
                color = preview.surfaceContainerHigh,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
            ) {}
            Spacer(Modifier.height(16.dp))
            PreviewTile(preview.surfaceContainer, preview.primary)
            Spacer(Modifier.height(8.dp))
            PreviewTile(preview.surfaceContainer, preview.primaryContainer)
            Spacer(Modifier.height(8.dp))
            PreviewTile(preview.surfaceContainer, preview.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            PreviewTile(preview.surfaceContainer, preview.tertiaryContainer)
        }
    }
}

@Composable
private fun PreviewTile(tileColor: Color, dotColor: Color) {
    Surface(
        color = tileColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(dotColor, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(dotColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp)),
            )
        }
    }
}