package com.lightning.browser.ui.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.home.ChipTone
import com.lightning.browser.ui.theme.EyebrowTextStyle
import com.lightning.browser.ui.theme.LightningColors
import com.lightning.browser.ui.theme.LightningTheme

@Composable
fun HistoryScreen(
    history: List<HistoryEntry>,
    onDelete: (HistoryEntry) -> Unit,
    onClear: () -> Unit,
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
        SettingsHeader(stringResource(R.string.history_title), onBack) {
            TextButton(onClick = onClear, enabled = history.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.history_clear),
                    color = colors.primary,
                )
            }
        }
        if (history.isEmpty()) {
            EmptyState()
        } else {
            Spacer(Modifier.height(4.dp))
            historyGroupOrder.forEach { groupRes ->
                val entries = history.filter { it.groupRes == groupRes }
                if (entries.isNotEmpty()) {
                    Text(
                        text = stringResource(groupRes).uppercase(),
                        style = EyebrowTextStyle,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = colors.surfaceContainer,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            entries.forEach { entry ->
                                SwipeToDeleteBox(onDelete = { onDelete(entry) }) {
                                    HistoryRow(entry)
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry) {
    val colors = LightningTheme.colors
    val (container, onContainer) = colors.chipColors(entry.tone)
    Surface(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Surface(
                color = container,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = null,
                        tint = onContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = entry.domain,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    val colors = LightningTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Article,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.history_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

private fun LightningColors.chipColors(tone: ChipTone): Pair<Color, Color> =
    when (tone) {
        ChipTone.PRIMARY -> primaryContainer to onPrimaryContainer
        ChipTone.SECONDARY -> secondaryContainer to onSecondaryContainer
        ChipTone.TERTIARY -> tertiaryContainer to onTertiaryContainer
    }