package com.lightning.browser.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.components.GroupedColumn
import com.lightning.browser.ui.theme.LightningTheme

@Composable
fun StartupScreen(
    startupBehavior: StartupBehavior,
    onStartupBehaviorSelect: (StartupBehavior) -> Unit,
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
        SettingsHeader(stringResource(R.string.startup_title), onBack)
        Text(
            text = stringResource(R.string.startup_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(20.dp))
        GroupedColumn(StartupBehavior.entries.size) { index, shape ->
            val choice = StartupBehavior.entries[index]
            SettingsRadioRow(
                title = stringResource(choice.labelRes),
                summary = stringResource(choice.summaryRes),
                selected = choice == startupBehavior,
                onClick = { onStartupBehaviorSelect(choice) },
                shape = shape,
            )
        }
    }
}
