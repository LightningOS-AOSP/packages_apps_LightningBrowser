package com.lightning.browser.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.components.GroupedColumn

@Composable
fun ControlsScreen(
    hapticsEnabled: Boolean,
    quickArcEnabled: Boolean,
    onHapticsChange: (Boolean) -> Unit,
    onQuickArcChange: (Boolean) -> Unit,
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
        SettingsHeader(stringResource(R.string.controls_title), onBack)
        Spacer(Modifier.height(16.dp))
        GroupedColumn(2) { i, shape ->
            when (i) {
                0 -> SettingsSwitchRow(
                    title = stringResource(R.string.appearance_haptics),
                    summary = stringResource(R.string.appearance_haptics_summary),
                    checked = hapticsEnabled,
                    onCheckedChange = onHapticsChange,
                    shape = shape,
                )
                else -> SettingsSwitchRow(
                    title = stringResource(R.string.appearance_quick_arc),
                    summary = stringResource(R.string.appearance_quick_arc_summary),
                    checked = quickArcEnabled,
                    onCheckedChange = onQuickArcChange,
                    shape = shape,
                )
            }
        }
    }
}
