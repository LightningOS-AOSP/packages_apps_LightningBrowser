package com.lightning.browser.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.theme.LightningTheme

@Composable
fun DnsScreen(
    provider: DnsProvider,
    customUrl: String,
    bypassTrusted: Boolean,
    onProviderSelect: (DnsProvider) -> Unit,
    onCustomUrlChange: (String) -> Unit,
    onBypassTrustedChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    var showCustomDialog by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(customUrl) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        SettingsHeader(stringResource(R.string.dns_title), onBack)
        Text(
            text = stringResource(R.string.dns_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(20.dp))
        Surface(
            color = colors.surfaceContainer,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(8.dp)) {
                DnsProvider.entries.forEachIndexed { index, choice ->
                    DnsProviderRow(
                        choice = choice,
                        selected = choice == provider,
                        customUrl = customUrl,
                        onClick = {
                            if (choice == DnsProvider.CUSTOM) {
                                draft = customUrl
                                showCustomDialog = true
                            }
                            onProviderSelect(choice)
                        },
                    )
                    if (index < DnsProvider.entries.lastIndex) {
                        SettingsDivider()
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Surface(
            color = colors.surfaceContainer,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(8.dp)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.dns_bypass_trusted),
                    summary = stringResource(R.string.dns_bypass_trusted_detail),
                    checked = bypassTrusted,
                    onCheckedChange = onBypassTrustedChange,
                )
            }
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text(stringResource(R.string.dns_custom_title)) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text(stringResource(R.string.dns_custom_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text(stringResource(R.string.dns_custom_cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onCustomUrlChange(draft.trim())
                    showCustomDialog = false
                }) {
                    Text(stringResource(R.string.dns_custom_save))
                }
            },
        )
    }
}

@Composable
private fun DnsProviderRow(
    choice: DnsProvider,
    selected: Boolean,
    customUrl: String,
    onClick: () -> Unit,
) {
    val colors = LightningTheme.colors
    val summary = if (choice == DnsProvider.CUSTOM && selected && customUrl.isNotBlank()) {
        customUrl
    } else {
        stringResource(choice.summaryRes)
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            RadioDot(selected)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(choice.labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    val colors = LightningTheme.colors
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(
                color = if (selected) Color.Transparent else colors.surfaceContainerHigh,
                shape = CircleShape,
            )
            .border(
                width = 2.dp,
                color = if (selected) colors.primary else colors.outline,
                shape = CircleShape,
            ),
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(10.dp)
                    .background(colors.primary, CircleShape),
            )
        }
    }
}