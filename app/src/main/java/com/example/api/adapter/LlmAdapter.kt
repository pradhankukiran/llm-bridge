package com.example.api.adapter

import com.example.data.ChatMessage
import com.example.data.LlmConfiguration

data class AdapterChatRequest(
    val context: android.content.Context,
    val config: LlmConfiguration,
    val chatHistory: List<ChatMessage>
)

data class AdapterChatResult(
    val text: String,
    val rawResponse: String,
    val responseCode: Int
)

interface LlmAdapter {
    suspend fun chat(
        request: AdapterChatRequest,
        onChunkReceived: (String) -> Unit = {}
    ): AdapterChatResult
}

object ProviderIds {
    const val OPENAI_COMPATIBLE = "openai-compatible"
    const val ANTHROPIC = "anthropic"
}

enum class WireProtocol {
    OPENAI_CHAT_COMPLETIONS,
    ANTHROPIC_MESSAGES
}

data class ProviderSpec(
    val id: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val wireProtocol: WireProtocol
)

enum class ModelCapability {
    TEXT_CHAT,
    MULTIMODAL_INPUT,
    IMAGE_INPUT,
    VIDEO_INPUT,
    STREAMING,
    TOOL_CALLING,
    STRUCTURED_OUTPUT,
    REASONING
}

enum class AccessTier {
    UNKNOWN,
    FREE_PROTOTYPING,
    FREE_ENDPOINT,
    PAID,
    ENTERPRISE_REQUIRED
}

enum class ReasoningWireMode {
    NONE,
    REASONING_EFFORT,
    CHAT_TEMPLATE_THINKING,
    CHAT_TEMPLATE_THINKING_MODE
}

data class InferenceDefaults(
    val maxTokens: Int,
    val temperature: Double,
    val topP: Double,
    val stream: Boolean,
    val timeoutSeconds: Int
)

data class ReasoningOption(
    val id: String,
    val label: String
)

data class ModelOffering(
    val id: String,
    val providerId: String,
    val modelSlug: String,
    val displayName: String,
    val wireProtocol: WireProtocol,
    val capabilities: Set<ModelCapability>,
    val contextTokens: Int?,
    val maxOutputTokens: Int,
    val defaults: InferenceDefaults,
    val accessTier: AccessTier = AccessTier.UNKNOWN,
    val pricingNote: String = "",
    val reasoningWireMode: ReasoningWireMode = ReasoningWireMode.NONE,
    val defaultReasoningMode: String = "",
    val reasoningOptions: List<ReasoningOption> = emptyList()
)
