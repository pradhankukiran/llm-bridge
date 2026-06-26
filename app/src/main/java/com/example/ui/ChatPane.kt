package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessage
import com.example.data.LlmConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInterface(
    activeConfig: LlmConfiguration?,
    chatHistory: List<ChatMessage>,
    isGenerating: Boolean,
    onSendMessage: (String, String, String) -> Unit,
    onStopGeneration: () -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var inputText by remember { mutableStateOf("") }
    var mediaUris by remember(activeConfig?.id) { mutableStateOf("") }
    var mediaType by remember(activeConfig?.id) { mutableStateOf(activeConfig?.mediaInputType ?: "auto") }
    val mediaSupported = activeConfig != null
 
    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            mediaUris = uri.toString()
        }
    }

    val listState = rememberLazyListState()
    val showThinking = isGenerating && (chatHistory.isEmpty() || chatHistory.last().role != "assistant" || chatHistory.last().content.isEmpty())
    val lastMessageContent = remember(chatHistory) { chatHistory.lastOrNull()?.content ?: "" }

    LaunchedEffect(chatHistory.size, showThinking, lastMessageContent) {
        val targetIndex = if (showThinking) chatHistory.size else chatHistory.size - 1
        if (targetIndex >= 0) {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val isAtBottom = if (visibleItems.isEmpty()) {
                true
            } else {
                val lastVisibleItem = visibleItems.last()
                lastVisibleItem.index >= layoutInfo.totalItemsCount - 2
            }
            if (isAtBottom || chatHistory.lastOrNull()?.role == "user") {
                if (isGenerating && chatHistory.lastOrNull()?.role == "assistant") {
                    listState.scrollToItem(targetIndex)
                } else {
                    listState.animateScrollToItem(targetIndex)
                }
            }
        }
    }

    val keyboardHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    LaunchedEffect(keyboardHeight) {
        if (keyboardHeight > 0.dp && chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (chatHistory.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Forum,
                    contentDescription = "Empty chat",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No history under: ${activeConfig?.name ?: "Select an active route"}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (activeConfig == null) {
                    Text(
                        text = "Please configure and activate a route first.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                reverseLayout = false,
                contentPadding = PaddingValues(bottom = 120.dp, top = 16.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(chatHistory) { msg ->
                    if (msg.role != "assistant" || msg.content.isNotEmpty()) {
                        ChatBubble(message = msg)
                    }
                }
                if (showThinking) {
                    item {
                        TypingIndicatorBubble()
                    }
                }
            }
        }

        // Floating Footer Row Input Container with Scrim Gradient Backdrop
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f
                    )
                )
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Media Attachment Preview
            if (mediaSupported && mediaUris.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
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
                            imageVector = Icons.Default.Info,
                            contentDescription = "Attached media",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Attached media: ${mediaUris.substringAfterLast("/")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { mediaUris = "" },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear attachment",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Input field border glow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mediaSupported) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                )
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Attach Media",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { 
                        Text(
                            text = "Chat with ${activeConfig?.modelName?.substringAfter("/") ?: "select Model..."}...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        ) 
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = false,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() && activeConfig != null) {
                                onSendMessage(inputText, mediaUris, mediaType)
                                inputText = ""
                                mediaUris = ""
                                keyboardController?.hide()
                            }
                        }
                    )
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isGenerating) MaterialTheme.colorScheme.error
                            else if (activeConfig != null && inputText.isNotBlank())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                        )
                        .clickable(
                            enabled = if (isGenerating) true
                                      else activeConfig != null && inputText.isNotBlank()
                        ) {
                            if (isGenerating) {
                                onStopGeneration()
                            } else {
                                onSendMessage(inputText, mediaUris, mediaType)
                                inputText = ""
                                mediaUris = ""
                                keyboardController?.hide()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGenerating) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isGenerating) "Stop generation" else "Send Message",
                        tint = if (isGenerating) Color.White
                               else if (activeConfig != null && inputText.isNotBlank()) Color.White
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val bubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isUser) 18.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 18.dp
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentWidth(align = if (isUser) Alignment.End else Alignment.Start)
                .clip(bubbleShape)
                .background(
                    if (isUser) {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            )
                        )
                    } else if (message.isError) {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                            )
                        )
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (message.isError) {
                        MaterialTheme.colorScheme.error
                    } else if (!isUser) {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    } else {
                        Color.Transparent
                    },
                    shape = bubbleShape
                )
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Column {
                MarkdownText(
                    content = message.content,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else if (message.isError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
fun TypingIndicatorBubble() {
    val transition = rememberInfiniteTransition(label = "typingIndicator")

    val dot1Progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0f at 0 using FastOutSlowInEasing
                1f at 250 using FastOutSlowInEasing
                0f at 500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1"
    )

    val dot2Progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0f at 150 using FastOutSlowInEasing
                1f at 400 using FastOutSlowInEasing
                0f at 650
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2"
    )

    val dot3Progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0f at 300 using FastOutSlowInEasing
                1f at 550 using FastOutSlowInEasing
                0f at 800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp))
                .padding(vertical = 12.dp, horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .graphicsLayer {
                            translationY = -dot1Progress * 4.dp.toPx()
                            alpha = 0.3f + (dot1Progress * 0.7f)
                        }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .graphicsLayer {
                            translationY = -dot2Progress * 4.dp.toPx()
                            alpha = 0.3f + (dot2Progress * 0.7f)
                        }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .graphicsLayer {
                            translationY = -dot3Progress * 4.dp.toPx()
                            alpha = 0.3f + (dot3Progress * 0.7f)
                        }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Thinking...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
