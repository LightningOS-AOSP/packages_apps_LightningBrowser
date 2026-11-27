package com.lightning.browser.ui.bar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.components.MorphButton
import com.lightning.browser.ui.components.TonalIconButton
import com.lightning.browser.ui.theme.LightningTheme

@Composable
fun LightningBottomBar(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onForward: () -> Unit = {},
    onBookmark: () -> Unit = {},
    onMenu: () -> Unit = {},
    onNewTab: () -> Unit = {},
    onLightningLongClick: () -> Unit = {},
    backEnabled: Boolean = false,
    forwardEnabled: Boolean = false,
) {
    val colors = LightningTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = colors.surfaceContainerHighest,
            shape = RoundedCornerShape(36.dp),
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
            ) {
                TonalIconButton(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    stringResource(R.string.bar_back),
                    onBack,
                    enabled = backEnabled,
                )
                TonalIconButton(
                    Icons.Filled.BookmarkBorder,
                    stringResource(R.string.bar_bookmark),
                    onBookmark,
                )
                MorphButton(
                    onClick = onNewTab,
                    contentDescription = stringResource(R.string.bar_new_tab),
                    size = 60.dp,
                    onLongClick = onLightningLongClick,
                )
                TonalIconButton(
                    Icons.Filled.MoreVert,
                    stringResource(R.string.bar_menu),
                    onMenu,
                )
                TonalIconButton(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    stringResource(R.string.bar_forward),
                    onForward,
                    enabled = forwardEnabled,
                )
            }
        }
    }
}