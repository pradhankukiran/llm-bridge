package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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

@OptIn(ExperimentalLayoutApi::class)
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

    if (editingConfig == null && !isAddingNew) {
        // --- ROUTE MANAGER LIST VIEW ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "API ROUTING GATEWAY",
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
                            .clickable { onSelectActiveConfig(config.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) 
                                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = if (isActive) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
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
                                    IconButton(onClick = { onDeleteConfig(config.id) }) {
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
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add New Route")
                Spacer(modifier = Modifier.width(8.dp))
                Text("ADD NEW ROUTE")
            }
        }
    } else {
        // --- ADD/EDIT ROUTE FORM VIEW ---
        val configToEdit = editingConfig
        
        var baseUrl by remember { mutableStateOf(configToEdit?.baseUrl ?: "") }
        var apiKey by remember { mutableStateOf(configToEdit?.apiKey ?: "") }
        var modelName by remember { mutableStateOf(configToEdit?.modelName ?: "") }
        
        var systemPrompt by remember { mutableStateOf(configToEdit?.systemPrompt ?: "") }
        var temperature by remember { mutableStateOf(configToEdit?.temperature?.toFloat() ?: 1.0f) }
        var maxTokens by remember { mutableStateOf(configToEdit?.maxTokens?.toString() ?: "4096") }
        var stream by remember { mutableStateOf(configToEdit?.stream ?: true) }
        
        var isKeyVisible by remember { mutableStateOf(false) }

        val apiType = if (baseUrl.contains("anthropic.com")) "ANTHROPIC" else "OPENAI"
        val generatedName = remember(baseUrl, modelName) {
            val domain = try {
                val uri = java.net.URI(baseUrl)
                uri.host ?: baseUrl.substringAfter("://").substringBefore("/")
            } catch (e: Exception) {
                baseUrl.substringAfter("://").substringBefore("/")
            }
            if (modelName.isNotBlank()) "$domain - $modelName" else domain.ifBlank { "Custom Route" }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { 
                    if (configurations.isEmpty()) {
                        onDismiss()
                    } else {
                        editingConfig = null
                        isAddingNew = false
                    }
                }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (configToEdit != null) "EDIT ROUTE" else "ADD NEW ROUTE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            M3InputField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = "API Base URL",
                placeholder = "https://api.openai.com/v1"
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key", style = MaterialTheme.typography.bodySmall) },
                placeholder = { Text("Enter API key...", style = MaterialTheme.typography.bodyMedium) },
                singleLine = true,
                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                        Icon(
                            imageVector = if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle visibility",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = modelName,
                onValueChange = { modelName = it },
                label = { Text("Model Name", style = MaterialTheme.typography.bodySmall) },
                placeholder = { Text("e.g. gpt-4o, claude-3-5-sonnet, llama3", style = MaterialTheme.typography.bodyMedium) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
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
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Receive message chunks incrementally in real-time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = stream,
                    onCheckedChange = { stream = it }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = "MODEL CONFIGURATION",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text("System Prompt", style = MaterialTheme.typography.bodySmall) },
                placeholder = { Text("Specify model persona or instructions...", style = MaterialTheme.typography.bodyMedium) },
                singleLine = false,
                minLines = 3,
                maxLines = 6,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
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
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "%.2f", temperature),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = temperature,
                    onValueChange = { temperature = it },
                    valueRange = 0.0f..2.0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = maxTokens,
                onValueChange = { maxTokens = it },
                label = { Text("Max Tokens", style = MaterialTheme.typography.bodySmall) },
                placeholder = { Text("e.g. 4096", style = MaterialTheme.typography.bodyMedium) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { 
                        if (configurations.isEmpty()) {
                            onDismiss()
                        } else {
                            editingConfig = null
                            isAddingNew = configurations.isEmpty()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("CANCEL")
                }

                Button(
                    onClick = {
                        if (baseUrl.isNotBlank() && modelName.isNotBlank()) {
                            val maxTokVal = maxTokens.toIntOrNull() ?: 4096
                            val savedConfig = LlmConfiguration(
                                id = configToEdit?.id ?: 0,
                                name = generatedName,
                                baseUrl = baseUrl.trim(),
                                apiKey = apiKey.trim(),
                                modelName = modelName.trim(),
                                apiType = apiType,
                                providerId = if (apiType == "ANTHROPIC") "anthropic" 
                                             else "openai-compatible",
                                maxTokens = maxTokVal,
                                temperature = temperature.toDouble(),
                                stream = stream,
                                systemPrompt = systemPrompt.trim(),
                                isActive = configToEdit?.isActive ?: false,
                                modelOfferingId = configToEdit?.modelOfferingId ?: ""
                            )
                            onSaveConfig(savedConfig)
                            editingConfig = null
                            isAddingNew = false
                        }
                    },
                    enabled = baseUrl.isNotBlank() && modelName.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("SAVE ROUTE")
                }
            }
        }
    }
}

@Composable
fun M3InputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { 
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            ) 
        },
        placeholder = { 
            Text(
                text = placeholder, 
                style = MaterialTheme.typography.bodyMedium
            ) 
        },
        singleLine = singleLine,
        maxLines = maxLines,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun rememberScrollState(): androidx.compose.foundation.ScrollState {
    return androidx.compose.foundation.rememberScrollState()
}
