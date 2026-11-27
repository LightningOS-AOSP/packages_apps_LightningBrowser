package com.lightning.browser.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lightning.browser.R
import com.lightning.browser.ui.components.GroupedColumn
import com.lightning.browser.ui.theme.ExpressiveShapes
import com.lightning.browser.ui.theme.LightningTheme
import com.lightning.browser.ui.theme.rememberLightningHaptics

@Composable
fun SearchEngineScreen(
    activeEngineId: String,
    builtInEngine: SearchEngine,
    customEngines: List<CustomSearchEngine>,
    onSelectBuiltIn: (SearchEngine) -> Unit,
    onSelectCustom: (CustomSearchEngine) -> Unit,
    onAddCustomEngine: (name: String, url: String, keyword: String) -> Unit,
    onUpdateCustomEngine: (CustomSearchEngine) -> Unit,
    onDeleteCustomEngine: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightningTheme.colors
    var editingEngine by remember { mutableStateOf<CustomSearchEngine?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var engineToDelete by remember { mutableStateOf<CustomSearchEngine?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        SettingsHeader(stringResource(R.string.search_engine_title), onBack)
        Text(
            text = stringResource(R.string.search_engine_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.search_engine_builtin_header),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
        GroupedColumn(SearchEngine.entries.size) { index, shape ->
            val choice = SearchEngine.entries[index]
            SettingsRadioRow(
                title = stringResource(choice.labelRes),
                summary = stringResource(choice.summaryRes),
                selected = choice.name == activeEngineId,
                onClick = { onSelectBuiltIn(choice) },
                shape = shape,
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.search_engine_custom_header),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
        if (customEngines.isNotEmpty()) {
            GroupedColumn(customEngines.size) { index, shape ->
                val custom = customEngines[index]
                CustomEngineRow(
                    engine = custom,
                    selected = custom.id == activeEngineId,
                    onClick = { onSelectCustom(custom) },
                    onEdit = { editingEngine = custom },
                    onDelete = { engineToDelete = custom },
                    shape = shape,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        GroupedColumn(1) { _, shape ->
            AddEngineRow(onClick = { showAddDialog = true }, shape = shape)
        }
    }

    if (showAddDialog) {
        SearchEngineDialog(
            initialEngine = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, url, keyword ->
                onAddCustomEngine(name, url, keyword)
                showAddDialog = false
            },
        )
    }

    editingEngine?.let { engine ->
        SearchEngineDialog(
            initialEngine = engine,
            onDismiss = { editingEngine = null },
            onSave = { name, url, keyword ->
                onUpdateCustomEngine(engine.copy(name = name, searchUrlFormat = url, keyword = keyword))
                editingEngine = null
            },
        )
    }

    engineToDelete?.let { engine ->
        AlertDialog(
            onDismissRequest = { engineToDelete = null },
            title = { Text(stringResource(R.string.search_engine_delete_confirm)) },
            text = { Text(engine.name) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCustomEngine(engine.id)
                    engineToDelete = null
                }) {
                    Text(stringResource(R.string.search_engine_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { engineToDelete = null }) {
                    Text(stringResource(R.string.search_engine_dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun CustomEngineRow(
    engine: CustomSearchEngine,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    shape: Shape = ExpressiveShapes.grouped(0, 1),
) {
    val colors = LightningTheme.colors
    val haptics = rememberLightningHaptics()
    val summary = if (engine.keyword.isNotBlank()) {
        "${engine.keyword} · ${engine.searchUrlFormat}"
    } else {
        engine.searchUrlFormat
    }
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            RadioDot(selected)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = engine.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.search_engine_edit),
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.search_engine_delete),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun AddEngineRow(
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
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .background(colors.primaryContainer, CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = colors.onPrimaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = stringResource(R.string.search_engine_add_button),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = colors.primary,
            )
        }
    }
}

@Composable
private fun SearchEngineDialog(
    initialEngine: CustomSearchEngine?,
    onDismiss: () -> Unit,
    onSave: (name: String, url: String, keyword: String) -> Unit,
) {
    var name by remember { mutableStateOf(initialEngine?.name.orEmpty()) }
    var url by remember { mutableStateOf(initialEngine?.searchUrlFormat.orEmpty()) }
    var keyword by remember { mutableStateOf(initialEngine?.keyword.orEmpty()) }
    var nameError by remember { mutableStateOf(false) }
    var urlError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initialEngine != null) R.string.edit_search_engine_title
                    else R.string.add_search_engine_title
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (nameError && it.trim().isNotEmpty()) nameError = false
                    },
                    label = { Text(stringResource(R.string.search_engine_name_label)) },
                    placeholder = { Text(stringResource(R.string.search_engine_name_hint)) },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text(stringResource(R.string.search_engine_name_error)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        val t = it.trim()
                        if (urlError && (t.startsWith("http://") || t.startsWith("https://")) && t.contains("%s")) {
                            urlError = false
                        }
                    },
                    label = { Text(stringResource(R.string.search_engine_url_label)) },
                    placeholder = { Text(stringResource(R.string.search_engine_url_hint)) },
                    isError = urlError,
                    supportingText = if (urlError) {
                        { Text(stringResource(R.string.search_engine_url_error)) }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text(stringResource(R.string.search_engine_keyword_label)) },
                    placeholder = { Text(stringResource(R.string.search_engine_keyword_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.search_engine_dialog_cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmedName = name.trim()
                val trimmedUrl = url.trim()
                val isNameValid = trimmedName.isNotEmpty()
                val isUrlValid = (trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")) &&
                    trimmedUrl.contains("%s")
                nameError = !isNameValid
                urlError = !isUrlValid
                if (isNameValid && isUrlValid) {
                    onSave(trimmedName, trimmedUrl, keyword.trim())
                }
            }) {
                Text(
                    stringResource(
                        if (initialEngine != null) R.string.search_engine_dialog_save
                        else R.string.search_engine_dialog_add
                    )
                )
            }
        },
    )
}
