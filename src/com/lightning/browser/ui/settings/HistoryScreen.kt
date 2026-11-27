package com.lightning.browser.ui.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lightning.browser.R
import com.lightning.browser.ui.components.GroupedColumn
import com.lightning.browser.ui.components.ShapedIcon
import com.lightning.browser.ui.home.ChipTone
import com.lightning.browser.ui.theme.EyebrowTextStyle
import com.lightning.browser.ui.theme.ExpressiveShapes
import com.lightning.browser.ui.theme.LightningColors
import com.lightning.browser.ui.theme.LightningTheme

@Composable
fun HistoryScreen(
    history: List<HistoryEntry>,
    onOpen: (HistoryEntry) -> Unit,
    onDelete: (HistoryEntry) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        ClearHistoryConfirmDialog(
            onDismiss = { showClearConfirm = false },
            onConfirm = {
                showClearConfirm = false
                onClear()
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        SettingsHeader(stringResource(R.string.history_title), onBack) {
            TextButton(
                onClick = { showClearConfirm = true },
                enabled = history.isNotEmpty(),
            ) {
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
                    GroupedColumn(entries.size) { i, shape ->
                        val entry = entries[i]
                        key(entry) {
                            SwipeToDeleteBox(onDelete = { onDelete(entry) }, shape = shape) {
                                HistoryRow(
                                    entry = entry,
                                    onClick = { onOpen(entry) },
                                    shape = shape,
                                    index = i,
                                )
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
private fun HistoryRow(
    entry: HistoryEntry,
    onClick: () -> Unit,
    shape: Shape = ExpressiveShapes.grouped(0, 1),
    index: Int = 0,
) {
    val colors = LightningTheme.colors
    val (container, onContainer) = colors.chipColors(entry.tone)
    Surface(
        onClick = onClick,
        shape = shape,
        color = colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            ShapedIcon(
                entry.icon,
                ExpressiveShapes.cycle(index),
                container,
                onContainer,
                size = 40.dp,
                iconSize = 20.dp,
            )
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

@Composable
private fun ClearHistoryConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = LightningTheme.colors
    val errorColor = MaterialTheme.colorScheme.error
    val onErrorColor = MaterialTheme.colorScheme.onError
    val errorContainer = MaterialTheme.colorScheme.errorContainer
    val onErrorContainer = MaterialTheme.colorScheme.onErrorContainer

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                ShapedIcon(
                    icon = Icons.Filled.DeleteSweep,
                    shape = ExpressiveShapes.Burst8,
                    container = errorContainer,
                    content = onErrorContainer,
                    size = 52.dp,
                    iconSize = 26.dp,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.history_clear_confirm_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.history_clear_confirm_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outline),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.history_clear_cancel_action),
                            color = colors.onSurface,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = onConfirm,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = errorColor,
                            contentColor = onErrorColor,
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.history_clear_confirm_action),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

private fun LightningColors.chipColors(tone: ChipTone): Pair<Color, Color> =
    when (tone) {
        ChipTone.PRIMARY -> primaryContainer to onPrimaryContainer
        ChipTone.SECONDARY -> secondaryContainer to onSecondaryContainer
        ChipTone.TERTIARY -> tertiaryContainer to onTertiaryContainer
    }