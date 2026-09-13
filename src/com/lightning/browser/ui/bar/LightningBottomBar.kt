package com.lightning.browser.ui.bar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.theme.LightningTheme

private val BAR_HEIGHT = 46.dp

@Composable
fun LightningBottomBar(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onForward: () -> Unit = {},
    onBookmark: () -> Unit = {},
    onMenu: () -> Unit = {},
    onNewTab: () -> Unit = {},
    backEnabled: Boolean = false,
    forwardEnabled: Boolean = false,
) {
    val colors = LightningTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp),
    ) {
        Surface(
            color = colors.surfaceContainer,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxSize(),
            ) {
                BarIconButton(Icons.AutoMirrored.Filled.ArrowBack, R.string.bar_back, backEnabled, onBack)
                BarIconButton(Icons.Filled.BookmarkBorder, R.string.bar_bookmark, true, onBookmark)
                BarIconButton(Icons.Filled.FlashOn, R.string.bar_new_tab, true, onNewTab, colors.primary)
                BarIconButton(Icons.Filled.MoreVert, R.string.bar_menu, true, onMenu)
                BarIconButton(Icons.AutoMirrored.Filled.ArrowForward, R.string.bar_forward, forwardEnabled, onForward)
            }
        }
    }
}

@Composable
private fun BarIconButton(
    icon: ImageVector,
    descRes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    tintOverride: androidx.compose.ui.graphics.Color? = null,
) {
    val colors = LightningTheme.colors
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(42.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(descRes),
            tint = tintOverride ?: if (enabled) colors.onSurface else colors.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}