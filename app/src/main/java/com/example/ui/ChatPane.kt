package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.data.ChatMessage
import com.example.data.LlmConfiguration

@Composable
fun ChatInterface(
    activeConfig: LlmConfiguration?,
    chatHistory: List<ChatMessage>,
    isGenerating: Boolean,
    isWaitingForFirstChunk: Boolean,
    onSendMessage: (String, String, String, String) -> Unit,
    onRetryLastMessage: () -> Unit,
    onStopGeneration: () -> Unit = {}
) {
    val density = LocalDensity.current
    var composerHeightPx by remember { mutableIntStateOf(0) }
    val messageListBottomPadding = with(density) { composerHeightPx.toDp() } + 16.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (chatHistory.isEmpty()) {
            EmptyConversation(activeConfig = activeConfig)
        } else {
            MessageScroller(
                activeConfig = activeConfig,
                messages = chatHistory,
                bottomPadding = messageListBottomPadding,
                showTypingMarker = isWaitingForFirstChunk,
                isGenerating = isGenerating,
                onRetryLastMessage = onRetryLastMessage
            )
        }

        ChatComposer(
            activeConfig = activeConfig,
            isGenerating = isGenerating,
            onSendMessage = onSendMessage,
            onStopGeneration = onStopGeneration,
            onHeightChanged = { composerHeightPx = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
