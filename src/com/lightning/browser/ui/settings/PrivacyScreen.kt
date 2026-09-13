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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.theme.LightningTheme

@Composable
fun PrivacyScreen(
    provider: DnsProvider,
    adBlocking: Boolean,
    blockedCount: Int,
    fingerprintLock: Boolean,
    onAdBlockingChange: (Boolean) -> Unit,
    onFingerprintChange: (Boolean) -> Unit,
    onOpenDns: () -> Unit,
    onOpenSitePermissions: () -> Unit,
    onClearData: () -> Unit,
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
        SettingsHeader(stringResource(R.string.privacy_title), onBack)
        Spacer(Modifier.height(16.dp))
        Surface(
            color = colors.surfaceContainer,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(8.dp)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.privacy_ad_blocking),
                    summary = stringResource(R.string.privacy_ad_blocking_summary, blockedCount),
                    checked = adBlocking,
                    onCheckedChange = onAdBlockingChange,
                )
                Spacer(Modifier.height(2.dp))
                SettingsDivider()
                Spacer(Modifier.height(2.dp))
                SettingsRow(
                    icon = Icons.Filled.Public,
                    title = stringResource(R.string.privacy_dns),
                    summary = stringResource(provider.labelRes),
                    onClick = onOpenDns,
                )
                Spacer(Modifier.height(2.dp))
                SettingsDivider()
                Spacer(Modifier.height(2.dp))
                SettingsSwitchRow(
                    title = stringResource(R.string.privacy_fingerprint),
                    summary = stringResource(R.string.privacy_fingerprint_summary),
                    checked = fingerprintLock,
                    onCheckedChange = onFingerprintChange,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Surface(
            color = colors.surfaceContainer,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(8.dp)) {
                SettingsRow(
                    icon = Icons.Filled.Apps,
                    title = stringResource(R.string.privacy_site_permissions),
                    summary = stringResource(R.string.privacy_site_permissions_summary),
                    onClick = onOpenSitePermissions,
                )
                Spacer(Modifier.height(2.dp))
                SettingsDivider()
                Spacer(Modifier.height(2.dp))
                SettingsRow(
                    icon = Icons.Filled.Delete,
                    title = stringResource(R.string.privacy_clear_data),
                    summary = stringResource(R.string.privacy_clear_data_summary),
                    onClick = onClearData,
                )
            }
        }
    }
}