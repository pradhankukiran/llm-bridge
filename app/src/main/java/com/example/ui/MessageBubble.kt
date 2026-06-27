package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ChatMessage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    showThinkingTags: Boolean = false,
    showRetry: Boolean = false,
    onRetry: () -> Unit = {},
    onMessageCopied: () -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    val isUser = message.role == "user"
    val parsedThinking = remember(message.content, isUser) {
        if (isUser) null else parseThinkingTagContent(message.content)
    }
    val displayContent = parsedThinking?.answer ?: message.content
    val hasHiddenThinkingOnly = !isUser &&
        parsedThinking?.hasThoughts == true &&
        parsedThinking.answer.isBlank()
    val copyMessage = {
        if (displayContent.isNotBlank()) {
            clipboardManager.setText(AnnotatedString(displayContent))
            onMessageCopied()
        }
    }
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
                .widthIn(max = if (isUser) 560.dp else 720.dp)
                .clip(bubbleShape)
                .combinedClickable(
                    onClick = {},
                    onLongClick = copyMessage
                )
                .background(messageBubbleColor(isUser = isUser, isError = message.isError))
                .border(
                    width = 1.dp,
                    color = if (message.isError) {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                    } else if (!isUser) {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    } else {
                        Color.Transparent
                    },
                    shape = bubbleShape
                )
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Column {
                val textColor = if (isUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else if (message.isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }

                if (!isUser && parsedThinking?.hasThoughts == true && showThinkingTags) {
                    ThinkingTagPanel(
                        thoughts = parsedThinking.thoughts,
                        isStreaming = parsedThinking.hasUnclosedThought,
                        color = textColor
                    )
                    if (parsedThinking.answer.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                if (hasHiddenThinkingOnly && !showThinkingTags) {
                    Text(
                        text = "Thinking...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (displayContent.isNotBlank()) {
                    MarkdownText(
                        content = displayContent,
                        color = textColor
                    )
                }

                if (isUser && message.mediaUri.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    MessageAttachment(
                        displayName = message.mediaDisplayName.ifBlank { message.mediaUri.substringAfterLast("/") },
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }

                if (showRetry) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onRetry,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Retry")
                    }
                }

                if (!isUser && displayContent.isNotBlank()) {
                    MessageActions(
                        modifier = Modifier.align(Alignment.End),
                        textColor = textColor,
                        onCopy = copyMessage
                    )
                }
            }
        }
    }
}

@Composable
private fun messageBubbleColor(isUser: Boolean, isError: Boolean): Color {
    return when {
        isUser -> MaterialTheme.colorScheme.primaryContainer
        isError -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
}

@Composable
private fun MessageAttachment(
    displayName: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AttachFile,
            contentDescription = "Attached media",
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodySmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MessageActions(
    modifier: Modifier = Modifier,
    textColor: Color,
    onCopy: () -> Unit
) {
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End
    ) {
        IconButton(
            onClick = onCopy,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy message",
                tint = textColor.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ThinkingTagPanel(
    thoughts: List<String>,
    isStreaming: Boolean,
    color: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val title = if (isStreaming) "Thinking..." else "Thinking"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color.copy(alpha = 0.8f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Hide thinking" else "Show thinking",
                    tint = color.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp)
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                MarkdownText(
                    content = thoughts.joinToString("\n\n"),
                    color = color.copy(alpha = 0.82f)
                )
            }
        }
    }
}
