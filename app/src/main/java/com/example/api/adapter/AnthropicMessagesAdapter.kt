package com.example.api.adapter

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AnthropicMessagesAdapter(
    private val baseHttpClient: OkHttpClient
) : LlmAdapter {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun chat(
        request: AdapterChatRequest,
        onChunkReceived: (String) -> Unit
    ): AdapterChatResult {
        val config = request.config
        val cleanBaseUrl = config.baseUrl.trimEnd('/')
        val url = if (cleanBaseUrl.endsWith("/messages")) cleanBaseUrl else "$cleanBaseUrl/messages"

        val headers = mutableMapOf(
            "x-api-key" to config.apiKey,
            "anthropic-version" to "2023-06-01",
            "content-type" to "application/json"
        )
        JsonWire.mergeHeaders(headers, config.extraHeadersJson)

        val historySystemPrompt = request.chatHistory
            .filter { it.role == "system" }
            .joinToString("\n") { it.content }

        val finalSystemPrompt = if (config.systemPrompt.isNotBlank()) {
            if (historySystemPrompt.isNotEmpty()) config.systemPrompt + "\n" + historySystemPrompt else config.systemPrompt
        } else {
            historySystemPrompt
        }

        val bodyJson = JSONObject().apply {
            put("model", config.modelName)
            put("messages", JsonWire.messagesArray(request.chatHistory, includeSystem = false))
            if (finalSystemPrompt.isNotEmpty()) {
                put("system", finalSystemPrompt)
            }
            put("max_tokens", config.maxTokens)
            put("temperature", config.temperature)
            put("top_p", config.topP)
        }
        JsonWire.mergeJsonObject(bodyJson, config.extraBodyJson)

        val requestBuilder = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody(jsonMediaType))
        for ((key, value) in headers) requestBuilder.addHeader(key, value)

        val httpClient = baseHttpClient.newBuilder()
            .readTimeout(config.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .build()

        httpClient.newCall(requestBuilder.build()).execute().use { response ->
            val raw = response.body?.string() ?: ""
            val text = if (response.isSuccessful) {
                val parsed = JsonWire.parseAnthropicResponse(raw)
                onChunkReceived(parsed)
                parsed
            } else {
                "HTTP ${response.code}: ${JsonWire.errorMessageSnippet(raw)}"
            }
            return AdapterChatResult(text, raw, response.code)
        }
    }
}
