package com.lightning.browser.ui.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.home.ChipTone
import com.lightning.browser.ui.theme.LightningColors
import com.lightning.browser.ui.theme.LightningTheme

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
            shape = RoundedCornerShape(50),
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
                Surface(
                    color = colors.surfaceContainer,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(8.dp)) {
                        folderBookmarks
                            .filter {
                                query.isBlank() || it.title.contains(query, ignoreCase = true) ||
                                    it.domain.contains(query, ignoreCase = true)
                            }
                            .forEach { bookmark ->
                                SwipeToDeleteBox(onDelete = { onDelete(bookmark) }) {
                                    BookmarkRow(bookmark, onClick = { onOpen(bookmark) })
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                    }
                }
            }
        } else if (bookmarks.isEmpty()) {
            EmptyState()
        } else {
            Surface(
                color = colors.surfaceContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(8.dp)) {
                    folders.forEach { folder ->
                        FolderRow(
                            folder = BookmarkFolder(folder, bookmarks.count { it.folder == folder }),
                            onClick = { openFolder = folder },
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    val rootList = bookmarks.filter { it.folder.isBlank() }
                    if (folders.isNotEmpty() && rootList.isNotEmpty()) {
                        SettingsDivider()
                        Spacer(Modifier.height(4.dp))
                    }
                    rootList
                        .filter {
                            query.isBlank() || it.title.contains(query, ignoreCase = true) ||
                                it.domain.contains(query, ignoreCase = true)
                        }
                        .forEach { bookmark ->
                            SwipeToDeleteBox(onDelete = { onDelete(bookmark) }) {
                                BookmarkRow(bookmark, onClick = { onOpen(bookmark) })
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                }
            }
        }
    }
}

@Composable
private fun FolderRow(folder: BookmarkFolder, onClick: () -> Unit) {
    val colors = LightningTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Surface(
                color = colors.surfaceContainerHigh,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
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
private fun BookmarkRow(bookmark: Bookmark, onClick: () -> Unit) {
    val colors = LightningTheme.colors
    val (container, onContainer) = colors.chipColors(bookmark.tone)
    Surface(
        onClick = onClick,
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
                        imageVector = bookmark.icon,
                        contentDescription = null,
                        tint = onContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
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