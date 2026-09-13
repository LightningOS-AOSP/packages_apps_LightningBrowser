package com.lightning.browser.ui.settings

import android.webkit.WebViewDatabase
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.theme.LightningTheme

@Composable
fun SitePermissionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val hasCredentials = remember(refresh) {
        runCatching {
            WebViewDatabase.getInstance(context).hasHttpAuthUsernamePassword()
        }.getOrDefault(false)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        SettingsHeader(stringResource(R.string.site_permissions_title), onBack)
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.site_permissions_note),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (hasCredentials) colors.primary else colors.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.site_permissions_creds_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(
                            if (hasCredentials) {
                                R.string.site_permissions_stored
                            } else {
                                R.string.site_permissions_none_stored
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                runCatching {
                    WebViewDatabase.getInstance(context).clearHttpAuthUsernamePassword()
                }
                refresh++
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
            ),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(stringResource(R.string.site_permissions_clear_all))
        }
        if (!hasCredentials) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
            ) {
                Text(
                    text = stringResource(R.string.site_permissions_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}