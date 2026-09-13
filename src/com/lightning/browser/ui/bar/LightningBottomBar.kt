package com.lightning.browser.ui.bar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.theme.LightningTheme

private val BAR_HEIGHT = 64.dp

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

    Box(modifier.height(BAR_HEIGHT)) {
        Surface(
            color = colors.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {}
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            BarIconButton(Icons.AutoMirrored.Filled.ArrowBack, R.string.bar_back, backEnabled, onBack)
            BarIconButton(Icons.Filled.BookmarkBorder, R.string.bar_bookmark, true, onBookmark)
            Spacer(Modifier.weight(1f))
            BarIconButton(Icons.Filled.MoreVert, R.string.bar_menu, true, onMenu)
            BarIconButton(Icons.AutoMirrored.Filled.ArrowForward, R.string.bar_forward, forwardEnabled, onForward)
        }
        // FAB hovers above the bar, overlapping its top edge.
        FloatingActionButton(
            onClick = onNewTab,
            shape = RoundedCornerShape(20.dp),
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(8.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-10).dp)
                .size(56.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.FlashOn,
                contentDescription = stringResource(R.string.bar_new_tab),
            )
        }
    }
}

@Composable
private fun BarIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, descRes: Int, enabled: Boolean, onClick: () -> Unit) {
    val colors = LightningTheme.colors
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(descRes),
            tint = if (enabled) colors.onSurface else colors.onSurfaceVariant,
        )
    }
}