package com.lightning.browser.ui.bar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.theme.ExpressiveShapes
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.rememberLightningHaptics

/**
 * Semi-sphere radial quick menu anchored above the center lightning button.
 * Smoothly blooms upward with Home, All tabs, and Settings actions.
 */
@Composable
fun QuickArcMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    onHome: () -> Unit,
    onTabs: () -> Unit,
    onSettings: () -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f),
        label = "quickArcProgress",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(180),
        label = "quickArcAlpha",
    )
    if (!visible && progress <= 0.01f && alpha <= 0.01f) return

    val colors = LightningTheme.colors
    val scrimInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.34f * alpha.coerceIn(0f, 1f)))
            .clickable(
                interactionSource = scrimInteraction,
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 86.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                color = colors.surfaceContainerHighest,
                shape = RoundedCornerShape(
                    topStart = 156.dp,
                    topEnd = 156.dp,
                    bottomStart = 28.dp,
                    bottomEnd = 28.dp,
                ),
                shadowElevation = 18.dp,
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier
                    .size(width = 312.dp, height = 176.dp)
                    .graphicsLayer {
                        val s = (0.28f + 0.72f * progress).coerceAtLeast(0.01f)
                        scaleX = s
                        scaleY = s
                        this.alpha = alpha.coerceIn(0f, 1f)
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    },
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Subtle anchor pill at the base pointing to the lightning button.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                            .size(width = 32.dp, height = 4.dp)
                            .background(colors.onSurfaceVariant.copy(alpha = 0.25f), CircleShape),
                    )

                    QuickArcItem(
                        icon = Icons.Filled.Home,
                        label = stringResource(R.string.quick_arc_home),
                        shape = ExpressiveShapes.Cookie9,
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer,
                        targetX = (-86).dp,
                        targetY = (-30).dp,
                        progress = progress,
                        alpha = alpha,
                        onClick = onHome,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 18.dp),
                    )
                    QuickArcItem(
                        icon = Icons.Filled.Layers,
                        label = stringResource(R.string.quick_arc_tabs),
                        shape = ExpressiveShapes.Burst8,
                        containerColor = colors.secondaryContainer,
                        contentColor = colors.onSecondaryContainer,
                        targetX = 0.dp,
                        targetY = (-68).dp,
                        progress = progress,
                        alpha = alpha,
                        onClick = onTabs,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 18.dp),
                    )
                    QuickArcItem(
                        icon = Icons.Filled.Settings,
                        label = stringResource(R.string.quick_arc_settings),
                        shape = ExpressiveShapes.Cookie9,
                        containerColor = colors.tertiaryContainer,
                        contentColor = colors.onTertiaryContainer,
                        targetX = 86.dp,
                        targetY = (-30).dp,
                        progress = progress,
                        alpha = alpha,
                        onClick = onSettings,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickArcItem(
    icon: ImageVector,
    label: String,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    targetX: Dp,
    targetY: Dp,
    progress: Float,
    alpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    val haptics = rememberLightningHaptics()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .offset(x = targetX * progress, y = targetY * progress)
            .graphicsLayer {
                val s = (0.4f + 0.6f * progress).coerceAtLeast(0.01f)
                scaleX = s
                scaleY = s
                this.alpha = alpha.coerceIn(0f, 1f)
            },
    ) {
        Surface(
            onClick = {
                haptics.click()
                onClick()
            },
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            shadowElevation = 4.dp,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = colors.onSurface,
            maxLines = 1,
        )
    }
}
