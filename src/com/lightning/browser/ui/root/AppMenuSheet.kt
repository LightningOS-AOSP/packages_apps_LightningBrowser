package com.lightning.browser.ui.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.theme.LightningTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMenuSheet(
    visible: Boolean,
    onHide: () -> Unit,
    onOpen: (LightningScreen) -> Unit,
) {
    if (visible) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = onHide,
            sheetState = sheetState,
            containerColor = LightningTheme.colors.surfaceContainer,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
            ) {
                SheetItem(Icons.Filled.Bookmark, stringResource(R.string.bookmarks_title), { onOpen(LightningScreen.BOOKMARKS) })
                SheetItem(Icons.Filled.History, stringResource(R.string.history_title), { onOpen(LightningScreen.HISTORY) })
                SheetItem(Icons.Filled.FileDownload, stringResource(R.string.settings_downloads), { onOpen(LightningScreen.DOWNLOADS) })
                SheetItem(Icons.Filled.Settings, stringResource(R.string.settings_title), { onOpen(LightningScreen.SETTINGS) })
            }
        }
    }
}

@Composable
private fun SheetItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    val colors = LightningTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
        ) {
            Surface(
                color = colors.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}