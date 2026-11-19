package com.lightning.browser.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.components.GroupedColumn
import com.lightning.browser.ui.components.ShapedIcon
import com.lightning.browser.ui.theme.ExpressiveShapes
import com.lightning.browser.ui.theme.LightningTheme

@Composable
fun AboutScreen(
    onOpenWebsite: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    val context = LocalContext.current
    var showLicenses by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        SettingsHeader(stringResource(R.string.about_title), onBack)
        Spacer(Modifier.height(24.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ShapedIcon(
                Icons.Filled.FlashOn,
                ExpressiveShapes.Cookie9,
                colors.primaryContainer,
                colors.onPrimaryContainer,
                size = 96.dp,
                iconSize = 44.dp,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.about_version),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(32.dp))
        GroupedColumn(2) { i, shape ->
            when (i) {
                0 -> SettingsRow(
                    icon = Icons.Filled.Code,
                    title = stringResource(R.string.about_licenses),
                    summary = stringResource(R.string.about_licenses_summary),
                    onClick = { showLicenses = true },
                    shape = shape,
                    index = i,
                )
                else -> SettingsRow(
                    icon = Icons.Filled.Public,
                    title = stringResource(R.string.about_website),
                    summary = stringResource(R.string.about_website_summary),
                    onClick = {
                        val website = context.getString(R.string.about_website_url)
                        onOpenWebsite(website)
                    },
                    shape = shape,
                    index = i,
                )
            }
        }
    }

    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            confirmButton = {
                TextButton(onClick = { showLicenses = false }) {
                    Text(stringResource(R.string.dns_custom_cancel), color = colors.primary)
                }
            },
            title = { Text(stringResource(R.string.about_licenses), color = colors.onSurface) },
            text = {
                Text(
                    text = stringResource(R.string.about_licenses_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            },
        )
    }
}