package com.example.api

import android.util.Log
import com.example.api.adapter.AdapterChatRequest
import com.example.api.adapter.AnthropicMessagesAdapter
import com.example.api.adapter.LlmAdapter
import com.example.api.adapter.OpenAiCompatibleAdapter
import com.example.data.ChatMessage
import com.example.data.LlmConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

sealed class LlmResponse {
    data class Success(val text: String, val rawResponse: String, val durationMs: Long) : LlmResponse()
    data class Error(val message: String, val code: Int, val rawResponse: String, val durationMs: Long) : LlmResponse()
}

class LlmClient(
    private val openAiCompatibleAdapter: LlmAdapter,
    private val anthropicAdapter: LlmAdapter
) {

    suspend fun executeChatCall(
        context: android.content.Context,
        config: LlmConfiguration,
        chatHistory: List<ChatMessage>,
        onChunkReceived: (String) -> Unit = {}
    ): LlmResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val adapter = adapterFor(config)
            val result = adapter.chat(AdapterChatRequest(context, config, chatHistory), onChunkReceived)
            val durationMs = System.currentTimeMillis() - startTime

            if (result.responseCode in 200..299) {
                LlmResponse.Success(result.text, result.rawResponse, durationMs)
            } else {
                LlmResponse.Error(result.text, result.responseCode, result.rawResponse, durationMs)
            }
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            val errorMsg = e.localizedMessage ?: e.javaClass.simpleName
            Log.e("LlmClient", "Request execution failed", e)
            LlmResponse.Error(errorMsg, -1, errorMsg, durationMs)
        }
    }

    private fun adapterFor(config: LlmConfiguration): LlmAdapter {
        return when {
            config.apiType == "ANTHROPIC" -> anthropicAdapter
            else -> openAiCompatibleAdapter
        }
    }
}
