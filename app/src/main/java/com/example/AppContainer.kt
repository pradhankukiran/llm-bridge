package com.example

import android.content.Context
import com.example.api.AndroidMediaLoader
import com.example.api.LlmClient
import com.example.api.adapter.AnthropicMessagesAdapter
import com.example.api.adapter.LlmAdapter
import com.example.api.adapter.MediaLoader
import com.example.api.adapter.OpenAiCompatibleAdapter
import com.example.data.AppDatabase
import com.example.data.ApiKeyCipher
import com.example.data.KeystoreApiKeyCipher
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
        OpenAiCompatibleAdapter(baseHttpClient, mediaLoader)
    }

    private val mediaLoader: MediaLoader by lazy {
        AndroidMediaLoader(context)
    }

    private val anthropicAdapter: LlmAdapter by lazy {
        AnthropicMessagesAdapter(baseHttpClient)
    }

    private val apiKeyCipher: ApiKeyCipher by lazy {
        KeystoreApiKeyCipher()
    }

    override val repository: LlmRepository by lazy {
        LlmRepository(database.llmDao(), apiKeyCipher)
    }

    override val llmClient: LlmClient by lazy {
        LlmClient(openAiCompatibleAdapter, anthropicAdapter)
    }
}
