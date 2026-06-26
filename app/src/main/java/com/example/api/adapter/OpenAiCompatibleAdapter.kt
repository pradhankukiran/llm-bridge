package com.example.api.adapter

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAiCompatibleAdapter(
    private val baseHttpClient: OkHttpClient,
    private val mediaLoader: MediaLoader
) : LlmAdapter {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun chat(
        request: AdapterChatRequest,
        onChunkReceived: (String) -> Unit
    ): AdapterChatResult {
        val config = request.config
        val cleanBaseUrl = config.baseUrl.trimEnd('/')
        val url = if (cleanBaseUrl.endsWith("/chat/completions")) {
            cleanBaseUrl
        } else {
            "$cleanBaseUrl/chat/completions"
        }

        val headers = mutableMapOf(
            "content-type" to "application/json",
            "accept" to if (config.stream) "text/event-stream" else "application/json"
        )
        if (config.apiKey.isNotBlank()) {
            headers["Authorization"] = "Bearer ${config.apiKey}"
        }
        JsonWire.mergeHeaders(headers, config.extraHeadersJson)

        val bodyJson = JSONObject().apply {
            put("model", config.modelName)
            put("messages", openAiMessagesArray(request))
            put("max_tokens", config.maxTokens)
            put("temperature", config.temperature)
            put("top_p", config.topP)
            put("stream", config.stream)
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
            if (!response.isSuccessful) {
                val raw = response.body?.string() ?: ""
                val text = "HTTP ${response.code}: ${JsonWire.errorMessageSnippet(raw)}"
                return AdapterChatResult(text, raw, response.code)
            }

            if (config.stream) {
                val source = response.body?.source() ?: return AdapterChatResult("Empty body", "", response.code)
                val fullText = StringBuilder()
                val rawBuffer = StringBuilder()
                while (true) {
                    val line = source.readUtf8Line() ?: break
                    rawBuffer.append(line).append("\n")
                    val chunk = JsonWire.parseOpenAiStreamLine(line)
                    if (chunk != null) {
                        fullText.append(chunk)
                        onChunkReceived(chunk)
                    }
                }
                if (fullText.isBlank()) {
                    return AdapterChatResult(
                        "Stream ended without content.\nRaw: $rawBuffer",
                        rawBuffer.toString(),
                        response.code
                    )
                }
                return AdapterChatResult(fullText.toString(), rawBuffer.toString(), response.code)
            } else {
                val raw = response.body?.string() ?: ""
                val text = JsonWire.parseOpenAiResponse(raw)
                return AdapterChatResult(text, raw, response.code)
            }
        }
    }

    private fun openAiMessagesArray(request: AdapterChatRequest): JSONArray {
        val mediaUris = request.config.mediaInputUris
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { uri -> mediaLoader.loadAsDataUrl(uri) }
            .toList()

        val baseMessages = if (mediaUris.isEmpty()) {
            JsonWire.messagesArray(request.chatHistory)
        } else {
            val messagesArray = JSONArray()
            val lastUserIndex = request.chatHistory.indexOfLast { it.role == "user" }
            for ((index, msg) in request.chatHistory.withIndex()) {
                messagesArray.put(JSONObject().apply {
                    put("role", msg.role)
                    if (index == lastUserIndex) {
                        put("content", multimodalContent(msg.content, mediaUris, request.config.mediaInputType))
                    } else {
                        put("content", msg.content)
                    }
                })
            }
            messagesArray
        }

        val finalMessages = if (request.config.systemPrompt.isNotBlank()) {
            val systemMsg = JSONObject().apply {
                put("role", "system")
                put("content", request.config.systemPrompt)
            }
            val newMsgs = JSONArray()
            newMsgs.put(systemMsg)
            for (i in 0 until baseMessages.length()) {
                newMsgs.put(baseMessages.get(i))
            }
            newMsgs
        } else {
            baseMessages
        }

        return finalMessages
    }

    private fun multimodalContent(text: String, mediaUris: List<String>, mediaInputType: String): JSONArray {
        val content = JSONArray()
        content.put(JSONObject().apply {
            put("type", "text")
            put("text", text)
        })
        for (uri in mediaUris) {
            val type = mediaPartType(uri, mediaInputType)
            content.put(JSONObject().apply {
                put("type", type)
                put(type, JSONObject().put("url", uri))
            })
        }
        return content
    }

    private fun mediaPartType(uri: String, mediaInputType: String): String {
        return when (mediaInputType.lowercase()) {
            "image" -> "image_url"
            "video" -> "video_url"
            else -> {
                val lower = uri.lowercase()
                if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".webm")) {
                    "video_url"
                } else {
                    "image_url"
                }
            }
        }
    }
}
