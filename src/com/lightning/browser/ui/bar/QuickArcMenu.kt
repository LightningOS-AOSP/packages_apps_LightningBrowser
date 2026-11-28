package com.lightning.browser.ui.bar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OfflinePin
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lightning.browser.R
import com.lightning.browser.ui.BrowserStore.QuickArcAction
import com.lightning.browser.ui.BrowserStore.QuickArcSize
import com.lightning.browser.ui.BrowserStore.QuickArcStyle
import com.lightning.browser.ui.theme.ExpressiveShapes
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.rememberLightningHaptics
import kotlin.math.cos
import kotlin.math.sin

private data class QuickArcActionSpec(
    val action: QuickArcAction,
    val icon: ImageVector,
    val labelRes: Int,
    val shape: Shape,
    val toneSlot: Int,
)

private val QUICK_ARC_SPECS = mapOf(
    QuickArcAction.HOME to QuickArcActionSpec(
        QuickArcAction.HOME, Icons.Filled.Home, R.string.quick_arc_home, ExpressiveShapes.Cookie9, 0,
    ),
    QuickArcAction.TABS to QuickArcActionSpec(
        QuickArcAction.TABS, Icons.Filled.Layers, R.string.quick_arc_tabs, ExpressiveShapes.Burst8, 1,
    ),
    QuickArcAction.SETTINGS to QuickArcActionSpec(
        QuickArcAction.SETTINGS, Icons.Filled.Settings, R.string.quick_arc_settings, ExpressiveShapes.Cookie9, 2,
    ),
    QuickArcAction.NEW_TAB to QuickArcActionSpec(
        QuickArcAction.NEW_TAB, Icons.Filled.Add, R.string.quick_arc_new_tab, ExpressiveShapes.Clover4, 0,
    ),
    QuickArcAction.BOOKMARKS to QuickArcActionSpec(
        QuickArcAction.BOOKMARKS, Icons.Filled.Bookmarks, R.string.quick_arc_bookmarks, ExpressiveShapes.Flower6, 1,
    ),
    QuickArcAction.HISTORY to QuickArcActionSpec(
        QuickArcAction.HISTORY, Icons.Filled.History, R.string.quick_arc_history, ExpressiveShapes.Scallop12, 2,
    ),
    QuickArcAction.DOWNLOADS to QuickArcActionSpec(
        QuickArcAction.DOWNLOADS, Icons.Filled.Download, R.string.quick_arc_downloads, ExpressiveShapes.Penta5, 0,
    ),
    QuickArcAction.SAVED_PAGES to QuickArcActionSpec(
        QuickArcAction.SAVED_PAGES, Icons.Filled.OfflinePin, R.string.quick_arc_saved_pages, ExpressiveShapes.Burst8, 1,
    ),
)

@Composable
fun QuickArcMenu(
    visible: Boolean,
    style: QuickArcStyle,
    arcSize: QuickArcSize,
    items: List<QuickArcAction>,
    onDismiss: () -> Unit,
    onSelectAction: (QuickArcAction) -> Unit,
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
                .padding(horizontal = 12.dp)
                .padding(bottom = 86.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            QuickArcContent(
                style = style,
                arcSize = arcSize,
                items = items,
                progress = progress,
                alpha = alpha,
                interactive = true,
                onSelectAction = onSelectAction,
            )
        }
    }
}

@Composable
fun QuickArcContent(
    style: QuickArcStyle,
    arcSize: QuickArcSize,
    items: List<QuickArcAction>,
    progress: Float = 1f,
    alpha: Float = 1f,
    interactive: Boolean = true,
    onSelectAction: (QuickArcAction) -> Unit = {},
) {
    val colors = LightningTheme.colors
    val activeItems = items.ifEmpty { listOf(QuickArcAction.HOME, QuickArcAction.TABS, QuickArcAction.SETTINGS) }

    val desiredWidth = when (arcSize) {
        QuickArcSize.SMALL -> 236.dp
        QuickArcSize.MEDIUM -> 284.dp
        QuickArcSize.LARGE -> 332.dp
    }

    BoxWithConstraints(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val containerWidth = desiredWidth.coerceAtMost(maxWidth)
        val scale = (containerWidth.value / 284f).coerceIn(0.75f, 1.2f)
        val containerHeight = (176f * scale).dp

        val countFactor = if (activeItems.size >= 5) 0.84f else 1f
        val buttonSize = (42f * scale * countFactor).coerceIn(30f, 48f).dp
        val iconSize = (19f * scale * countFactor).coerceIn(15f, 23f).dp
        val labelWidth = (54f * scale * (if (activeItems.size >= 5) 0.92f else 1f)).coerceIn(42f, 64f).dp
        val labelSizeSp = if (activeItems.size >= 5 || scale < 0.88f) 9.sp else 10.sp

        val motionModifier = Modifier.graphicsLayer {
            val s = (0.28f + 0.72f * progress).coerceAtLeast(0.01f)
            scaleX = s
            scaleY = s
            this.alpha = alpha.coerceIn(0f, 1f)
            transformOrigin = TransformOrigin(0.5f, 1f)
        }

        if (style == QuickArcStyle.HOLLOW) {
            val bandColor = colors.surfaceContainerHighest
            val borderColor = colors.outlineVariant.copy(alpha = 0.4f)
            val bandThickness = (68f * scale).dp
            val bottomCenterOffset = (36f * scale).dp
            val ringRadius = ((containerWidth - bandThickness) / 2f - 4.dp).coerceAtLeast(56.dp)

            Box(
                modifier = Modifier
                    .size(width = containerWidth, height = containerHeight)
                    .then(motionModifier)
                    .drawBehind {
                        val bandPx = bandThickness.toPx()
                        val rPx = ringRadius.toPx()
                        val cx = size.width / 2f
                        val cy = size.height - bottomCenterOffset.toPx()
                        val topLeft = Offset(cx - rPx, cy - rPx)
                        val arcSizePx = Size(rPx * 2f, rPx * 2f)

                        drawArc(
                            color = borderColor,
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSizePx,
                            style = Stroke(width = bandPx + 2.dp.toPx(), cap = StrokeCap.Round),
                        )
                        drawArc(
                            color = bandColor,
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSizePx,
                            style = Stroke(width = bandPx, cap = StrokeCap.Round),
                        )
                    },
            ) {
                QuickArcItemsOverlay(
                    items = activeItems,
                    radiusX = ringRadius,
                    radiusY = ringRadius,
                    centerFromBottom = bottomCenterOffset,
                    buttonSize = buttonSize,
                    iconSize = iconSize,
                    labelWidth = labelWidth,
                    labelSizeSp = labelSizeSp,
                    progress = progress,
                    alpha = alpha,
                    interactive = interactive,
                    onSelectAction = onSelectAction,
                )
            }
        } else {
            Surface(
                color = colors.surfaceContainerHighest,
                shape = RoundedCornerShape(
                    topStart = containerWidth / 2f,
                    topEnd = containerWidth / 2f,
                    bottomStart = 28.dp,
                    bottomEnd = 28.dp,
                ),
                shadowElevation = 18.dp,
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier
                    .size(width = containerWidth, height = containerHeight)
                    .then(motionModifier),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                            .size(width = 32.dp, height = 4.dp)
                            .background(colors.onSurfaceVariant.copy(alpha = 0.25f), CircleShape),
                    )

                    QuickArcItemsOverlay(
                        items = activeItems,
                        radiusX = containerWidth * 0.36f,
                        radiusY = containerHeight * 0.60f,
                        centerFromBottom = (32f * scale).dp,
                        buttonSize = buttonSize,
                        iconSize = iconSize,
                        labelWidth = labelWidth,
                        labelSizeSp = labelSizeSp,
                        progress = progress,
                        alpha = alpha,
                        interactive = interactive,
                        onSelectAction = onSelectAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.QuickArcItemsOverlay(
    items: List<QuickArcAction>,
    radiusX: Dp,
    radiusY: Dp,
    centerFromBottom: Dp,
    buttonSize: Dp,
    iconSize: Dp,
    labelWidth: Dp,
    labelSizeSp: androidx.compose.ui.unit.TextUnit,
    progress: Float,
    alpha: Float,
    interactive: Boolean,
    onSelectAction: (QuickArcAction) -> Unit,
) {
    val colors = LightningTheme.colors
    val count = items.size
    val startDeg = if (count >= 4) -168.0 else -154.0
    val endDeg = if (count >= 4) -12.0 else -26.0
    val itemTotalHalfHeight = (buttonSize / 2f) + 8.dp

    items.forEachIndexed { index, action ->
        val spec = QUICK_ARC_SPECS[action] ?: return@forEachIndexed
        val angleDeg = if (count == 1) {
            -90.0
        } else {
            startDeg + (endDeg - startDeg) * index / (count - 1).toDouble()
        }
        val rad = Math.toRadians(angleDeg)
        val targetX = radiusX * cos(rad).toFloat()
        val targetY = radiusY * sin(rad).toFloat()

        val (containerColor, contentColor) = when (spec.toneSlot) {
            0 -> colors.primaryContainer to colors.onPrimaryContainer
            1 -> colors.secondaryContainer to colors.onSecondaryContainer
            else -> colors.tertiaryContainer to colors.onTertiaryContainer
        }

        val anchorPadding = (centerFromBottom - itemTotalHalfHeight).coerceAtLeast(2.dp)

        QuickArcItem(
            icon = spec.icon,
            label = stringResource(spec.labelRes),
            shape = spec.shape,
            containerColor = containerColor,
            contentColor = contentColor,
            buttonSize = buttonSize,
            iconSize = iconSize,
            labelWidth = labelWidth,
            labelSizeSp = labelSizeSp,
            targetX = targetX,
            targetY = targetY,
            progress = progress,
            alpha = alpha,
            interactive = interactive,
            onClick = { onSelectAction(action) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = anchorPadding),
        )
    }
}

@Composable
private fun QuickArcItem(
    icon: ImageVector,
    label: String,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    buttonSize: Dp,
    iconSize: Dp,
    labelWidth: Dp,
    labelSizeSp: androidx.compose.ui.unit.TextUnit,
    targetX: Dp,
    targetY: Dp,
    progress: Float,
    alpha: Float,
    interactive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    val haptics = rememberLightningHaptics()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(labelWidth)
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
                if (interactive) {
                    haptics.click()
                    onClick()
                }
            },
            enabled = interactive,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier.size(buttonSize),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(iconSize),
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = labelSizeSp),
            fontWeight = FontWeight.Medium,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
