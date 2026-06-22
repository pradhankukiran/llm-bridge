package com.example

import android.content.Context
import com.example.api.LlmClient
import com.example.api.adapter.AnthropicMessagesAdapter
import com.example.api.adapter.LlmAdapter
import com.example.api.adapter.OpenAiCompatibleAdapter
import com.example.data.AppDatabase
import com.example.data.LlmRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

interface AppContainer {
    val repository: LlmRepository
    val llmClient: LlmClient
}

class AppContainerImpl(private val context: Context) : AppContainer {
    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    private val baseHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val openAiCompatibleAdapter: LlmAdapter by lazy {
        OpenAiCompatibleAdapter(baseHttpClient)
    }

    private val anthropicAdapter: LlmAdapter by lazy {
        AnthropicMessagesAdapter(baseHttpClient)
    }

    override val repository: LlmRepository by lazy {
        LlmRepository(database.llmDao())
    }

    override val llmClient: LlmClient by lazy {
        LlmClient(openAiCompatibleAdapter, anthropicAdapter)
    }
}
