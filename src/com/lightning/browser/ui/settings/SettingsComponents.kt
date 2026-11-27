package com.lightning.browser.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.lightning.browser.R
import com.lightning.browser.ui.components.ShapedIcon
import com.lightning.browser.ui.components.TonalIconButton
import com.lightning.browser.ui.theme.ExpressiveShapes
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.rememberLightningHaptics
import com.lightning.browser.ui.theme.tone
import com.lightning.browser.ui.theme.ThemeMode

@Composable
fun ThemeMode.label(): String = when (this) {
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
}

@Composable
fun SettingsHeader(
    title: String,
    onBack: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val colors = LightningTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TonalIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                onClick = onBack,
                tint = colors.onSurface,
            )
            Spacer(Modifier.weight(1f))
            trailing()
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = colors.onSurface,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 16.dp),
        )
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    summary: String,
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            ShapedIcon(
                icon,
                ExpressiveShapes.cycle(index),
                tone.container,
                tone.content,
                size = 48.dp,
                iconSize = 24.dp,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }
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
fun SettingsSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: Shape = ExpressiveShapes.grouped(0, 1),
) {
    val colors = LightningTheme.colors
    val haptics = rememberLightningHaptics()
    Surface(
        shape = shape,
        color = colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = {
                    haptics.tick()
                    onCheckedChange(it)
                },
                thumbContent = if (checked) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onPrimary,
                    checkedTrackColor = colors.primary,
                    checkedBorderColor = colors.primary,
                    uncheckedThumbColor = colors.surfaceContainerHigh,
                    uncheckedTrackColor = colors.surfaceContainer,
                    uncheckedBorderColor = colors.outline,
                ),
            )
        }
    }
}

@Composable
fun SettingsRadioRow(
    title: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit,
    shape: Shape = ExpressiveShapes.grouped(0, 1),
) {
    val colors = LightningTheme.colors
    val haptics = rememberLightningHaptics()
    Surface(
        onClick = {
            haptics.tick()
            onClick()
        },
        shape = shape,
        color = colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            RadioDot(selected)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
internal fun RadioDot(selected: Boolean) {
    val colors = LightningTheme.colors
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(
                color = if (selected) Color.Transparent else colors.surfaceContainerHigh,
                shape = CircleShape,
            )
            .border(
                width = 2.dp,
                color = if (selected) colors.primary else colors.outline,
                shape = CircleShape,
            ),
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(10.dp)
                    .background(colors.primary, CircleShape),
            )
        }
    }
}

// Material error colour, kept outside the palette on purpose for destructive actions.
private val SweepColor = Color(0xFFB3261E)

@Composable
fun SwipeToDeleteBox(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraSmall,
    content: @Composable () -> Unit,
) {
    val haptics = rememberLightningHaptics()
    var isRemoved by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                isRemoved = true
                true
            } else {
                false
            }
        },
    )
    var wasTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(dismissState.targetValue) {
        val triggered = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
        if (triggered && !wasTriggered) {
            haptics.gestureThreshold()
        }
        wasTriggered = triggered
    }
    LaunchedEffect(isRemoved) {
        if (isRemoved) {
            delay(240L)
            onDelete()
        }
    }
    AnimatedVisibility(
        visible = !isRemoved,
        exit = shrinkVertically(animationSpec = tween(220)) + fadeOut(animationSpec = tween(180)),
        modifier = modifier,
    ) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            backgroundContent = {
                val triggered = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
                val bgColor by animateColorAsState(
                    targetValue = if (triggered) SweepColor else SweepColor.copy(alpha = 0.72f),
                    animationSpec = tween(150),
                    label = "swipeBgColor",
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (triggered) 1.2f else 0.9f,
                    animationSpec = tween(150),
                    label = "swipeIconScale",
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .background(bgColor),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(end = 20.dp)
                            .size(20.dp)
                            .scale(iconScale),
                    )
                }
            },
        ) {
            content()
        }
    }
}