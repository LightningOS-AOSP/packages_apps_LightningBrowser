package com.lightning.browser.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lightning.browser.ui.theme.LightningMotion
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.LobedShape

@Composable
fun Modifier.pressScale(source: InteractionSource, pressed: Float = 0.94f): Modifier {
    val isPressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(
        if (isPressed) pressed else 1f,
        LightningMotion.fastSpatial(),
        label = "pressScale",
    )
    return this.graphicsLayer {
        scaleX = s
        scaleY = s
    }
}

/** Icon inside a shaped container. */
@Composable
fun ShapedIcon(
    icon: ImageVector,
    shape: Shape,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = content, modifier = Modifier.size(iconSize))
    }
}

/** Shape-morphing primary button: cookie at rest, circle + 40deg twist while pressed. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MorphButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    icon: ImageVector = Icons.Filled.FlashOn,
    container: Color = LightningTheme.colors.primary,
    content: Color = LightningTheme.colors.onPrimary,
    lobes: Int = 9,
    onLongClick: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val amp by animateFloatAsState(
        if (pressed) 0f else 0.06f,
        LightningMotion.fastSpatial(),
        label = "morphAmp",
    )
    val rot by animateFloatAsState(
        if (pressed) 40f else 0f,
        LightningMotion.defaultSpatial(),
        label = "morphRot",
    )
    Box(
        modifier = modifier
            .size(size)
            .pressScale(source)
            .clip(LobedShape(lobes, amp.coerceAtLeast(0f), rot))
            .background(container)
            .combinedClickable(
                interactionSource = source,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick?.let { callback ->
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        callback()
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = content, modifier = Modifier.size(size * 0.45f))
    }
}

/** Round icon button: tonal wash fades in on press. */
@Composable
fun TonalIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    tint: Color = LightningTheme.colors.onSurface,
) {
    val colors = LightningTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val wash by animateColorAsState(
        if (pressed) colors.secondaryContainer else Color.Transparent,
        LightningMotion.defaultEffects(),
        label = "wash",
    )
    Box(
        modifier = modifier
            .size(size)
            .pressScale(source)
            .clip(CircleShape)
            .background(wash)
            .clickable(
                enabled = enabled,
                interactionSource = source,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.38f),
            modifier = Modifier.size(iconSize),
        )
    }
}