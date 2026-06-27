package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessage
import com.example.data.LlmConfiguration

@Composable
fun EmptyConversation(activeConfig: LlmConfiguration?) {
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
            text = if (activeConfig == null) {
                "Select and activate a route to start chatting."
            } else {
                "No messages yet."
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (activeConfig != null) {
            Text(
                text = "Start a new conversation below.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
fun MessageScroller(
    activeConfig: LlmConfiguration?,
    messages: List<ChatMessage>,
    bottomPadding: Dp,
    showTypingMarker: Boolean,
    isGenerating: Boolean,
    onRetryLastMessage: () -> Unit,
    onMessageCopied: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, showTypingMarker, messages.lastOrNull()?.content) {
        val targetIndex = if (showTypingMarker) messages.size else messages.size - 1
        if (targetIndex >= 0) {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val isAtBottom = if (visibleItems.isEmpty()) {
                true
            } else {
                val lastVisibleItem = visibleItems.last()
                lastVisibleItem.index >= layoutInfo.totalItemsCount - 2
            }
            if (isAtBottom || messages.lastOrNull()?.role == "user") {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    val keyboardHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    LaunchedEffect(keyboardHeight) {
        if (keyboardHeight > 0.dp && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        reverseLayout = false,
        contentPadding = PaddingValues(
            bottom = bottomPadding,
            top = 16.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(
            items = messages,
            key = { message -> chatMessageKey(message) }
        ) { message ->
            if (message.role != "assistant" || message.content.isNotEmpty()) {
                MessageBubble(
                    message = message,
                    showThinkingTags = activeConfig?.reasoningMode == REASONING_MODE_SHOW_THINKING,
                    showRetry = message == messages.lastOrNull() && message.isError && !isGenerating,
                    onRetry = onRetryLastMessage,
                    onMessageCopied = onMessageCopied
                )
            }
        }
        if (showTypingMarker) {
            item(key = "typing-indicator") {
                TypingMarker()
            }
        }
    }
}

private fun chatMessageKey(message: ChatMessage): String {
    if (message.id != 0) return "message-${message.id}"
    return "pending-${message.sessionId}-${message.role}-${message.timestamp}"
}

@Composable
fun TypingMarker() {
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
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
                )
                .padding(vertical = 12.dp, horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedDot(progress = dot1Progress)
                AnimatedDot(progress = dot2Progress)
                AnimatedDot(progress = dot3Progress)
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

@Composable
private fun AnimatedDot(progress: Float) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .graphicsLayer {
                translationY = -progress * 4.dp.toPx()
                alpha = 0.3f + (progress * 0.7f)
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
    )
}
