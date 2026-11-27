package com.lightning.browser.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.lightning.browser.R
import com.lightning.browser.ui.BrowserStore
import com.lightning.browser.ui.browser.AppHandler
import com.lightning.browser.ui.browser.AppHandlerResolver
import com.lightning.browser.ui.components.GroupedColumn
import com.lightning.browser.ui.components.ShapedIcon
import com.lightning.browser.ui.theme.EyebrowTextStyle
import com.lightning.browser.ui.theme.ExpressiveShapes
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.rememberLightningHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSettingsScreen(
    saveFolder: BrowserStore.DownloadFolder,
    askBeforeDownload: Boolean,
    deletePrivateOnClose: Boolean,
    otherFilesAsk: Boolean,
    contentPreferences: Map<String, BrowserStore.ContentPreference>,
    onSaveFolderChange: (BrowserStore.DownloadFolder) -> Unit,
    onAskBeforeDownloadChange: (Boolean) -> Unit,
    onDeletePrivateOnCloseChange: (Boolean) -> Unit,
    onOtherFilesAskChange: (Boolean) -> Unit,
    onContentPreferenceChange: (String, BrowserStore.ContentPreference) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    val context = LocalContext.current
    var pickingFolder by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<BrowserStore.ContentTypeRule?>(null) }

    val rules = BrowserStore.defaultContentTypeRules
    val systemDefaults = remember(rules) {
        rules.associate { it.key to AppHandlerResolver.resolveSystemDefault(context, it.mimeOrScheme) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        SettingsHeader(stringResource(R.string.dl_settings_title), onBack)
        Spacer(Modifier.height(8.dp))

        GroupedColumn(3) { i, shape ->
            when (i) {
                0 -> SettingsRow(
                    icon = Icons.Filled.FolderOpen,
                    title = stringResource(R.string.dl_save_to_title),
                    summary = stringResource(saveFolder.labelRes),
                    onClick = { pickingFolder = true },
                    shape = shape,
                    index = i,
                )
                1 -> SettingsSwitchRow(
                    title = stringResource(R.string.dl_ask_before),
                    summary = stringResource(R.string.dl_ask_before_summary),
                    checked = askBeforeDownload,
                    onCheckedChange = onAskBeforeDownloadChange,
                    shape = shape,
                )
                else -> SettingsSwitchRow(
                    title = stringResource(R.string.dl_delete_private),
                    summary = stringResource(R.string.dl_delete_private_summary),
                    checked = deletePrivateOnClose,
                    onCheckedChange = onDeletePrivateOnCloseChange,
                    shape = shape,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.dl_files_apps_title).uppercase(),
            style = EyebrowTextStyle,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.dl_files_apps_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(10.dp))

        GroupedColumn(rules.size) { i, shape ->
            val rule = rules[i]
            val pref = contentPreferences[rule.key] ?: BrowserStore.ContentPreference(rule.defaultAction)
            val defaultApp = systemDefaults[rule.key]
            val actionLabel = when (pref.action) {
                BrowserStore.ContentAction.OPEN_WITH_APP -> when {
                    !pref.appLabel.isNullOrBlank() -> stringResource(R.string.dl_action_use_app, pref.appLabel)
                    defaultApp != null -> stringResource(R.string.dl_action_use_default_app, defaultApp.label)
                    else -> stringResource(R.string.dl_action_use_default)
                }
                else -> stringResource(pref.action.labelRes)
            }
            ContentTypeRow(
                title = rule.title,
                actionLabel = actionLabel,
                shape = shape,
                onClick = { editingRule = rule },
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.dl_other_files_title).uppercase(),
            style = EyebrowTextStyle,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(10.dp))
        GroupedColumn(2) { i, shape ->
            when (i) {
                0 -> SettingsRadioRow(
                    title = stringResource(R.string.dl_other_auto_save),
                    summary = stringResource(R.string.dl_other_auto_save_summary),
                    selected = !otherFilesAsk,
                    onClick = { onOtherFilesAskChange(false) },
                    shape = shape,
                )
                else -> SettingsRadioRow(
                    title = stringResource(R.string.dl_other_ask),
                    summary = stringResource(R.string.dl_other_ask_summary),
                    selected = otherFilesAsk,
                    onClick = { onOtherFilesAskChange(true) },
                    shape = shape,
                )
            }
        }
    }

    if (pickingFolder) {
        AlertDialog(
            onDismissRequest = { pickingFolder = false },
            title = { Text(stringResource(R.string.dl_save_to_title), color = colors.onSurface) },
            text = {
                GroupedColumn(BrowserStore.DownloadFolder.entries.size) { i, shape ->
                    val folder = BrowserStore.DownloadFolder.entries[i]
                    SettingsRadioRow(
                        title = stringResource(folder.labelRes),
                        summary = folder.relativePath,
                        selected = folder == saveFolder,
                        onClick = {
                            onSaveFolderChange(folder)
                            pickingFolder = false
                        },
                        shape = shape,
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pickingFolder = false }) {
                    Text(stringResource(R.string.dns_custom_cancel), color = colors.primary)
                }
            },
        )
    }

    editingRule?.let { rule ->
        val current = contentPreferences[rule.key] ?: BrowserStore.ContentPreference(rule.defaultAction)
        val handlers = remember(rule.key) { AppHandlerResolver.queryHandlers(context, rule.mimeOrScheme) }
        val defaultApp = systemDefaults[rule.key]
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { editingRule = null },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = colors.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .navigationBarsPadding(),
            ) {
                // Header with ShapedIcon, rule title, MIME/scheme subtitle, and close button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ShapedIcon(
                        icon = ruleCategoryIcon(rule.mimeOrScheme),
                        shape = ExpressiveShapes.Cookie9,
                        container = colors.primaryContainer,
                        content = colors.onPrimaryContainer,
                        size = 44.dp,
                        iconSize = 22.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = rule.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = rule.mimeOrScheme,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { editingRule = null }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.dns_custom_cancel),
                            tint = colors.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Section 1: Browser actions
                Text(
                    text = stringResource(R.string.dl_section_browser_actions),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                )
                GroupedColumn(rule.supportedActions.size) { i, shape ->
                    val action = rule.supportedActions[i]
                    val selected = current.action == action && current.packageName == null
                    ActionSheetRow(
                        title = stringResource(action.labelRes),
                        icon = actionIcon(action),
                        handler = null,
                        selected = selected,
                        shape = shape,
                        onClick = {
                            onContentPreferenceChange(
                                rule.key,
                                BrowserStore.ContentPreference(action),
                            )
                            editingRule = null
                        },
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Section 2: External apps
                Text(
                    text = stringResource(R.string.dl_section_external_apps),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                )
                val appCount = 1 + handlers.size
                GroupedColumn(appCount) { i, shape ->
                    if (i == 0) {
                        val defaultTitle = if (defaultApp != null) {
                            stringResource(R.string.dl_action_use_default_app, defaultApp.label)
                        } else {
                            stringResource(R.string.dl_action_use_default)
                        }
                        val selected = current.action == BrowserStore.ContentAction.OPEN_WITH_APP &&
                            current.packageName == null
                        ActionSheetRow(
                            title = defaultTitle,
                            icon = Icons.AutoMirrored.Outlined.OpenInNew,
                            handler = defaultApp,
                            selected = selected,
                            shape = shape,
                            onClick = {
                                onContentPreferenceChange(
                                    rule.key,
                                    BrowserStore.ContentPreference(BrowserStore.ContentAction.OPEN_WITH_APP),
                                )
                                editingRule = null
                            },
                        )
                    } else {
                        val handler = handlers[i - 1]
                        val selected = current.action == BrowserStore.ContentAction.OPEN_WITH_APP &&
                            current.packageName == handler.packageName
                        ActionSheetRow(
                            title = stringResource(R.string.dl_action_use_app, handler.label),
                            icon = Icons.AutoMirrored.Outlined.OpenInNew,
                            handler = handler,
                            selected = selected,
                            shape = shape,
                            onClick = {
                                onContentPreferenceChange(
                                    rule.key,
                                    BrowserStore.ContentPreference(
                                        action = BrowserStore.ContentAction.OPEN_WITH_APP,
                                        packageName = handler.packageName,
                                        className = handler.className,
                                        appLabel = handler.label,
                                    ),
                                )
                                editingRule = null
                            },
                        )
                    }
                }
                if (handlers.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.dl_no_external_apps),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

private fun ruleCategoryIcon(mimeOrScheme: String): ImageVector = when {
    mimeOrScheme.startsWith("image/") -> Icons.Outlined.Image
    mimeOrScheme.contains("pdf") -> Icons.Outlined.PictureAsPdf
    mimeOrScheme.contains("xml") -> Icons.Outlined.Code
    mimeOrScheme.contains("mailto") -> Icons.Outlined.Email
    mimeOrScheme.contains("/") -> Icons.Outlined.InsertDriveFile
    else -> Icons.AutoMirrored.Outlined.OpenInNew
}

private fun actionIcon(action: BrowserStore.ContentAction): ImageVector = when (action) {
    BrowserStore.ContentAction.SAVE_FILE -> Icons.Outlined.Download
    BrowserStore.ContentAction.OPEN_IN_BROWSER -> Icons.Outlined.OpenInBrowser
    BrowserStore.ContentAction.ALWAYS_ASK -> Icons.AutoMirrored.Outlined.HelpOutline
    BrowserStore.ContentAction.OPEN_WITH_APP -> Icons.AutoMirrored.Outlined.OpenInNew
}

@Composable
private fun ActionSheetRow(
    title: String,
    icon: ImageVector,
    handler: AppHandler?,
    selected: Boolean,
    shape: Shape,
    onClick: () -> Unit,
) {
    val colors = LightningTheme.colors
    val haptics = rememberLightningHaptics()
    val iconBitmap = remember(handler?.packageName) {
        handler?.icon?.let { runCatching { it.toBitmap(72, 72).asImageBitmap() }.getOrNull() }
    }
    Surface(
        onClick = {
            haptics.tick()
            onClick()
        },
        shape = shape,
        color = if (selected) colors.surfaceContainerHighest else colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) colors.primary else colors.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = colors.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
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
private fun ContentTypeRow(
    title: String,
    actionLabel: String,
    shape: Shape,
    onClick: () -> Unit,
) {
    val colors = LightningTheme.colors
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
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.primary,
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
