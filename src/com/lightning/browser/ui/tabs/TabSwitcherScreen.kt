package com.lightning.browser.ui.tabs

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.TabUnselected
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import com.lightning.browser.R
import com.lightning.browser.ui.components.ShapedIcon
import com.lightning.browser.ui.theme.EyebrowTextStyle
import com.lightning.browser.ui.theme.ExpressiveShapes
import com.lightning.browser.ui.theme.LightningTheme

@Composable
fun TabSwitcherScreen(
    tabs: SnapshotStateList<BrowserTab>,
    thumbnails: Map<String, Bitmap> = emptyMap(),
    onCloseTab: (Int) -> Unit = {},
    onCloseAll: () -> Unit = {},
    onNewTab: () -> Unit = {},
    onOpenTab: (Int) -> Unit = {},
) {
    val colors = LightningTheme.colors
    var showCloseAllConfirm by remember { mutableStateOf(false) }

    if (showCloseAllConfirm) {
        CloseAllTabsConfirmDialog(
            onDismiss = { showCloseAllConfirm = false },
            onConfirm = {
                showCloseAllConfirm = false
                onCloseAll()
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.tabs_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.onSurface,
                )
                Spacer(Modifier.width(12.dp))
                Surface(
                    shape = CircleShape,
                    color = colors.primaryContainer,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Text(
                        text = tabs.size.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { showCloseAllConfirm = true },
                    enabled = tabs.isNotEmpty(),
                ) {
                    Text(
                        text = stringResource(R.string.tabs_close_all),
                        style = EyebrowTextStyle,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            Crossfade(
                targetState = tabs.isEmpty(),
                animationSpec = tween(220),
                label = "tabsEmptyCrossfade",
                modifier = Modifier.fillMaxSize(),
            ) { isEmpty ->
                if (isEmpty) {
                    EmptyTabs()
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(
                            start = 20.dp, top = 8.dp, end = 20.dp, bottom = 96.dp,
                        ),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(tabs, key = { _, tab -> tab.id }) { index, tab ->
                            TabCard(
                                tab = tab,
                                index = index,
                                thumbnail = thumbnails[tab.id],
                                onClose = { onCloseTab(index) },
                                onOpen = { onOpenTab(index) },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(220),
                                    fadeOutSpec = tween(180),
                                    placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                ),
                            )
                        }
                    }
                }
            }
        }

        LargeFloatingActionButton(
            onClick = onNewTab,
            shape = ExpressiveShapes.Cookie9,
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = stringResource(R.string.bar_new_tab),
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
private fun EmptyTabs() {
    val colors = LightningTheme.colors
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "No tabs yet",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun TabCard(
    tab: BrowserTab,
    index: Int,
    thumbnail: Bitmap? = null,
    onClose: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    // Private tabs get the violet secondary wash so they stand out from the
    // normal surface ones.
    val cardColor = if (tab.private) colors.secondaryContainer else colors.surfaceContainerHigh
    val onCardColor = if (tab.private) colors.onSecondaryContainer else colors.onSurface

    var appeared by remember(tab.id) { mutableStateOf(false) }
    var closing by remember(tab.id) { mutableStateOf(false) }
    LaunchedEffect(tab.id) { appeared = true }
    LaunchedEffect(closing) {
        if (closing) {
            delay(180L)
            onClose()
        }
    }
    val scale by animateFloatAsState(
        targetValue = when {
            closing -> 0.82f
            appeared -> 1f
            else -> 0.88f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabCardScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (closing || !appeared) 0f else 1f,
        animationSpec = tween(180),
        label = "tabCardAlpha",
    )

    Surface(
        onClick = onOpen,
        shape = ExpressiveShapes.diagonal(index, big = 32, small = 10),
        color = cardColor,
        contentColor = onCardColor,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        },
    ) {
        Column {
            // Thumbnail placeholder until real WebView captures land.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(colors.surfaceContainerHighest),
            ) {
                if (tab.url.isNotEmpty() && thumbnail != null) {
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.FlashOn,
                        contentDescription = null,
                        tint = if (tab.private) colors.primary else colors.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp),
                    )
                }
                Surface(
                    onClick = { closing = true },
                    color = colors.surface.copy(alpha = 0.85f),
                    shape = ExpressiveShapes.Burst8,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(30.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.tabs_close_tab),
                            tint = colors.onSurface,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                ShapedIcon(
                    Icons.Filled.FlashOn,
                    ExpressiveShapes.Cookie9,
                    colors.primaryContainer,
                    colors.onPrimaryContainer,
                    size = 28.dp,
                    iconSize = 14.dp,
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                    if (tab.domain.isNotEmpty()) {
                        Text(
                            text = tab.domain,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CloseAllTabsConfirmDialog(
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
                    icon = Icons.Filled.TabUnselected,
                    shape = ExpressiveShapes.Burst8,
                    container = errorContainer,
                    content = onErrorContainer,
                    size = 52.dp,
                    iconSize = 26.dp,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.tabs_close_all_confirm_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.tabs_close_all_confirm_message),
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
                            text = stringResource(R.string.tabs_close_all_cancel_action),
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
                            text = stringResource(R.string.tabs_close_all_confirm_action),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}