package com.lightning.browser.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.ThemeMode

@Composable
fun SettingsRootScreen(
    themeMode: ThemeMode,
    searchEngine: SearchEngine,
    onOpenAppearance: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenSearchEngine: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        SettingsHeader(stringResource(R.string.settings_title), onBack)
        Spacer(Modifier.height(4.dp))
        Surface(
            color = colors.surfaceContainer,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(8.dp)) {
                SettingsRow(
                    icon = Icons.Filled.Shield,
                    title = stringResource(R.string.settings_privacy_security),
                    summary = stringResource(R.string.settings_privacy_summary),
                    onClick = onOpenPrivacy,
                )
                Spacer(Modifier.height(2.dp))
                SettingsRow(
                    icon = Icons.Filled.Palette,
                    title = stringResource(R.string.settings_appearance),
                    summary = themeMode.label(),
                    onClick = onOpenAppearance,
                )
                Spacer(Modifier.height(2.dp))
                SettingsRow(
                    icon = Icons.Filled.Language,
                    title = stringResource(R.string.settings_search_engine),
                    summary = stringResource(searchEngine.labelRes),
                    onClick = onOpenSearchEngine,
                )
                Spacer(Modifier.height(2.dp))
                SettingsRow(
                    icon = Icons.Filled.Download,
                    title = stringResource(R.string.settings_downloads),
                    summary = stringResource(R.string.settings_downloads_summary),
                    onClick = onOpenDownloads,
                )
                Spacer(Modifier.height(2.dp))
                SettingsRow(
                    icon = Icons.Filled.Info,
                    title = stringResource(R.string.settings_about),
                    summary = stringResource(R.string.settings_about_summary),
                    onClick = onOpenAbout,
                )
            }
        }
    }
}