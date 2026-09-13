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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.browser.BrowserDownloads
import com.lightning.browser.ui.theme.EyebrowTextStyle
import com.lightning.browser.ui.theme.LightningTheme
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

    LaunchedEffect(context) {
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
                Surface(
                    color = colors.surfaceContainer,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(8.dp)) {
                        list.forEach { item ->
                            DownloadRow(
                                item = item,
                                onPause = { BrowserDownloads.pause(context, item.downloadId) },
                                onResume = { BrowserDownloads.resume(context, item.downloadId) },
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun StorageAccessCard(
    onRequest: () -> Unit,
) {
    val colors = LightningTheme.colors
    Surface(
        color = colors.surfaceContainer,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Surface(
                color = colors.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
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
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
) {
    val colors = LightningTheme.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = colors.surfaceContainerHigh,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.FileDownload,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "${item.domain} · ${if (item.state != DownloadState.COMPLETE) item.progressPercent() else item.detail}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
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
                    }
                    DownloadState.PAUSED -> {
                        Text(
                            text = "Paused",
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
                    }
                    DownloadState.COMPLETE -> Unit
                }
            }
            if (item.state != DownloadState.COMPLETE) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { item.progress },
                    color = if (item.state == DownloadState.PAUSED) colors.onSurfaceVariant else colors.primary,
                    trackColor = colors.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                )
            }
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