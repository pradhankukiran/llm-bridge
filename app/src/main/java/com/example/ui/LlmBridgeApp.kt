package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LlmBridgeApp(viewModel: LlmViewModel) {
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val activeConfig by viewModel.activeConfig.collectAsStateWithLifecycle()
    val configurations by viewModel.configurations.collectAsStateWithLifecycle()
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val isWaitingForFirstChunk by viewModel.isWaitingForFirstChunk.collectAsStateWithLifecycle()
    val isSwitchingSession by viewModel.isSwitchingSession.collectAsStateWithLifecycle()

    var showSettingsScreen by remember { mutableStateOf(false) }
    var showLogsSheet by remember { mutableStateOf(false) }
    var renamingSession by remember { mutableStateOf<ChatSession?>(null) }
    var showClearChatConfirm by remember { mutableStateOf(false) }
    var sessionPendingDelete by remember { mutableStateOf<ChatSession?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun runWhenIdle(action: () -> Unit) {
        if (isGenerating) {
            scope.launch { snackbarHostState.showSnackbar("Stop generation first") }
        } else {
            action()
        }
    }

    if (showSettingsScreen) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ProviderSettingsPane(
                activeConfig = activeConfig,
                configurations = configurations,
                onSaveConfig = { config ->
                    viewModel.addOrUpdateConfiguration(config)
                },
                onSelectActiveConfig = { id ->
                    viewModel.selectActiveConfiguration(id)
                    showSettingsScreen = false
                },
                onDeleteConfig = { id ->
                    showSettingsScreen = false
                    viewModel.deleteConfiguration(id) { snapshot ->
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Route deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreDeletedConfiguration(snapshot)
                            }
                        }
                    }
                },
                onDismiss = { showSettingsScreen = false }
            )
        }
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.86f)
                    .widthIn(max = 360.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Conversations",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )

                    Button(
                        onClick = {
                            runWhenIdle {
                                viewModel.createNewSession()
                                scope.launch { drawerState.close() }
                            }
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
                                    .combinedClickable(
                                        onClick = {
                                            runWhenIdle {
                                                viewModel.selectSession(session.id)
                                                scope.launch { drawerState.close() }
                                            }
                                        },
                                        onLongClick = { runWhenIdle { renamingSession = session } }
                                    ),
                                color = if (isActive) MaterialTheme.colorScheme.primaryContainer 
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
                                            tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer 
                                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = session.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer 
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
                                            onClick = { runWhenIdle { renamingSession = session } }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Rename",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { runWhenIdle { sessionPendingDelete = session } }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
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
                    onSettingsClick = { runWhenIdle { showSettingsScreen = true } },
                    onLogsClick = { showLogsSheet = true },
                    onClearChat = { runWhenIdle { showClearChatConfirm = true } },
                    onRouteClick = { runWhenIdle { showSettingsScreen = true } }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    isWaitingForFirstChunk = isWaitingForFirstChunk,
                    onSendMessage = { text, mediaUris, mediaDisplayName, mediaType ->
                        viewModel.sendChatMessage(text, mediaUris, mediaDisplayName, mediaType)
                    },
                    onRetryLastMessage = { viewModel.retryLastUserMessage() },
                    onStopGeneration = { viewModel.stopGeneration() }
                )
                AnimatedVisibility(
                    visible = isSwitchingSession,
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
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
    if (showClearChatConfirm) {
        AlertDialog(
            onDismissRequest = { showClearChatConfirm = false },
            title = { Text("Clear chat history?") },
            text = { Text("This removes every message in the current conversation. You can undo it briefly afterward.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearChatHistory { deletedMessages ->
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Chat history cleared",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreMessages(deletedMessages)
                                }
                            }
                        }
                        showClearChatConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val pendingSessionDelete = sessionPendingDelete
    if (pendingSessionDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionPendingDelete = null },
            title = { Text("Delete conversation?") },
            text = { Text("\"${pendingSessionDelete.title}\" and all its messages will be deleted. You can undo it briefly afterward.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(pendingSessionDelete.id) { snapshot ->
                            scope.launch {
                                drawerState.close()
                                val result = snackbarHostState.showSnackbar(
                                    message = "Conversation deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreDeletedSession(snapshot)
                                }
                            }
                        }
                        sessionPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLogsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLogsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ) {
            DiagnosticsLogsPane(
                recentLogs = recentLogs,
                sessionTitle = activeSession?.title,
                onClearLogs = {
                    showLogsSheet = false
                    viewModel.clearAllLogs { deletedLogs ->
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Diagnostics logs cleared",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreLogs(deletedLogs)
                            }
                        }
                    }
                }
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
    onClearChat: () -> Unit,
    onRouteClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Column {
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
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            },
            actions = {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Provider Configuration",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Conversation options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Clear chat") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onClearChat()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Diagnostics") },
                            leadingIcon = { Icon(Icons.Default.BugReport, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onLogsClick()
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )
        ActiveRouteStrip(
            activeConfig = activeConfig,
            onClick = onRouteClick
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    }
}

@Composable
private fun ActiveRouteStrip(
    activeConfig: LlmConfiguration?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val config = activeConfig
            Icon(
                imageVector = Icons.Default.Route,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            if (config == null) {
                Text(
                    text = "Select a route",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = config.modelName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
