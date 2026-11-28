package com.lightning.browser.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.components.GroupedColumn
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.ThemeMode

private data class SettingsSearchEntry(
    val icon: ImageVector,
    val title: String,
    val summary: String,
    val keywords: String = "",
    val onClick: () -> Unit,
)

@Composable
fun SettingsRootScreen(
    startupBehavior: StartupBehavior,
    themeMode: ThemeMode,
    searchEngine: SearchEngine,
    searchEngineSummary: String? = null,
    onOpenStartup: () -> Unit,
    onOpenControls: () -> Unit,
    onOpenQuickArc: () -> Unit = onOpenControls,
    onOpenBrowsing: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenDns: () -> Unit = onOpenPrivacy,
    onOpenSitePermissions: () -> Unit = onOpenPrivacy,
    onOpenSearchEngine: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    val resolvedSearchEngineSummary = searchEngineSummary ?: stringResource(searchEngine.labelRes)

    val startupTitle = stringResource(R.string.settings_startup)
    val privacyTitle = stringResource(R.string.settings_privacy_security)
    val appearanceTitle = stringResource(R.string.settings_appearance)
    val controlsTitle = stringResource(R.string.settings_controls)
    val browsingTitle = stringResource(R.string.settings_browsing)
    val searchEngineTitle = stringResource(R.string.settings_search_engine)
    val downloadsTitle = stringResource(R.string.settings_downloads)
    val aboutTitle = stringResource(R.string.settings_about)

    val searchableEntries = listOf(
        SettingsSearchEntry(
            icon = Icons.Filled.Home,
            title = startupTitle,
            summary = stringResource(startupBehavior.labelRes),
            keywords = "homepage last opened tab launch open",
            onClick = onOpenStartup,
        ),
        SettingsSearchEntry(
            icon = Icons.Filled.Shield,
            title = privacyTitle,
            summary = stringResource(R.string.settings_privacy_summary),
            keywords = "security adblock dns fingerprint biometric permissions cookies",
            onClick = onOpenPrivacy,
        ),
        SettingsSearchEntry(
            icon = Icons.Filled.Shield,
            title = stringResource(R.string.privacy_ad_blocking),
            summary = "$privacyTitle · ${stringResource(R.string.settings_privacy_summary)}",
            keywords = "ad blocker tracker ads",
            onClick = onOpenPrivacy,
        ),
        SettingsSearchEntry(
            icon = Icons.Filled.Shield,
            title = stringResource(R.string.privacy_dns),
            summary = "$privacyTitle · ${stringResource(R.string.dns_subtitle)}",
            keywords = "doh cloudflare google quad9 custom",
            onClick = onOpenDns,
        ),
        SettingsSearchEntry(
            icon = Icons.Filled.Shield,
            title = stringResource(R.string.privacy_fingerprint),
            summary = "$privacyTitle · ${stringResource(R.string.privacy_fingerprint_summary)}",
            keywords = "biometric lock auth security",
            onClick = onOpenPrivacy,
        ),
        SettingsSearchEntry(
            icon = Icons.Filled.Shield,
            title = stringResource(R.string.privacy_site_permissions),
            summary = "$privacyTitle · ${stringResource(R.string.privacy_site_permissions_summary)}",
            keywords = "saved sign ins passwords credentials permissions",
            onClick = onOpenSitePermissions,
        ),
        SettingsSearchEntry(
            icon = Icons.Filled.Shield,
            title = stringResource(R.string.privacy_clear_data),
            summary = "$privacyTitle · ${stringResource(R.string.privacy_clear_data_summary)}",
            keywords = "clear cache history cookies erase wipe",
            onClick = onOpenPrivacy,
        ),
        SettingsSearchEntry(
            icon = Icons.Filled.Palette,
            title = appearanceTitle,
            summary = themeMode.label(),
            keywords = "theme dark light system preview",
            onClick = onOpenAppearance,
        ),
        SettingsSearchEntry(
            icon = Icons.Filled.Palette,
            title = stringResource(R.string.appearance_tab_previews),
            summary = "$appearanceTitle · ${stringResource(R.string.appearance_tab_previews_summary)}",
            keywords = "thumbnails cards tabs",
            onClick = onOpenAppearance,
        ),
        SettingsSearchEntry(
            icon = Icons.Filled.Palette,
            title = stringResource(R.string.appearance_reading_time),
            summary = "$appearanceTitle · ${stringResource(R.string.appearance_reading_time_summary)}",
            keywords = "article estimate minutes",
            onClick = onOpenAppearance,
        ),
        SettingsSearchEntry(
            icon = Icons.Filled.TouchApp,
            title = controlsTitle,
            summary = stringResource(R.string.settings_controls_summary),
            keywords = "gestures toolbar scroll visibility haptics quick arc",
            onClick = onOpenControls,
        ),
        SettingsSearchEntry(
            icon = Icons.Filled.TouchApp,
            title = stringResource(R.string.appearance_haptics),
            summary = "$controlsTitle · ${stringResource(R.string.appearance_haptics_summary)}",
            keywords = "vibration tactile feedback",
            onClick = onOpenControls,
        ),
        SettingsSearchEntry(
            icon = Icons.Filled.TouchApp,
            title = stringResource(R.string.appearance_quick_arc),
            summary = "$controlsTitle · ${stringResource(R.string.appearance_quick_arc_summary)}",
            keywords = "shortcuts dome hollow size items",
            onClick = onOpenQuickArc,
        ),
        SettingsSearchEntry(
            icon = Icons.Filled.Tab,
            title = browsingTitle,
            summary = stringResource(R.string.settings_browsing_summary),
            keywords = "tabs duplicate warning recently closed wayback clipboard",
            onClick = onOpenBrowsing,
        ),
        SettingsSearchEntry(
            icon = Icons.Filled.Tab,
            title = stringResource(R.string.browsing_duplicate_warning),
            summary = "$browsingTitle · ${stringResource(R.string.browsing_duplicate_warning_summary)}",
            keywords = "tabs url warn",
            onClick = onOpenBrowsing,
        ),
        SettingsSearchEntry(
            icon = Icons.Filled.Tab,
            title = stringResource(R.string.browsing_recently_closed),
            summary = "$browsingTitle · ${stringResource(R.string.browsing_recently_closed_summary)}",
            keywords = "reopen restore history",
            onClick = onOpenBrowsing,
        ),
        SettingsSearchEntry(
            icon = Icons.Filled.Language,
            title = searchEngineTitle,
            summary = resolvedSearchEngineSummary,
            keywords = "google duckduckgo bing brave perplexity wikipedia custom",
            onClick = onOpenSearchEngine,
        ),
        SettingsSearchEntry(
            icon = Icons.Filled.Language,
            title = stringResource(R.string.search_suggestions_header),
            summary = "$searchEngineTitle · ${stringResource(R.string.search_suggestions_show)}",
            keywords = "autocomplete address bar history bookmarks clipboard open tabs shortcuts recent searches",
            onClick = onOpenSearchEngine,
        ),
        SettingsSearchEntry(
            icon = Icons.Filled.Download,
            title = downloadsTitle,
            summary = stringResource(R.string.settings_downloads_summary),
            keywords = "folder save location path pdf viewer private",
            onClick = onOpenDownloads,
        ),
        SettingsSearchEntry(
            icon = Icons.Filled.Download,
            title = stringResource(R.string.pdf_viewer_default_title),
            summary = "$downloadsTitle · ${stringResource(R.string.pdf_viewer_default_inactive)}",
            keywords = "pdf reader viewer default app",
            onClick = onOpenDownloads,
        ),
        SettingsSearchEntry(
            icon = Icons.Filled.Info,
            title = aboutTitle,
            summary = stringResource(R.string.settings_about_summary),
            keywords = "version licenses website open source",
            onClick = onOpenAbout,
        ),
    )

    val trimmedQuery = query.trim()
    val filteredEntries = if (trimmedQuery.isEmpty()) {
        emptyList()
    } else {
        searchableEntries.filter { entry ->
            entry.title.contains(trimmedQuery, ignoreCase = true) ||
                entry.summary.contains(trimmedQuery, ignoreCase = true) ||
                entry.keywords.contains(trimmedQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        SettingsHeader(stringResource(R.string.settings_title), onBack)
        Spacer(Modifier.height(4.dp))
        TextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = {
                Text(
                    text = stringResource(R.string.settings_search_hint),
                    color = colors.onSurfaceVariant,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                )
            },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                        )
                    }
                }
            } else {
                null
            },
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colors.surfaceContainerHigh,
                unfocusedContainerColor = colors.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = colors.primary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        )
        Spacer(Modifier.height(20.dp))

        if (trimmedQuery.isNotEmpty()) {
            if (filteredEntries.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.settings_search_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            } else {
                GroupedColumn(filteredEntries.size) { i, shape ->
                    val entry = filteredEntries[i]
                    SettingsRow(
                        icon = entry.icon,
                        title = entry.title,
                        summary = entry.summary,
                        onClick = entry.onClick,
                        shape = shape,
                        index = i,
                    )
                }
            }
        } else {
            GroupedColumn(8) { i, shape ->
                when (i) {
                    0 -> SettingsRow(
                        icon = Icons.Filled.Home,
                        title = startupTitle,
                        summary = stringResource(startupBehavior.labelRes),
                        onClick = onOpenStartup,
                        shape = shape,
                        index = i,
                    )
                    1 -> SettingsRow(
                        icon = Icons.Filled.Shield,
                        title = privacyTitle,
                        summary = stringResource(R.string.settings_privacy_summary),
                        onClick = onOpenPrivacy,
                        shape = shape,
                        index = i,
                    )
                    2 -> SettingsRow(
                        icon = Icons.Filled.Palette,
                        title = appearanceTitle,
                        summary = themeMode.label(),
                        onClick = onOpenAppearance,
                        shape = shape,
                        index = i,
                    )
                    3 -> SettingsRow(
                        icon = Icons.Filled.TouchApp,
                        title = controlsTitle,
                        summary = stringResource(R.string.settings_controls_summary),
                        onClick = onOpenControls,
                        shape = shape,
                        index = i,
                    )
                    4 -> SettingsRow(
                        icon = Icons.Filled.Tab,
                        title = browsingTitle,
                        summary = stringResource(R.string.settings_browsing_summary),
                        onClick = onOpenBrowsing,
                        shape = shape,
                        index = i,
                    )
                    5 -> SettingsRow(
                        icon = Icons.Filled.Language,
                        title = searchEngineTitle,
                        summary = resolvedSearchEngineSummary,
                        onClick = onOpenSearchEngine,
                        shape = shape,
                        index = i,
                    )
                    6 -> SettingsRow(
                        icon = Icons.Filled.Download,
                        title = downloadsTitle,
                        summary = stringResource(R.string.settings_downloads_summary),
                        onClick = onOpenDownloads,
                        shape = shape,
                        index = i,
                    )
                    else -> SettingsRow(
                        icon = Icons.Filled.Info,
                        title = aboutTitle,
                        summary = stringResource(R.string.settings_about_summary),
                        onClick = onOpenAbout,
                        shape = shape,
                        index = i,
                    )
                }
            }
        }
    }
}
