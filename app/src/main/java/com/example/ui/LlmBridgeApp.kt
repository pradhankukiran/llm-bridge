package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChatMessage
import com.example.data.LlmConfiguration
import com.example.data.ChatSession
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LlmBridgeApp(viewModel: LlmViewModel) {
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val activeConfig by viewModel.activeConfig.collectAsStateWithLifecycle()
    val configurations by viewModel.configurations.collectAsStateWithLifecycle()
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showLogsSheet by remember { mutableStateOf(false) }
    var renamingSession by remember { mutableStateOf<ChatSession?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxHeight().width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "CONVERSATIONS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.createNewSession()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "New Chat")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Chat")
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(sessions) { session ->
                            val isActive = session.id == activeSession?.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        viewModel.selectSession(session.id)
                                        scope.launch { drawerState.close() }
                                    },
                                color = if (isActive) MaterialTheme.colorScheme.secondaryContainer 
                                        else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChatBubble,
                                            contentDescription = "Chat",
                                            tint = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer 
                                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = session.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer 
                                                   else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        IconButton(
                                            onClick = { renamingSession = session },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Rename",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteSession(session.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                HeaderBlock(
                    activeConfig = activeConfig,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onSettingsClick = { showSettingsSheet = true },
                    onLogsClick = { showLogsSheet = true },
                    onClearChat = { viewModel.clearChatHistory() }
                )
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                ChatInterface(
                    activeConfig = activeConfig,
                    chatHistory = chatHistory,
                    isGenerating = isGenerating,
                    onSendMessage = { text, mediaUris, mediaType -> viewModel.sendChatMessage(text, mediaUris, mediaType) }
                )
            }
        }
    }

    // Rename Dialog
    val renaming = renamingSession
    if (renaming != null) {
        var tempTitle by remember(renaming) { mutableStateOf(renaming.title) }
        AlertDialog(
            onDismissRequest = { renamingSession = null },
            title = { Text("Rename Conversation") },
            text = {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempTitle.isNotBlank()) {
                            viewModel.renameSession(renaming.id, tempTitle.trim())
                            renamingSession = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingSession = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Bottom Sheets
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ProviderSettingsPane(
                activeConfig = activeConfig,
                configurations = configurations,
                onSaveConfig = { config ->
                    viewModel.addOrUpdateConfiguration(config)
                },
                onSelectActiveConfig = { id -> 
                    viewModel.selectActiveConfiguration(id)
                },
                onDeleteConfig = { id -> 
                    viewModel.deleteConfiguration(id)
                },
                onDismiss = { showSettingsSheet = false }
            )
        }
    }

    if (showLogsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLogsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ) {
            DiagnosticsLogsPane(
                recentLogs = recentLogs,
                onClearLogs = { viewModel.clearAllLogs() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBlock(
    activeConfig: LlmConfiguration?,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogsClick: () -> Unit,
    onClearChat: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Conversations",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "LLM Bridge",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (activeConfig != null) {
                    val modelName = activeConfig.modelName.substringAfter("/")
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = modelName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onClearChat) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear Chat History",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onLogsClick) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Diagnostics Logs",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Provider Configuration",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}
