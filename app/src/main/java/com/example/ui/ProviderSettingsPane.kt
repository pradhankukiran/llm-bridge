package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LlmConfiguration

data class ProviderOption(val name: String, val id: String, val defaultUrl: String)

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ProviderSettingsPane(
    activeConfig: LlmConfiguration?,
    configurations: List<LlmConfiguration>,
    onSaveConfig: (LlmConfiguration) -> Unit,
    onSelectActiveConfig: (Int) -> Unit,
    onDeleteConfig: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var editingConfig by remember { mutableStateOf<LlmConfiguration?>(null) }
    var isAddingNew by remember(configurations) { mutableStateOf(configurations.isEmpty()) }
    var configPendingDelete by remember { mutableStateOf<LlmConfiguration?>(null) }

    if (editingConfig == null && !isAddingNew) {
        // --- ROUTE MANAGER LIST VIEW ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "API routing gateway",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(configurations) { config ->
                    val isActive = config.id == activeConfig?.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onSelectActiveConfig(config.id) },
                                onLongClick = { editingConfig = config }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) 
                                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        border = if (isActive) {
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = config.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        color = if (config.apiType == "ANTHROPIC") MaterialTheme.colorScheme.tertiaryContainer 
                                                else MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = config.apiType,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            color = if (config.apiType == "ANTHROPIC") MaterialTheme.colorScheme.onTertiaryContainer 
                                                    else MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Base URL: ${config.baseUrl.take(40)}${if (config.baseUrl.length > 40) "..." else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Model: ${config.modelName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(onClick = { editingConfig = config }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Route",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (configurations.size > 1) {
                                    IconButton(onClick = { configPendingDelete = config }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Route",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { isAddingNew = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add New Route")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add new route",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val pendingDelete = configPendingDelete
        if (pendingDelete != null) {
            AlertDialog(
                onDismissRequest = { configPendingDelete = null },
                title = { Text("Delete route?") },
                text = { Text("\"${pendingDelete.name}\" and all its conversations will be deleted. You can undo it briefly afterward.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteConfig(pendingDelete.id)
                            configPendingDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { configPendingDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    } else {
        // --- ADD/EDIT ROUTE FORM VIEW ---
        val configToEdit = editingConfig
        val popForm = {
            if (configurations.isEmpty()) {
                onDismiss()
            } else {
                editingConfig = null
                isAddingNew = false
            }
        }
        androidx.activity.compose.BackHandler(enabled = true) { popForm() }
        
        var baseUrl by remember(configToEdit) { mutableStateOf(configToEdit?.baseUrl ?: "") }
        var apiKey by remember(configToEdit) { mutableStateOf("") }
        var modelName by remember(configToEdit) { mutableStateOf(configToEdit?.modelName ?: "") }

        var systemPrompt by remember(configToEdit) { mutableStateOf(configToEdit?.systemPrompt ?: "") }
        var temperature by remember(configToEdit) { mutableStateOf(configToEdit?.temperature?.toFloat() ?: 1.0f) }
        var maxTokens by remember(configToEdit) { mutableStateOf(configToEdit?.maxTokens?.toString() ?: "4096") }
        var lastValidMaxTokens by remember(configToEdit) { mutableStateOf(configToEdit?.maxTokens ?: 4096) }
        var stream by remember(configToEdit) { mutableStateOf(configToEdit?.stream ?: true) }
        var showThinkingTags by remember(configToEdit) {
            mutableStateOf(configToEdit?.reasoningMode == REASONING_MODE_SHOW_THINKING)
        }
        var saveAttempted by remember(configToEdit) { mutableStateOf(false) }
        var isSavingConfig by remember(configToEdit) { mutableStateOf(false) }

        var isKeyVisible by remember { mutableStateOf(false) }

        var apiType by remember(configToEdit) { mutableStateOf(configToEdit?.apiType ?: "OPENAI") }
        val generatedName = remember(baseUrl, modelName) {
            val domain = try {
                val uri = java.net.URI(baseUrl)
                uri.host ?: baseUrl.substringAfter("://").substringBefore("/")
            } catch (e: Exception) {
                baseUrl.substringAfter("://").substringBefore("/")
            }
            if (modelName.isNotBlank()) "$domain - $modelName" else domain.ifBlank { "Custom Route" }
        }
        val trimmedBaseUrl = baseUrl.trim()
        val baseUrlIsValid = remember(trimmedBaseUrl) {
            runCatching {
                val uri = java.net.URI(trimmedBaseUrl)
                uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
            }.getOrDefault(false)
        }
        val modelNameIsValid = modelName.isNotBlank()
        val maxTokensValue = maxTokens.toIntOrNull()
        val maxTokensIsValid = maxTokens.isBlank() || (maxTokensValue != null && maxTokensValue > 0)
        val formIsValid = baseUrlIsValid && modelNameIsValid && maxTokensIsValid
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .navigationBarsPadding()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = popForm) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (configToEdit != null) "Edit route" else "Add new route",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Connection settings",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "API Protocol",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = apiType == "OPENAI",
                                onClick = { apiType = "OPENAI" },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("OpenAI-compatible", fontSize = 12.sp)
                            }
                            SegmentedButton(
                                selected = apiType == "ANTHROPIC",
                                onClick = { apiType = "ANTHROPIC" },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("Anthropic Messages", fontSize = 12.sp)
                            }
                        }
                    }

                    LlmInputField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = "API Base URL",
                        placeholder = if (apiType == "ANTHROPIC") "https://api.anthropic.com/v1" else "https://api.openai.com/v1",
                        isError = saveAttempted && !baseUrlIsValid,
                        supportingText = if (saveAttempted && !baseUrlIsValid) "Enter a valid http or https URL." else null
                    )

                    LlmInputField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = if (configToEdit != null) "Replace API Key" else "API Key",
                        placeholder = if (configToEdit != null) "Stored key will be kept" else "Enter API key...",
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle visibility",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )

                    LlmInputField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = "Model Name",
                        placeholder = "e.g. gpt-4o, claude-3-5-sonnet, llama3",
                        isError = saveAttempted && !modelNameIsValid,
                        supportingText = if (saveAttempted && !modelNameIsValid) "Model name is required." else null
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Stream Response",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Receive message chunks incrementally in real-time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        Switch(
                            checked = stream,
                            onCheckedChange = { stream = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show Thinking Tags",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Display <think> blocks as collapsible reasoning panels.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        Switch(
                            checked = showThinkingTags,
                            onCheckedChange = { showThinkingTags = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Model configuration",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    LlmInputField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        label = "System Prompt",
                        placeholder = "Specify model persona or instructions...",
                        singleLine = false,
                        minLines = 3,
                        maxLines = 6
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Temperature",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.2f", temperature),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Slider(
                            value = temperature,
                            onValueChange = { temperature = it },
                            valueRange = 0.0f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.secondary,
                                activeTrackColor = MaterialTheme.colorScheme.secondary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    LlmInputField(
                        value = maxTokens,
                        onValueChange = { value ->
                            if (value.all { it.isDigit() }) {
                                maxTokens = value
                                value.toIntOrNull()?.takeIf { it > 0 }?.let { lastValidMaxTokens = it }
                            }
                        },
                        label = "Max Tokens",
                        placeholder = "e.g. 4096",
                        isError = saveAttempted && !maxTokensIsValid,
                        supportingText = if (maxTokens.isBlank()) {
                            "Leaving this blank restores $lastValidMaxTokens."
                        } else if (saveAttempted && !maxTokensIsValid) {
                            "Enter a positive number."
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = popForm,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        saveAttempted = true
                        if (formIsValid && !isSavingConfig) {
                            isSavingConfig = true
                            val maxTokVal = maxTokens.toIntOrNull() ?: lastValidMaxTokens
                            val base = configToEdit ?: LlmConfiguration(
                                name = generatedName,
                                baseUrl = "",
                                apiKey = "",
                                modelName = "",
                                apiType = apiType,
                                isActive = false
                            )
                            val savedConfig = base.copy(
                                name = generatedName,
                                baseUrl = baseUrl.trim(),
                                apiKey = if (configToEdit != null && apiKey.isBlank()) base.apiKey else apiKey.trim(),
                                modelName = modelName.trim(),
                                apiType = apiType,
                                maxTokens = maxTokVal,
                                temperature = temperature.toDouble(),
                                stream = stream,
                                reasoningMode = if (showThinkingTags) REASONING_MODE_SHOW_THINKING else "",
                                systemPrompt = systemPrompt.trim()
                            )
                            onSaveConfig(savedConfig)
                            editingConfig = null
                            isAddingNew = false
                        }
                    },
                    enabled = !isSavingConfig,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Save route",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun LlmInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { 
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            ) 
        },
        placeholder = { 
            Text(
                text = placeholder, 
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            ) 
        },
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        isError = isError,
        supportingText = supportingText?.let { message ->
            { Text(message) }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
            unfocusedContainerColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun rememberScrollState(): androidx.compose.foundation.ScrollState {
    return androidx.compose.foundation.rememberScrollState()
}
