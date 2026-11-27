package com.lightning.browser.ui.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.components.GroupedColumn
import com.lightning.browser.ui.components.ShapedIcon
import com.lightning.browser.ui.home.ChipTone
import com.lightning.browser.ui.theme.ExpressiveShapes
import com.lightning.browser.ui.theme.LightningColors
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.tone

@Composable
fun BookmarksScreen(
    bookmarks: List<Bookmark>,
    onOpen: (Bookmark) -> Unit,
    onDelete: (Bookmark) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    var openFolder by rememberSaveable { mutableStateOf<String?>(null) }

    val folders = bookmarks.map { it.folder }.filter { it.isNotBlank() }.distinct()
    val folderBookmarks = openFolder?.let { name -> bookmarks.filter { it.folder == name } } ?: emptyList()

    BackHandler(enabled = openFolder != null) { openFolder = null }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        SettingsHeader(
            title = openFolder ?: stringResource(R.string.bookmarks_title),
            onBack = if (openFolder != null) { { openFolder = null } } else onBack,
        )
        Spacer(Modifier.height(8.dp))
        TextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = {
                Text(
                    text = stringResource(R.string.bookmarks_search_hint),
                    color = colors.onSurfaceVariant,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                )
            },
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colors.surfaceContainerHigh,
                unfocusedContainerColor = colors.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = colors.primary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        )
        Spacer(Modifier.height(20.dp))

        if (openFolder != null) {
            if (folderBookmarks.isEmpty()) {
                EmptyState()
            } else {
                val visible = folderBookmarks.filter {
                    query.isBlank() || it.title.contains(query, ignoreCase = true) ||
                        it.domain.contains(query, ignoreCase = true)
                }
                GroupedColumn(visible.size) { i, shape ->
                    val bookmark = visible[i]
                    key(bookmark) {
                        SwipeToDeleteBox(onDelete = { onDelete(bookmark) }, shape = shape) {
                            BookmarkRow(
                                bookmark = bookmark,
                                onClick = { onOpen(bookmark) },
                                shape = shape,
                                index = i,
                            )
                        }
                    }
                }
            }
        } else if (bookmarks.isEmpty()) {
            EmptyState()
        } else {
            val rootList = bookmarks.filter { it.folder.isBlank() }.filter {
                query.isBlank() || it.title.contains(query, ignoreCase = true) ||
                    it.domain.contains(query, ignoreCase = true)
            }
            GroupedColumn(folders.size + rootList.size) { i, shape ->
                if (i < folders.size) {
                    val folder = folders[i]
                    FolderRow(
                        folder = BookmarkFolder(folder, bookmarks.count { it.folder == folder }),
                        onClick = { openFolder = folder },
                        shape = shape,
                        index = i,
                    )
                } else {
                    val bookmark = rootList[i - folders.size]
                    key(bookmark) {
                        SwipeToDeleteBox(onDelete = { onDelete(bookmark) }, shape = shape) {
                            BookmarkRow(
                                bookmark = bookmark,
                                onClick = { onOpen(bookmark) },
                                shape = shape,
                                index = i,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderRow(
    folder: BookmarkFolder,
    onClick: () -> Unit,
    shape: Shape = ExpressiveShapes.grouped(0, 1),
    index: Int = 0,
) {
    val colors = LightningTheme.colors
    val tone = colors.tone(index % 3)
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
                Icons.Filled.Folder,
                ExpressiveShapes.cycle(index),
                tone.container,
                tone.content,
                size = 40.dp,
                iconSize = 20.dp,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = folder.count.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun BookmarkRow(
    bookmark: Bookmark,
    onClick: () -> Unit,
    shape: Shape = ExpressiveShapes.grouped(0, 1),
    index: Int = 0,
) {
    val colors = LightningTheme.colors
    val (container, onContainer) = colors.chipColors(bookmark.tone)
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
                bookmark.icon,
                ExpressiveShapes.cycle(index),
                container,
                onContainer,
                size = 40.dp,
                iconSize = 20.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = bookmark.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = bookmark.domain,
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
        Text(
            text = stringResource(R.string.bookmarks_empty),
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