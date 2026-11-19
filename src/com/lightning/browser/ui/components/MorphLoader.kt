package com.lightning.browser.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.LobedShape

@Composable
fun MorphLoader(description: String, modifier: Modifier = Modifier, size: Dp = 96.dp) {
    val colors = LightningTheme.colors
    val t = rememberInfiniteTransition(label = "loader")
    val rot by t.animateFloat(
        0f,
        360f,
        infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "rot",
    )
    val amp by t.animateFloat(
        0.03f,
        0.13f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "amp",
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rot }
                .clip(LobedShape(7, amp))
                .background(colors.primaryContainer),
        )
        Box(
            Modifier
                .size(size * 0.62f)
                .graphicsLayer { rotationZ = -rot * 0.6f }
                .clip(LobedShape(5, amp * 1.2f))
                .background(colors.primary),
        )
        Icon(
            Icons.Filled.FlashOn,
            description,
            tint = colors.onPrimary,
            modifier = Modifier.size(size * 0.3f),
        )
    }
}