package com.lightning.browser.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.BrowserStore.QuickArcAction
import com.lightning.browser.ui.BrowserStore.QuickArcSize
import com.lightning.browser.ui.BrowserStore.QuickArcStyle
import com.lightning.browser.ui.bar.QuickArcContent
import com.lightning.browser.ui.components.GroupedColumn
import com.lightning.browser.ui.theme.EyebrowTextStyle
import com.lightning.browser.ui.theme.LightningMotion
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.rememberLightningHaptics

private data class ArcItemOption(
    val action: QuickArcAction,
    val labelRes: Int,
)

private val ALL_ARC_OPTIONS = listOf(
    ArcItemOption(QuickArcAction.HOME, R.string.quick_arc_home),
    ArcItemOption(QuickArcAction.TABS, R.string.quick_arc_tabs),
    ArcItemOption(QuickArcAction.SETTINGS, R.string.quick_arc_settings),
    ArcItemOption(QuickArcAction.NEW_TAB, R.string.quick_arc_new_tab),
    ArcItemOption(QuickArcAction.BOOKMARKS, R.string.quick_arc_bookmarks),
    ArcItemOption(QuickArcAction.HISTORY, R.string.quick_arc_history),
    ArcItemOption(QuickArcAction.DOWNLOADS, R.string.quick_arc_downloads),
    ArcItemOption(QuickArcAction.SAVED_PAGES, R.string.quick_arc_saved_pages),
)

@Composable
fun QuickArcSettingsScreen(
    enabled: Boolean,
    style: QuickArcStyle,
    arcSize: QuickArcSize,
    items: List<QuickArcAction>,
    onEnabledChange: (Boolean) -> Unit,
    onStyleChange: (QuickArcStyle) -> Unit,
    onSizeChange: (QuickArcSize) -> Unit,
    onItemsChange: (List<QuickArcAction>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    val haptics = rememberLightningHaptics()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        SettingsHeader(stringResource(R.string.appearance_quick_arc), onBack)
        Spacer(Modifier.height(12.dp))

        GroupedColumn(1) { _, shape ->
            SettingsSwitchRow(
                title = stringResource(R.string.quick_arc_enable_title),
                summary = stringResource(R.string.appearance_quick_arc_summary),
                checked = enabled,
                onCheckedChange = onEnabledChange,
                shape = shape,
            )
        }

        Spacer(Modifier.height(20.dp))

        Column(modifier = Modifier.alpha(if (enabled) 1f else 0.5f)) {
            // Live preview card
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(234.dp)
                    .background(colors.surfaceContainerHigh, RoundedCornerShape(28.dp))
                    .padding(horizontal = 6.dp, vertical = 14.dp),
            ) {
                QuickArcContent(
                    style = style,
                    arcSize = arcSize,
                    items = items,
                    progress = 1f,
                    alpha = 1f,
                    interactive = false,
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.quick_arc_style_title).uppercase(),
                style = EyebrowTextStyle,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            ExpressiveOptionRow(
                options = listOf(
                    QuickArcStyle.FILLED to stringResource(R.string.quick_arc_style_filled),
                    QuickArcStyle.HOLLOW to stringResource(R.string.quick_arc_style_hollow),
                ),
                selected = style,
                enabled = enabled,
                onSelect = {
                    haptics.tick()
                    onStyleChange(it)
                },
            )

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.quick_arc_size_title).uppercase(),
                style = EyebrowTextStyle,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            ExpressiveOptionRow(
                options = listOf(
                    QuickArcSize.SMALL to stringResource(R.string.quick_arc_size_small),
                    QuickArcSize.MEDIUM to stringResource(R.string.quick_arc_size_medium),
                    QuickArcSize.LARGE to stringResource(R.string.quick_arc_size_large),
                ),
                selected = arcSize,
                enabled = enabled,
                onSelect = {
                    haptics.tick()
                    onSizeChange(it)
                },
            )

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.quick_arc_items_title).uppercase(),
                style = EyebrowTextStyle,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.quick_arc_items_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(8.dp))

            GroupedColumn(ALL_ARC_OPTIONS.size) { i, shape ->
                val option = ALL_ARC_OPTIONS[i]
                val checked = option.action in items
                val canToggle = enabled && ((checked && items.size > 1) || (!checked && items.size < 6))
                SettingsSwitchRow(
                    title = stringResource(option.labelRes),
                    summary = "",
                    checked = checked,
                    enabled = canToggle,
                    onCheckedChange = { wantEnable ->
                        if (wantEnable) {
                            if (!checked && items.size < 6) {
                                onItemsChange(items + option.action)
                            }
                        } else {
                            if (checked && items.size > 1) {
                                onItemsChange(items.filterNot { it == option.action })
                            }
                        }
                    },
                    shape = shape,
                )
            }
        }
    }
}

@Composable
private fun <T> ExpressiveOptionRow(
    options: List<Pair<T, String>>,
    selected: T,
    enabled: Boolean,
    onSelect: (T) -> Unit,
) {
    val colors = LightningTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        options.forEachIndexed { index, (value, label) ->
            val isSelected = value == selected
            val inner by animateDpAsState(
                if (isSelected) 24.dp else 12.dp,
                LightningMotion.defaultSpatial(),
                label = "arcOptRadius",
            )
            val container by animateColorAsState(
                if (isSelected) colors.primary else colors.surfaceContainerHigh,
                LightningMotion.defaultEffects(),
                label = "arcOptBg",
            )
            val content by animateColorAsState(
                if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                LightningMotion.defaultEffects(),
                label = "arcOptFg",
            )
            val last = options.lastIndex
            val shape = RoundedCornerShape(
                topStart = if (index == 0) 24.dp else inner,
                bottomStart = if (index == 0) 24.dp else inner,
                topEnd = if (index == last) 24.dp else inner,
                bottomEnd = if (index == last) 24.dp else inner,
            )
            Surface(
                onClick = { if (enabled) onSelect(value) },
                shape = shape,
                color = container,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = content,
                    )
                }
            }
        }
    }
}
