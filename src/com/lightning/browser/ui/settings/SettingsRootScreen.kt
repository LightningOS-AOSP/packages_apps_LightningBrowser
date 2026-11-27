package com.lightning.browser.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tab
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.components.GroupedColumn
import com.lightning.browser.ui.theme.ThemeMode

@Composable
fun SettingsRootScreen(
    startupBehavior: StartupBehavior,
    themeMode: ThemeMode,
    searchEngine: SearchEngine,
    onOpenStartup: () -> Unit,
    onOpenBrowsing: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenSearchEngine: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAbout: () -> Unit,
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
        SettingsHeader(stringResource(R.string.settings_title), onBack)
        Spacer(Modifier.height(4.dp))
        GroupedColumn(7) { i, shape ->
            when (i) {
                0 -> SettingsRow(
                    icon = Icons.Filled.Home,
                    title = stringResource(R.string.settings_startup),
                    summary = stringResource(startupBehavior.labelRes),
                    onClick = onOpenStartup,
                    shape = shape,
                    index = i,
                )
                1 -> SettingsRow(
                    icon = Icons.Filled.Shield,
                    title = stringResource(R.string.settings_privacy_security),
                    summary = stringResource(R.string.settings_privacy_summary),
                    onClick = onOpenPrivacy,
                    shape = shape,
                    index = i,
                )
                2 -> SettingsRow(
                    icon = Icons.Filled.Palette,
                    title = stringResource(R.string.settings_appearance),
                    summary = themeMode.label(),
                    onClick = onOpenAppearance,
                    shape = shape,
                    index = i,
                )
                3 -> SettingsRow(
                    icon = Icons.Filled.Tab,
                    title = stringResource(R.string.settings_browsing),
                    summary = stringResource(R.string.settings_browsing_summary),
                    onClick = onOpenBrowsing,
                    shape = shape,
                    index = i,
                )
                4 -> SettingsRow(
                    icon = Icons.Filled.Language,
                    title = stringResource(R.string.settings_search_engine),
                    summary = stringResource(searchEngine.labelRes),
                    onClick = onOpenSearchEngine,
                    shape = shape,
                    index = i,
                )
                5 -> SettingsRow(
                    icon = Icons.Filled.Download,
                    title = stringResource(R.string.settings_downloads),
                    summary = stringResource(R.string.settings_downloads_summary),
                    onClick = onOpenDownloads,
                    shape = shape,
                    index = i,
                )
                else -> SettingsRow(
                    icon = Icons.Filled.Info,
                    title = stringResource(R.string.settings_about),
                    summary = stringResource(R.string.settings_about_summary),
                    onClick = onOpenAbout,
                    shape = shape,
                    index = i,
                )
            }
        }
    }
}
