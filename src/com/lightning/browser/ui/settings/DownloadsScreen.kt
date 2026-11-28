package com.lightning.browser.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lightning.browser.R
import com.lightning.browser.ui.components.GroupedColumn
import com.lightning.browser.ui.components.ShapedIcon
import com.lightning.browser.ui.browser.BrowserDownloads
import com.lightning.browser.ui.theme.EyebrowTextStyle
import com.lightning.browser.ui.theme.ExpressiveShapes
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.tone
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay

@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    val context = LocalContext.current
    var items by remember { mutableStateOf(emptyList<DownloadItem>()) }
    var hasAccess by remember { mutableStateOf(BrowserDownloads.hasFileAccess(context)) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var selectedItem by remember { mutableStateOf<DownloadItem?>(null) }

    LaunchedEffect(context, refreshKey) {
        while (true) {
            items = BrowserDownloads.queryAll(context)
            hasAccess = BrowserDownloads.hasFileAccess(context)
            val active = items.any { it.state != DownloadState.COMPLETE }
            delay(if (active) 1_000L else 4_000L)
        }
    }

    val groups = remember(items) { groupByDate(items) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        SettingsHeader(stringResource(R.string.downloads_title), onBack)
        Spacer(Modifier.height(8.dp))
        if (!hasAccess) {
            StorageAccessCard(onRequest = { BrowserDownloads.requestFileAccess(context) })
            Spacer(Modifier.height(20.dp))
        }
        if (groups.isEmpty()) {
            DownloadsEmpty()
        } else {
            groups.forEach { (labelRes, list) ->
                Text(
                    text = stringResource(labelRes).uppercase(),
                    style = EyebrowTextStyle,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )
                Spacer(Modifier.height(4.dp))
                GroupedColumn(list.size) { i, shape ->
                    val entry = list[i]
                    DownloadRow(
                        item = entry,
                        onClick = { if (entry.state == DownloadState.COMPLETE) selectedItem = entry },
                        onPause = {
                            BrowserDownloads.pause(context, entry.downloadId)
                            refreshKey++
                        },
                        onResume = {
                            BrowserDownloads.resume(context, entry.downloadId)
                            refreshKey++
                        },
                        onCancel = {
                            BrowserDownloads.cancel(context, entry.downloadId)
                            refreshKey++
                        },
                        onRetry = {
                            BrowserDownloads.retry(context, entry.downloadId)
                            refreshKey++
                        },
                        shape = shape,
                        index = i,
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    selectedItem?.let { item ->
        DownloadOptionsDialog(
            item = item,
            onDismiss = { selectedItem = null },
            onOpen = {
                selectedItem = null
                BrowserDownloads.openDownload(context, item)
            },
            onLocate = {
                selectedItem = null
                BrowserDownloads.locateDownload(context, item)
            },
            onShare = {
                selectedItem = null
                BrowserDownloads.shareDownload(context, item)
            },
            onDeleteHistory = {
                selectedItem = null
                BrowserDownloads.deleteHistoryOnly(context, item)
                refreshKey++
            },
            onDeleteBoth = {
                selectedItem = null
                BrowserDownloads.deleteWithStorage(context, item)
                refreshKey++
            },
        )
    }
}

@Composable
private fun StorageAccessCard(
    onRequest: () -> Unit,
) {
    val colors = LightningTheme.colors
    Surface(
        color = colors.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            ShapedIcon(
                Icons.Filled.FolderOpen,
                ExpressiveShapes.Clover4,
                colors.primaryContainer,
                colors.onPrimaryContainer,
                size = 40.dp,
                iconSize = 20.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.downloads_storage_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.downloads_storage_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onRequest) {
                Text(
                    text = stringResource(R.string.downloads_storage_grant),
                    color = colors.primary,
                )
            }
        }
    }
}

@Composable
private fun DownloadsEmpty() {
    val colors = LightningTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
    ) {
        Surface(
            color = colors.surfaceContainerHigh,
            shape = CircleShape,
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.FileDownload,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.downloads_empty),
            style = MaterialTheme.typography.titleSmall,
            color = colors.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.downloads_empty_detail),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun DownloadRow(
    item: DownloadItem,
    onClick: () -> Unit = {},
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onCancel: () -> Unit = {},
    onRetry: () -> Unit = {},
    shape: Shape = ExpressiveShapes.grouped(0, 1),
    index: Int = 0,
) {
    val colors = LightningTheme.colors
    val tone = colors.tone(index % 3)
    Surface(
        onClick = onClick,
        enabled = item.state == DownloadState.COMPLETE,
        shape = shape,
        color = colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShapedIcon(
                    Icons.Filled.FileDownload,
                    ExpressiveShapes.cycle(index),
                    tone.container,
                    tone.content,
                    size = 40.dp,
                    iconSize = 20.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val missingLabel = stringResource(R.string.download_file_missing)
                    val isMissing = item.detail == missingLabel
                    val statusText = item.detail
                    Text(
                        text = if (item.domain.isNotEmpty()) "${item.domain} · $statusText" else statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isMissing) MaterialTheme.colorScheme.error else colors.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(4.dp))
                when (item.state) {
                    DownloadState.DOWNLOADING -> {
                        Text(
                            text = item.progressPercent(),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                        )
                        IconButton(onClick = onPause, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Pause,
                                contentDescription = stringResource(R.string.download_pause),
                                tint = colors.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.download_cancel),
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    DownloadState.PAUSED -> {
                        Text(
                            text = if (item.detail.startsWith("Waiting")) item.detail else "Paused",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                        )
                        IconButton(onClick = onResume, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.download_resume),
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.download_cancel),
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    DownloadState.COMPLETE -> {
                        if (item.detail == "Cancelled" || item.detail == "Failed") {
                            IconButton(onClick = onRetry, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = stringResource(R.string.download_retry),
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
            if (item.state != DownloadState.COMPLETE) {
                Spacer(Modifier.height(10.dp))
                val animatedProgress by animateFloatAsState(
                    targetValue = item.progress.coerceIn(0f, 1f),
                    animationSpec = tween(250),
                    label = "downloadProgress",
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    color = if (item.state == DownloadState.PAUSED) colors.onSurfaceVariant else colors.primary,
                    trackColor = colors.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                )
            }
        }
    }
}

@Composable
private fun DownloadOptionsDialog(
    item: DownloadItem,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onLocate: () -> Unit,
    onShare: () -> Unit,
    onDeleteHistory: () -> Unit,
    onDeleteBoth: () -> Unit,
) {
    val colors = LightningTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShapedIcon(
                        icon = Icons.Filled.FileDownload,
                        shape = ExpressiveShapes.Cookie9,
                        container = colors.primaryContainer,
                        content = colors.onPrimaryContainer,
                        size = 44.dp,
                        iconSize = 22.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (item.domain.isNotEmpty()) "${item.domain} · ${item.detail}" else item.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                val isMissing = item.detail == stringResource(R.string.download_file_missing)
                if (isMissing) {
                    GroupedColumn(1) { _, shape ->
                        DownloadActionRow(
                            icon = Icons.Filled.Delete,
                            label = stringResource(R.string.download_action_delete_history),
                            shape = shape,
                            onClick = onDeleteHistory,
                        )
                    }
                } else {
                    GroupedColumn(5) { idx, shape ->
                        when (idx) {
                            0 -> DownloadActionRow(
                                icon = Icons.AutoMirrored.Filled.OpenInNew,
                                label = stringResource(R.string.download_action_open),
                                shape = shape,
                                onClick = onOpen,
                            )
                            1 -> DownloadActionRow(
                                icon = Icons.Filled.FolderOpen,
                                label = stringResource(R.string.download_action_locate),
                                shape = shape,
                                onClick = onLocate,
                            )
                            2 -> DownloadActionRow(
                                icon = Icons.Filled.Share,
                                label = stringResource(R.string.download_action_share),
                                shape = shape,
                                onClick = onShare,
                            )
                            3 -> DownloadActionRow(
                                icon = Icons.Filled.Delete,
                                label = stringResource(R.string.download_action_delete_history),
                                shape = shape,
                                onClick = onDeleteHistory,
                            )
                            4 -> DownloadActionRow(
                                icon = Icons.Filled.DeleteForever,
                                label = stringResource(R.string.download_action_delete_both),
                                destructive = true,
                                shape = shape,
                                onClick = onDeleteBoth,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadActionRow(
    icon: ImageVector,
    label: String,
    shape: Shape,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = LightningTheme.colors
    val tint = if (destructive) MaterialTheme.colorScheme.error else colors.onSurface
    Surface(
        onClick = onClick,
        shape = shape,
        color = colors.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = tint,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun DownloadItem.progressPercent(): String = "${(progress * 100).toInt()}%"

private fun groupByDate(items: List<DownloadItem>): List<Pair<Int, List<DownloadItem>>> {
    if (items.isEmpty()) return emptyList()
    val zone = ZoneId.systemDefault()
    fun startOfDay(daysAgo: Int): Long =
        LocalDate.now().minusDays(daysAgo.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
    val today = startOfDay(0)
    val yesterday = startOfDay(1)
    val todayItems = items.filter { it.timestamp >= today }
    val yesterdayItems = items.filter { it.timestamp in yesterday until today }
    val earlierItems = items.filter { it.timestamp < yesterday }
    return listOf(
        R.string.downloads_group_today to todayItems,
        R.string.downloads_group_yesterday to yesterdayItems,
        R.string.downloads_group_earlier to earlierItems,
    ).filter { it.second.isNotEmpty() }
}