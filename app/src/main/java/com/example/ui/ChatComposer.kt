package com.example.ui

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.LlmConfiguration

@Composable
fun ChatComposer(
    activeConfig: LlmConfiguration?,
    isGenerating: Boolean,
    onSendMessage: (String, String, String, String) -> Unit,
    onStopGeneration: () -> Unit,
    onHeightChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var inputText by remember { mutableStateOf("") }
    var mediaUris by remember(activeConfig?.id) { mutableStateOf("") }
    var mediaType by remember(activeConfig?.id) { mutableStateOf(activeConfig?.mediaInputType ?: "auto") }
    val mediaSupported = activeConfig != null
    val inputEnabled = activeConfig != null && !isGenerating

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            mediaUris = uri.toString()
        }
    }

    val attachmentName = remember(mediaUris) {
        mediaUris.takeIf { it.isNotBlank() }?.let { uriText ->
            resolveDisplayName(context.contentResolver, Uri.parse(uriText)) ?: uriText.substringAfterLast("/")
        }
    }
    val submitMessage = {
        if (inputText.isNotBlank() && inputEnabled) {
            onSendMessage(inputText, mediaUris, attachmentName.orEmpty(), mediaType)
            inputText = ""
            mediaUris = ""
            keyboardController?.hide()
        }
    }

    Column(
        modifier = modifier
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
            .onSizeChanged { onHeightChanged(it.height) }
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        if (mediaSupported && mediaUris.isNotEmpty()) {
            ComposerAttachmentPreview(
                attachmentName = attachmentName.orEmpty(),
                onClear = { mediaUris = "" }
            )
        }

        ComposerInputRow(
            activeConfig = activeConfig,
            inputText = inputText,
            isGenerating = isGenerating,
            inputEnabled = inputEnabled,
            mediaSupported = mediaSupported,
            onInputTextChange = { inputText = it },
            onAttach = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            },
            onSubmit = submitMessage,
            onStopGeneration = onStopGeneration
        )
    }
}

@Composable
private fun ComposerAttachmentPreview(
    attachmentName: String,
    onClear: () -> Unit
) {
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
                imageVector = Icons.Default.AttachFile,
                contentDescription = "Attached media",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Attached media: $attachmentName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onClear,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear attachment",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ComposerInputRow(
    activeConfig: LlmConfiguration?,
    inputText: String,
    isGenerating: Boolean,
    inputEnabled: Boolean,
    mediaSupported: Boolean,
    onInputTextChange: (String) -> Unit,
    onAttach: () -> Unit,
    onSubmit: () -> Unit,
    onStopGeneration: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = if (isGenerating) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (mediaSupported) {
            IconButton(
                enabled = inputEnabled,
                onClick = onAttach
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Attach Media",
                    tint = if (inputEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        TextField(
            value = inputText,
            onValueChange = onInputTextChange,
            placeholder = {
                Text(
                    text = if (activeConfig == null) {
                        "Select a route to chat"
                    } else if (isGenerating) {
                        "Response in progress"
                    } else {
                        "Start chatting..."
                    },
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
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = false,
            minLines = 1,
            maxLines = 4,
            enabled = inputEnabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            keyboardActions = KeyboardActions(onSend = { onSubmit() })
        )

        SendStopButton(
            isGenerating = isGenerating,
            canSend = inputEnabled && inputText.isNotBlank(),
            onSubmit = onSubmit,
            onStopGeneration = onStopGeneration
        )
    }
}

@Composable
private fun SendStopButton(
    isGenerating: Boolean,
    canSend: Boolean,
    onSubmit: () -> Unit,
    onStopGeneration: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (isGenerating) {
                    MaterialTheme.colorScheme.error
                } else if (canSend) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                }
            )
            .clickable(enabled = isGenerating || canSend) {
                if (isGenerating) {
                    onStopGeneration()
                } else {
                    onSubmit()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isGenerating) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
            contentDescription = if (isGenerating) "Stop generation" else "Send Message",
            tint = if (isGenerating || canSend) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            },
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun resolveDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
    return contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        } else {
            null
        }
    }
}
