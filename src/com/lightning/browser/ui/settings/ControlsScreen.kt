package com.lightning.browser.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.BrowserStore.BarScrollBehavior
import com.lightning.browser.ui.components.GroupedColumn
import com.lightning.browser.ui.theme.EyebrowTextStyle
import com.lightning.browser.ui.theme.LightningTheme

@Composable
fun ControlsScreen(
    hapticsEnabled: Boolean,
    quickArcSummary: String,
    barScrollBehavior: BarScrollBehavior,
    onHapticsChange: (Boolean) -> Unit,
    onOpenQuickArc: () -> Unit,
    onBarScrollBehaviorChange: (BarScrollBehavior) -> Unit,
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
                else -> SettingsRow(
                    icon = Icons.Filled.FlashOn,
                    title = stringResource(R.string.appearance_quick_arc),
                    summary = quickArcSummary,
                    onClick = onOpenQuickArc,
                    shape = shape,
                    index = i,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.settings_toolbar_behavior_title).uppercase(),
            style = EyebrowTextStyle,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_toolbar_behavior_summary),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(10.dp))
        GroupedColumn(4) { i, shape ->
            val (behavior, titleRes, descRes) = when (i) {
                0 -> Triple(
                    BarScrollBehavior.HIDE_BOTH,
                    R.string.toolbar_behavior_hide_both,
                    R.string.toolbar_behavior_hide_both_desc,
                )
                1 -> Triple(
                    BarScrollBehavior.ALWAYS_VISIBLE,
                    R.string.toolbar_behavior_always_visible,
                    R.string.toolbar_behavior_always_visible_desc,
                )
                2 -> Triple(
                    BarScrollBehavior.TOP_ALWAYS,
                    R.string.toolbar_behavior_top_always,
                    R.string.toolbar_behavior_top_always_desc,
                )
                else -> Triple(
                    BarScrollBehavior.BOTTOM_ALWAYS,
                    R.string.toolbar_behavior_bottom_always,
                    R.string.toolbar_behavior_bottom_always_desc,
                )
            }
            SettingsRadioRow(
                title = stringResource(titleRes),
                summary = stringResource(descRes),
                selected = barScrollBehavior == behavior,
                onClick = { onBarScrollBehaviorChange(behavior) },
                shape = shape,
            )
        }
    }
}
