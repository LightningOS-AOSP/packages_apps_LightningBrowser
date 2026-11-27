package com.lightning.browser.ui.root

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.components.ShapedIcon
import com.lightning.browser.ui.theme.ExpressiveShapes
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.tone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMenuSheet(
    visible: Boolean,
    onHide: () -> Unit,
    onOpen: (LightningScreen) -> Unit,
    onCopyUrl: () -> Unit = {},
    onReload: () -> Unit = {},
    onShare: () -> Unit = {},
    onFindInPage: () -> Unit = {},
    onToggleDesktop: () -> Unit = {},
    desktopMode: Boolean = false,
    onIncognito: () -> Unit = {},
    incognito: Boolean = false,
    copyUrlVisible: Boolean = false,
) {
    if (visible) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = onHide,
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp),
            containerColor = LightningTheme.colors.surfaceContainerLow,
        ) {
            val actions = buildList {
                if (copyUrlVisible) {
                    add(
                        MenuAction(Icons.Filled.Refresh, stringResource(R.string.menu_reload)) {
                            onReload()
                            onHide()
                        },
                    )
                    add(
                        MenuAction(Icons.Filled.ContentCopy, stringResource(R.string.menu_copy_url)) {
                            onCopyUrl()
                            onHide()
                        },
                    )
                    add(
                        MenuAction(Icons.Filled.Share, stringResource(R.string.menu_share)) {
                            onShare()
                            onHide()
                        },
                    )
                    add(
                        MenuAction(Icons.Filled.FindInPage, stringResource(R.string.menu_find_in_page)) {
                            onFindInPage()
                            onHide()
                        },
                    )
                    add(
                        MenuAction(
                            icon = Icons.Filled.DesktopWindows,
                            title = stringResource(R.string.menu_desktop_site),
                            active = desktopMode,
                        ) {
                            onToggleDesktop()
                            onHide()
                        },
                    )
                }
                if (!incognito) {
                    add(
                        MenuAction(Icons.Filled.VisibilityOff, stringResource(R.string.menu_incognito)) {
                            onIncognito()
                            onHide()
                        },
                    )
                }
                add(
                    MenuAction(Icons.Filled.Bookmark, stringResource(R.string.bookmarks_title)) {
                        onOpen(LightningScreen.BOOKMARKS)
                    },
                )
                add(
                    MenuAction(Icons.Filled.History, stringResource(R.string.history_title)) {
                        onOpen(LightningScreen.HISTORY)
                    },
                )
                add(
                    MenuAction(Icons.Filled.FileDownload, stringResource(R.string.settings_downloads)) {
                        onOpen(LightningScreen.DOWNLOADS)
                    },
                )
                add(
                    MenuAction(Icons.Filled.Settings, stringResource(R.string.settings_title)) {
                        onOpen(LightningScreen.SETTINGS)
                    },
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                actions.chunked(2).forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEachIndexed { columnIndex, action ->
                            ActionTile(
                                action = action,
                                index = rowIndex * 2 + columnIndex,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

private data class MenuAction(
    val icon: ImageVector,
    val title: String,
    val active: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
private fun ActionTile(action: MenuAction, index: Int, modifier: Modifier = Modifier) {
    val colors = LightningTheme.colors
    val tone = colors.tone(index % 3)
    Surface(
        onClick = action.onClick,
        shape = ExpressiveShapes.diagonal(index, 32, 10),
        color = tone.container,
        border = if (action.active) BorderStroke(1.5.dp, tone.content) else null,
        modifier = modifier.height(88.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                ShapedIcon(
                    action.icon,
                    ExpressiveShapes.cycle(index),
                    colors.surface.copy(alpha = 0.6f),
                    tone.content,
                    size = 40.dp,
                    iconSize = 20.dp,
                )
                if (action.active) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(tone.content),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = tone.container,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
            Text(
                text = action.title,
                style = MaterialTheme.typography.labelLarge,
                color = tone.content,
                maxLines = 1,
            )
        }
    }
}